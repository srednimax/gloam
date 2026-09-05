package app.gloam.shade

/**
 * How long a shade the user started by hand stays up (CONTEXT.md: **auto-off**).
 *
 * A **fixed set of five**, so it is five settings and not a list — `CLAUDE.md`'s cardinality test.
 * Nobody creates a value here at runtime, which is why this stays in DataStore and no database comes
 * back with it.
 *
 * **[Never] is the choice that disables it, and "off" is not available to mean that.** CONTEXT.md
 * reserves *off* for the shade not being drawn, so "auto-off: off" is a sentence meaning the shade
 * stays on — a settings row that can be read backwards is a settings row somebody reads backwards.
 *
 * Kotlin note: an `enum class` with a constructor argument is a lookup table. The minutes travel
 * with the member rather than in a `when` somewhere else, which is the same shape `AppChannel` and
 * `TopLevelDestination` already use here — and it is what lets storage hold a plain number while the
 * UI holds a case (see [ofMinutes]).
 */
enum class AutoOff(
    val minutes: Int,
) {
    Never(0),
    Minutes30(30),
    Hour1(60),
    Hours2(120),
    Hours4(240),
    ;

    companion object {
        /**
         * Two hours, and it ships **provisional**.
         *
         * The span the plan asks for is "an episode to a film", and this is the longest value that
         * is still obviously *not* "the next day" — which is the failure being designed out. It is
         * an argument about a taste made in a room with one person in it, so `PLAN.md` rule 5 hands
         * the question to the twelve testers rather than treating it as settled.
         *
         * **Defaulting *on* is a different question and is not a taste.** Until Phase 2b's Quick
         * Settings tile ships, auto-off is the only escape hatch that needs no gesture at all: the
         * notification's Stop action takes a long-press to reach on HyperOS (`phase-1.md`, R8) and
         * the app's own Stop button sits under the shade. A safety floor is not put to a vote.
         */
        val Default = Hours2

        /**
         * The stored minutes back into a case, falling back to [Default] rather than throwing.
         *
         * **Storage holds the minutes rather than the name**, which is the opposite of what
         * `theme_mode` does, and the difference is which direction the value can move: a theme mode
         * is a closed set, while a duration is a number a later build might offer more of. An `Int`
         * lets a future *3 hours* exist without a stored `Hours3` that today's build cannot parse —
         * it reads as unrecognised, and unrecognised means the default.
         *
         * The fallback is the same rule `themeMode` follows and for the same reason: a preference
         * file survives a downgrade, and the wrong duration is a far better outcome than a crash
         * loop at startup.
         */
        fun ofMinutes(minutes: Int): AutoOff = entries.firstOrNull { it.minutes == minutes } ?: Default
    }
}

/**
 * The stored value meaning *no deadline*.
 *
 * DataStore has no null, and a sentinel that is also a valid instant would be **1970** — which is in
 * the past, which [isDue] would answer `true` to, which would take the shade down the instant it
 * went up. That is the whole reason this constant and [deadlineOrNull] exist rather than the raw
 * `Long` travelling around: the failure is silent and it is the app's one job that it not happen.
 */
const val NO_DEADLINE = 0L

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * The instant the shade should come down, or `null` for [AutoOff.Never].
 *
 * **Absolute rather than a countdown, and that is load-bearing rather than tidy.** HyperOS kills the
 * service and `START_STICKY` restarts it; a restarted service has no idea how long it was dead, so a
 * stored *duration* would silently restart the clock while a stored *instant* resumes the deadline
 * the user was actually promised.
 *
 * **This is one candidate now, not the answer.** It was `deadlineFor` until the schedule arrived
 * with a second promise about the same stored value, and the rename is deliberate rather than
 * cosmetic: [app.gloam.shade.deadlineFor] is the function that resolves *every* live deadline, and
 * leaving a two-argument overload beside the five-argument one is the single most likely way that
 * rule gets broken later. A new call site takes the shorter signature, compiles, passes every test,
 * and quietly ships a shade the schedule cannot bound. An overload here is not a convenience, it is
 * a trap with the same name as the safe thing — so this one is `internal` and differently named.
 */
internal fun autoOffDeadline(
    startedAt: Long,
    choice: AutoOff,
): Long? = if (choice == AutoOff.Never) null else startedAt + choice.minutes * MILLIS_PER_MINUTE

/**
 * The stored `Long` back into a deadline, with [NO_DEADLINE] read as *absent* rather than as 1970.
 *
 * The one place the sentinel is known, in the direction that matters. It is the mirror of
 * `ShadeValues.backlight` collapsing to `BRIGHTNESS_OVERRIDE_NONE`: Java's APIs and DataStore's keys
 * both had to spell *absent* as a number, Kotlin does not, and the conversion belongs at the single
 * boundary rather than in every caller.
 */
fun deadlineOrNull(stored: Long): Long? = stored.takeIf { it != NO_DEADLINE }

/**
 * Whether a stored deadline has already passed. A `null` deadline is never due.
 *
 * `>=` rather than `>`: the loop that fires this treats *remaining ≤ 0* as due, and a reader that
 * disagreed with it by one millisecond would leave a deadline that is exactly now unfired until the
 * next re-check.
 */
fun isDue(
    now: Long,
    offAtMillis: Long?,
): Boolean = offAtMillis != null && now >= offAtMillis
