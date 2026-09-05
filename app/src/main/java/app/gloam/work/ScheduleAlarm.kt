package app.gloam.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import app.gloam.shade.Schedule
import app.gloam.shade.ScheduleReceiver
import app.gloam.shade.nextOn
import java.time.ZoneId

/**
 * `adb logcat -s GloamSchedule:*` is how checkpoint E reads the schedule, and this file deliberately
 * shares the receiver's tag rather than taking one of its own: an arm and the fire it produced are
 * two halves of one story, and a reading that has to interleave two tags by timestamp is a reading
 * nobody trusts. The gate's `GloamGate` is the one that stays separate, for the opposite reason.
 */
private const val TAG = "GloamSchedule"

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

/**
 * Arm the next scheduled-on, or cancel if the schedule can never open. Idempotent; call it freely.
 *
 * **One alarm at a time, for the next transition, re-derived from the window every time anything
 * happens.** ADR-0003's *"nothing persists a schedule; due dates are derived"* survives intact:
 * there is no per-night enqueue, nothing to cancel, nothing to orphan, nothing to double-fire, and
 * no state anywhere that a restore could disagree with. What is armed is a [hopFor] hop toward
 * [Schedule.nextOn] rather than `nextOn` itself — see that function for why one long arm is not good
 * enough on this platform.
 *
 * Four decisions here each have a wrong answer that compiles (`docs/phase-4.md` section 4):
 *
 * - **`setAndAllowWhileIdle`, not `setExact*` and not `setAlarmClock`.** Both exact forms need
 *   `SCHEDULE_EXACT_ALARM`, which needs a Play declaration form for an app that is not a clock or a
 *   calendar; `USE_EXACT_ALARM` is worse — auto-granted, unrevokable, and restricted by policy to
 *   alarm-clock apps, so declaring it in a dimmer puts the listing at risk to save a settings
 *   screen (ADR-0003).
 * - **`RTC_WAKEUP`, not `RTC`.** A non-waking alarm fires when the device next wakes — and turning
 *   the screen on *is* a device wake, so the race would be between this broadcast and the first
 *   frame the user sees. Losing it is a bright flash followed by the shade, on a phone picked up in
 *   a dark room, which is the exact experience this app exists to prevent. The cost is one wake per
 *   hop.
 * - **`FLAG_IMMUTABLE`**, required from API 31 and correct anyway: nothing outside this app has any
 *   business filling in fields on an intent that raises the shade.
 * - **`FLAG_UPDATE_CURRENT`**, which is what makes re-arming idempotent. A `PendingIntent`'s
 *   identity is its request code and component, so `set` replaces rather than stacks and section 6's
 *   several re-arm sites cannot produce several alarms.
 *
 * **Cancelling is the same call**, which is why no caller needs a branch: a `nextOn` of null — the
 * schedule disabled, or the degenerate `onAt == offAt` window — cancels rather than arming, so there
 * is no second entry point that can be forgotten.
 *
 * **No new permission.** `setAndAllowWhileIdle` needs none, and the receiver is `exported="false"`,
 * reached only by the `PendingIntent` below. `scripts/aab-permissions.py` proves that on the built
 * artifact rather than in the diff.
 *
 * Kotlin/Android note: a `PendingIntent` is a token handed to another process — here to the system's
 * alarm service — that lets it perform this action *as us*, later, whether or not we are alive. It
 * has no JS analogue; the nearest thing is a capability handed to a service worker, except that it
 * outlives the process rather than the page.
 */
fun Context.armScheduleAlarm(
    schedule: Schedule,
    now: Long = System.currentTimeMillis(),
) {
    val alarms = getSystemService(AlarmManager::class.java) ?: return
    val pending = schedulePendingIntent()

    val nextOn = schedule.nextOn(now, ZoneId.systemDefault())
    if (nextOn == null) {
        alarms.cancel(pending)
        Log.i(TAG, "no window will open; alarm cancelled")
        return
    }

    val target = now + hopFor(nextOn - now)
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pending)
    Log.i(TAG, "armed for $target, a ${target - now}ms hop toward the on-instant $nextOn")
}

/**
 * The one token, and every call site gets the same one.
 *
 * Same request code and same component means the same identity, which is what
 * [PendingIntent.FLAG_UPDATE_CURRENT] and [AlarmManager.cancel] both work from — arming twice
 * replaces, and cancelling reaches the alarm that arming created.
 */
private fun Context.schedulePendingIntent(): PendingIntent =
    PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, ScheduleReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
