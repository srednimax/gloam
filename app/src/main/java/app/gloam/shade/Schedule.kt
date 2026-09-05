package app.gloam.shade

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The nightly window: on at one time, off at another (CONTEXT.md: **schedule**).
 *
 * **One pair, not two independent switches**, and the type is what says so. A pair of times can be
 * asked *is it inside* and *when does it next open*; two switches can only be asked twice and
 * reconciled by whoever is asking. That difference is the whole reason the degenerate case below has
 * one answer rather than a convention every call site has to remember.
 *
 * **A fixed set of three values, so it is three settings and not a list** — `CLAUDE.md`'s
 * cardinality test, the same one that keeps [AutoOff] in DataStore. Seven per-day windows would be
 * fourteen settings by that test and still no database; what keeps them out is size rather than
 * storage, and `docs/phase-4.md` says so where a future reader will look for it.
 *
 * Kotlin note: `java.time` is available unconditionally at `minSdk` 33 — it desugars from API 26 and
 * Gloam ships to nothing below 33 (ADR-0008) — so there is no `ThreeTenABP`, no desugaring flag and
 * no `Calendar` anywhere in this file. [LocalTime] is a wall-clock time with **no date and no zone**:
 * exactly the thing the user set, and deliberately not something you can subtract from an instant.
 */
data class Schedule(
    val enabled: Boolean,
    val onAt: LocalTime,
    val offAt: LocalTime,
)

/**
 * Whether this schedule can ever open at all.
 *
 * **`onAt == offAt` is an empty window, never a twenty-four-hour one.** Both readings are available
 * and only one of them is safe: *on at 22:00, off at 22:00* read as a full day is a shade that never
 * comes down — the single failure this app exists to design out, reached by a user who tapped the
 * same time twice. Read as empty it is a schedule that does nothing, which is visible, harmless and
 * obviously wrong to the person who set it. The screen refuses to save it with a sentence saying
 * why; this is the floor under that refusal rather than a substitute for it.
 *
 * A disabled schedule answers identically, so **no caller needs to check the flag** before asking
 * any of the four questions below.
 */
private val Schedule.isActive: Boolean get() = enabled && onAt != offAt

/**
 * Is the window open at [now]? Half-open: 22:00 is inside it, 07:00 is not.
 *
 * Three shapes, and the third is the one a nightly reading window actually has:
 *
 * | `onAt` vs `offAt` | Shape | Inside when |
 * | --- | --- | --- |
 * | `on < off` | Same day — 13:00 to 17:00 | `on <= t < off` |
 * | `on > off` | **Crosses midnight** — 22:00 to 07:00 | `t >= on` **or** `t < off` |
 * | `on == off` | Degenerate | **Never** — see [isActive] |
 *
 * **Half-open, `[on, off)`, and it agrees with [isDue] by construction.** `isDue` treats a deadline
 * that is exactly now as due, so a window whose end did not agree would fire the deadline a minute
 * after this function had already gone false.
 */
fun Schedule.contains(
    now: Long,
    zone: ZoneId,
): Boolean {
    if (!isActive) return false
    val t = now.localTimeIn(zone)
    return if (onAt < offAt) t >= onAt && t < offAt else t >= onAt || t < offAt
}

/**
 * The next instant **strictly after** [now] at which the window opens, or `null` if it never does.
 *
 * **Strictly after is not tidiness, it is what stops the alarm from looping.** The schedule's own
 * receiver re-arms from inside the broadcast the alarm just delivered, so a `nextOn` that could
 * answer *now* is an alarm that re-arms itself for the instant it is already at — a broadcast loop
 * on a user's phone, at 22:00, with a foreground-service start inside it.
 */
fun Schedule.nextOn(
    now: Long,
    zone: ZoneId,
): Long? = if (!isActive) null else nextOccurrence(onAt, now, zone)

/**
 * The instant the window containing [now] closes, or `null` when [now] is outside it.
 *
 * This is the value the schedule writes into `off_at_millis`, so it is *the deadline* rather than a
 * fact about the window — which is why it answers null outside the window rather than answering
 * tomorrow's close. A shade started by hand at 13:00 under a 22:00-to-07:00 window is bounded by
 * auto-off and by nothing else, and a non-null answer here would silently make it bounded by a
 * window it is not in.
 */
fun Schedule.windowEnd(
    now: Long,
    zone: ZoneId,
): Long? {
    if (!contains(now, zone)) return null
    val today = now.localDateIn(zone)
    // Crossing midnight and already past `onAt` means the close is tomorrow's; every other way of
    // being inside the window closes on the day we are already in.
    val day = if (onAt > offAt && now.localTimeIn(zone) >= onAt) today.plusDays(1) else today
    return day.atLocal(offAt, zone)
}

