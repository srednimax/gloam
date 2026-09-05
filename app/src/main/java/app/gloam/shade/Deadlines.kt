package app.gloam.shade

import app.gloam.data.AppPreferences
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import kotlin.math.min

/**
 * Why the shade is going up, which decides whether auto-off is one of the deadlines.
 *
 * **Auto-off does not apply to a scheduled episode**, and `CONTEXT.md` decided that before this file
 * existed — its *auto-off* row reads "the duration a **hand-started** shade lasts". It is not a
 * re-reading for convenience: auto-off exists because a shade somebody put up by hand can be
 * forgotten, and a shade that came up by itself at 22:00 with an off-time of 07:00 attached has not
 * been forgotten by anybody. Applying it anyway would mean a two-hour default silently truncating
 * every scheduled night.
 *
 * Kotlin note: an `enum class` used as a *reason* parameter is a discriminated union with two cases.
 * A `Boolean` would fit and is the version to avoid — `beginShadeAt(true)` at a call site says
 * nothing, and the next case somebody adds has nowhere to go.
 */
enum class ShadeStart {
    ByHand,
    BySchedule,
}

/**
 * Why the shade came down, which decides whether the night is spent.
 *
 * The mirror of [ShadeStart], and it exists for the same architectural reason: why an episode ended
 * is not something [AppPreferences] can know, and a schedule-aware marker write inside `endShade`
 * would put a decision about windows inside the file that is supposed to hold keys and defaults.
 *
 * [Reaped] is the boot receiver's, and it is the only one that leaves the marker alone. Without it,
 * a phone switched off at 21:00 with a passed deadline and booted at 23:00 inside a 22:00-to-07:00
 * window would have tonight spent by the tidy-up itself — and the schedule skipped for a reason that
 * has nothing to do with the schedule, on the one path whose whole job is cleaning up after the
 * phone being off.
 */
enum class ShadeEnd {
    ByHand,
    ByDeadline,
    Reaped,
}

/**
 * The instant the shade comes down: **the earliest of the deadlines live at this moment**.
 *
 * ## The rule, and the failure it is shaped around
 *
 * There are two ways one stored deadline with several writers can go wrong, and only one of them is
 * a safety property:
 *
 * - **A deadline silently lost** — the user is promised 23:00, something overwrites it with 07:00,
 *   and the shade is up all night. This is the failure, and it is the app's named fear.
 * - **A deadline silently shortened** — the user expects 07:00 and gets 23:00. Annoying, visible,
 *   self-correcting (the Start button is right there), and it errs toward light.
 *
 * Every rule here errs toward the second: the same direction the ramp's two bounds err, the same
 * direction the boot receiver's three refusals err, and the same direction the notification
 * permission went — the app runs and says what was given up, but where the two failures are
 * *asymmetric* it takes the visible one.
 *
 * ## The candidates, and when each is live
 *
 * | Candidate | Live when | Value |
 * | --- | --- | --- |
 * | Auto-off | The episode is **hand-started** | `now + choice.minutes`, null for `Never` |
 * | The window's end | The shade is up **inside** an enabled window | [Schedule.windowEnd], null outside |
 *
 * **The window's end applying to a hand-started episode is the row that earns the rule.** Start the
 * shade by hand at 06:00 with a four-hour auto-off under a 22:00-to-07:00 window: the candidates are
 * 10:00 and 07:00, and without this the shade sits over the morning for three hours after the user
 * said *off at 07:00*.
 *
 * The whole thing is **monotone except at a start**: this function and the schedule's on-instant are
 * the only two moments a deadline may move *outward*; every other writer may only bring it forward.
 */
fun deadlineFor(
    now: Long,
    zone: ZoneId,
    start: ShadeStart,
    autoOff: AutoOff,
    schedule: Schedule,
): Long? {
    val fromHand = if (start == ShadeStart.ByHand) autoOffDeadline(now, autoOff) else null
    val fromWindow = schedule.windowEnd(now, zone)
    return when {
        fromHand == null -> fromWindow
        fromWindow == null -> fromHand
        else -> min(fromHand, fromWindow)
    }
}

/**
 * The on-instant this ending spends, or `null` to leave the marker alone.
 *
 * An episode the *person* owns — Stop, or a deadline they were promised — spends the night it ended
 * in, so the reconcile does not put the shade straight back at the next process start. An episode
 * reaped by the boot receiver spends nothing, because nobody decided anything.
 *
 * Outside a window there is no night to spend, and [Schedule.windowStart] answering null is what
 * says so — which is why there is no separate check for it here.
 */
internal fun nightSpentBy(
    reason: ShadeEnd,
    schedule: Schedule,
    now: Long,
    zone: ZoneId,
): Long? = if (reason == ShadeEnd.Reaped) null else schedule.windowStart(now, zone)

/**
 * A stored deadline brought **forward** to the window's end, never pushed out.
 *
 * The direction is what makes this safe in every case at once: an off-time moved *later* leaves the
 * stored value alone, and so does disabling the schedule, because [Schedule.windowEnd] is then null
 * or further out. Both err toward light. It preserves an explicit override for the same reason —
 * `min(23:30, 07:00)` is still 23:30 — and it is idempotent, so calling it on every process start
 * costs nothing.
 *
 * `null` means *no deadline*, which is unbounded rather than zero, so it is the one value the window
 * always wins against.
 */
internal fun tightenedDeadline(
    stored: Long?,
    schedule: Schedule,
    now: Long,
    zone: ZoneId,
): Long? {
    val end = schedule.windowEnd(now, zone) ?: return stored
    return if (stored == null) end else min(stored, end)
}

/**
 * Read both settings, resolve every live deadline, and store the earliest with the flag.
 *
 * **An extension in `shade/` rather than a method on [AppPreferences].** That class is a store: its
 * job is keys, defaults and transactions, and the one piece of policy already in it — collapsing the
 * no-deadline sentinel to null — is there because it is the boundary that owns the sentinel. Which
 * deadline wins is not storage policy, it is the shade's, and it belongs beside the pure function it
 * calls. The default arguments are what keep four call sites to one line each.
 */
suspend fun AppPreferences.beginShadeAt(
    start: ShadeStart,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    beginShade(deadlineFor(now, zone, start, autoOff.first(), schedule.first()))
}

/** Clear the intent, and spend the night if this ending was one the person owns. */
suspend fun AppPreferences.endShadeAt(
    reason: ShadeEnd,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    endShade(nightSpentBy(reason, schedule.first(), now, zone))
}

/**
 * With a shade up, bring the stored deadline forward to the window's end if that is sooner.
 *
 * Without this, editing the schedule under a live shade does nothing until the next on-instant: a
 * scheduled shade up at 22:30 with a deadline of 07:00, an off-time moved to 23:00, and the shade
 * stays up until morning against a promise the user has just withdrawn. The pure function would
 * never have been called, so no test of it would have noticed.
 *
 * `beginShade` rather than a deadline setter, because there is no deadline setter — the flag and the
 * deadline are written as a pair — and `running` is already true here by the guard above it.
 */
suspend fun AppPreferences.tightenToWindow(
    schedule: Schedule,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val intent = shadeIntentNow()
    if (!intent.running) return
    val tightened = tightenedDeadline(intent.offAtMillis, schedule, now, zone)
    if (tightened != intent.offAtMillis) beginShade(tightened)
}
