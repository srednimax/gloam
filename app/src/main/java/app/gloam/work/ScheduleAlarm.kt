package app.gloam.work

/**
 * How wide a window the platform gives an inexact alarm: **75% of its futurity, capped at an hour.**
 *
 * Both halves are `AlarmManager`'s own heuristic rather than anything of ours, and both were measured
 * on the phone rather than read off documentation (`docs/phase-4.md` section 1). `dumpsys alarm`
 * prints the window the instant an alarm is armed:
 *
 * ```
 * armed 2 minutes out:   window=+1m29s997ms      (75% of 120s)
 * armed 10 hours out:    window=+1h0m0s0ms       (the cap)
 * ```
 *
 * **And the far end of that window is where this ROM delivers.** Three cells - awake, in forced Doze
 * with the battery exemption, and in forced Doze without it - each fired within 50 ms of
 * `maxWhenElapsed`: 90,017 ms, 90,045 ms and 90,049 ms late against a 90-second window. So the
 * window is not a distribution to be optimistic about; it is the lateness.
 */
private const val WINDOW_FACTOR_NUMERATOR = 3L

private const val WINDOW_FACTOR_DENOMINATOR = 4L

/** The cap on that window, and the reason one long arm is not good enough. */
private const val WINDOW_CAP_MS = 60L * 60 * 1000

/**
 * Above this gap the cap binds rather than the 75%, so `gap - 1h` is the widest safe hop.
 *
 * Below it the cap is irrelevant and `gap / 1.75` is. The boundary is where the two agree:
 * `0.75x = 1h` at `x = 80 min`, which is reachable from `gap = 2h20m`.
 */
private const val CAPPED_REGIME_MS = 140L * 60 * 1000

/**
 * How close the chain gets before it stops trying, and therefore how late the shade can be.
 *
 * The last hop aims **at** the on-instant rather than short of it, so it is the one hop allowed to
 * overshoot - by at most 75% of five minutes. Everything above this in the chain is chosen never to
 * overshoot at all, which is what makes the final imprecision the *only* imprecision.
 */
private const val FINAL_HOP_MS = 5L * 60 * 1000

/**
 * How far ahead to arm the next alarm, given [gap] milliseconds until the window opens.
 *
 * ## Why an alarm chain rather than one alarm
 *
 * The obvious implementation arms `nextOn` directly and is wrong for a reason no amount of reading
 * would have found: a schedule set at breakfast is an alarm armed fourteen hours out, and an alarm
 * armed that far out carries an hour-wide window that this ROM spends in full. *On at 22:00* would
 * mean *somewhere before 23:00*, every night, which is not a reading window anybody would accept.
 *
 * So the alarm is re-armed in hops. An alarm armed for `now + x` arrives no later than
 * `now + x + min(0.75x, 1h)`, so it cannot overshoot the on-instant as long as
 *
 * ```
 * x + min(0.75x, 1h) <= gap
 * ```
 *
 * which gives the two regimes below. Each hop lands strictly inside the last 43% of what remains and
 * never past it, so the gap shrinks geometrically: **a 24-hour gap closes to the final hop in five
 * hops**, not the every-ten-minutes poll the problem first suggests.
 *
 * ## Why it costs no new machinery
 *
 * A hop fires *before* the window opens, so the schedule's receiver takes the branch it already has
 * for an alarm that arrived outside the window: it declines, and the re-arm at the bottom of its
 * `when` arms the next hop. There is no chain state anywhere - each hop is derived from the schedule
 * and the clock, exactly as ADR-0003 requires, and losing one is losing an alarm rather than losing
 * a place in a sequence.
 *
 * **Delivering early is harmless by construction**, which is what makes this robust: the bound above
 * is an upper bound only, and an alarm batched early with somebody else's simply becomes a hop that
 * re-arms sooner.
 */
internal fun hopFor(gap: Long): Long =
    when {
        // Already there, or past it. The caller has nothing to arm; answering the gap keeps this
        // total rather than making every call site check first.
        gap <= 0L -> gap

        // The last hop aims at the on-instant itself and is the only one allowed to arrive late.
        gap <= FINAL_HOP_MS -> gap

        // The cap binds: an hour of window whatever we ask for, so leave an hour of room.
        gap >= CAPPED_REGIME_MS -> gap - WINDOW_CAP_MS

        // The 75% binds: x + 0.75x <= gap, so x = 4/7 of the gap.
        else -> gap * WINDOW_FACTOR_DENOMINATOR / (WINDOW_FACTOR_DENOMINATOR + WINDOW_FACTOR_NUMERATOR)
    }

/**
 * The latest an alarm armed [hop] from now can arrive, by the platform's own heuristic.
 *
 * Exposed for the sweep rather than for callers: the property worth asserting is that no hop
 * overshoots, and a test that recomputed the window from its own copy of the constants would be a
 * test of a copy.
 */
internal fun latestArrival(hop: Long): Long =
    hop + minOf(hop * WINDOW_FACTOR_NUMERATOR / WINDOW_FACTOR_DENOMINATOR, WINDOW_CAP_MS)