/**
 * The instant the window containing [now] opened, or `null` when [now] is outside it.
 *
 * **The identity of a night**, and that is what it is for. The schedule's reconcile has to tell
 * *this window has not been acted on* from *this window was acted on and the user pressed Stop*, and
 * comparing the marker it stored against the on-instant of the window it is looking at is what
 * separates them. The property that makes it usable is that it is **constant across every minute of
 * one window** — a version that drifted by a minute would let the reconcile raise the same night's
 * shade over and over — and the sweep asserts exactly that rather than trusting the derivation.
 *
 * The exact mirror of [windowEnd]: same helper, same null outside the window.
 */
fun Schedule.windowStart(
    now: Long,
    zone: ZoneId,
): Long? {
    if (!contains(now, zone)) return null
    val today = now.localDateIn(zone)
    // Crossing midnight and *before* `offAt` means we are in the small hours of a window that opened
    // yesterday. Every other way of being inside it opened on the day we are in.
    val day = if (onAt > offAt && now.localTimeIn(zone) < offAt) today.minusDays(1) else today
    return day.atLocal(onAt, zone)
}

/**
 * The first instant strictly after [now] whose local time is [time].
 *
 * Walks candidate days and takes the first strictly-future one. **Three days rather than two**,
 * because a daylight-saving gap can push a day's occurrence forward by the size of the gap, so
 * today's candidate can land after the naive reading of tomorrow's.
 */
private fun nextOccurrence(
    time: LocalTime,
    now: Long,
    zone: ZoneId,
): Long {
    var day = now.localDateIn(zone)
    repeat(DAYS_WALKED) {
        val candidate = day.atLocal(time, zone)
        if (candidate > now) return candidate
        day = day.plusDays(1)
    }
    // Unreachable: a local time recurs every day, so the third candidate is at least 24 hours out.
    // Answering the last candidate rather than throwing, because the caller of last resort is an
    // alarm being armed on somebody's phone at 22:00.
    return day.atLocal(time, zone)
}

private const val DAYS_WALKED = 3

/**
 * A local date and a wall-clock time resolved to an instant **in this zone, on this day**.
 *
 * The one place daylight saving is dealt with, and the behaviour on the two irregular days is
 * `java.time`'s rather than ours:
 *
 * - **Spring forward.** In `Europe/Warsaw` on the last Sunday in March, 02:00 to 02:59 do not exist.
 *   `atZone` resolves a gap **forwards by the size of the gap**, so a schedule set to come on at
 *   02:30 comes on at 03:30 that one night. Deterministic and documented; the alternatives —
 *   throwing, or skipping the night — are worse in both directions.
 * - **Autumn back.** On the last Sunday in October, 02:30 happens twice, and `atZone` resolves an
 *   overlap to the **earlier** offset. So the shade comes on at the first 02:30 rather than the
 *   second — and, in the mirror case, a window ending at 02:30 ends at the first one, which can put
 *   [windowEnd] in the past while the second 02:30 is still an hour away. That errs toward light,
 *   which is the direction every rule in this app errs in when the two failures are asymmetric.
 * - **The window's length changes by an hour on those two nights**, in both directions, and that is
 *   correct rather than a bug to compensate for: the user said *off at 07:00*, and 07:00 is when the
 *   clock says 07:00.
 *
 * Kotlin note: `ZoneId` is the function between a wall-clock time and a point on the timeline, and
 * it is **not injective in either direction** — one local time can be zero instants or two. That is
 * why everything here goes through `atZone` and takes what it resolves to, rather than doing
 * arithmetic on minutes. JS has one `Date` doing all three jobs and a `Temporal` proposal to fix
 * exactly this; Kotlin has had the fixed version since API 26.
 */
private fun LocalDate.atLocal(
    time: LocalTime,
    zone: ZoneId,
): Long = atTime(time).atZone(zone).toInstant().toEpochMilli()

private fun Long.localTimeIn(zone: ZoneId): LocalTime = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

private fun Long.localDateIn(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * Minutes since local midnight, as the wall-clock time the user set.
 *
 * Coerced rather than validated, the way `dim_level` and `warmth` are: a value outside `0..1439` can
 * only arrive from a build with a different range, and an `Int` cannot fail to parse — which is the
 * whole reason storage holds a number here rather than a formatted string.
 */
fun timeOf(minutes: Int): LocalTime = LocalTime.ofSecondOfDay(minutes.coerceIn(0, MINUTES_PER_DAY - 1) * 60L)

/** The inverse of [timeOf]: the wall-clock time as the number storage keeps. */
fun minutesOf(time: LocalTime): Int = time.hour * 60 + time.minute

private const val MINUTES_PER_DAY = 24 * 60
