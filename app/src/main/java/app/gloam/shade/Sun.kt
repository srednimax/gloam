package app.gloam.shade

import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * A place on the earth in degrees, north and east positive. It is either a lent location or an
 * estimated one (CONTEXT.md: **location**), and the sun doesn't care which.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Sunrise and sunset on one day at one place, as epoch millis. Either is `null` when the sun does not
 * do that on that day, which is the polar case ADR-0013 falls back to the fixed times for.
 *
 * The two are nullable **independently**. On the last day before the polar night the sun still rises
 * and sets, and the next day it does neither, but the refinement below can find one edge of a day and
 * not the other. The caller asks about the event it needs rather than inferring one from the other.
 */
data class SunTimes(
    val rise: Long?,
    val set: Long?,
)

/**
 * When the sun rises and sets on [day] at [latitude], [longitude] (degrees, north and east positive).
 *
 * **Sunset means what weather apps print** (ADR-0013 §3): the sun's upper edge on the horizon with
 * standard refraction, a centre altitude of −0.833°. The equations are NOAA's solar calculator's
 * (Meeus, *Astronomical Algorithms*), which NOAA gives as good to about a minute between 72° S and
 * 72° N. Nothing here is a library or a network call.
 *
 * **[day] is the sun's day at this longitude, not a calendar date in any time zone.** It is the day
 * whose local mean noon, 12:00 UTC shifted by `longitude / 15` hours, falls on that date. For almost
 * every zone that is the calendar date too, because a zone is drawn roughly around its meridian. Where
 * a zone is drawn far from it, the two can differ by a day. Samoa keeps UTC+13 at 172° W, so its
 * calendar 21 June is the sun's 20 June there. The schedule matches nights by instant rather than by
 * date for exactly that reason.
 *
 * Results are **whole minutes**. NOAA's accuracy is a minute, so seconds would be noise, and a whole
 * minute is what the schedule screen prints. The instant the shade comes on and the time the screen
 * shows then agree under any formatter, rounding or truncating.
 */
fun sunTimes(
    day: LocalDate,
    latitude: Double,
    longitude: Double,
): SunTimes {
    val place = Coordinates(latitude, longitude)
    return SunTimes(rise = sunrise(day, place), set = sunset(day, place))
}

/** [sunTimes]'s rise alone. The schedule needs one event from each of two days, so it asks for one. */
fun sunrise(
    day: LocalDate,
    place: Coordinates,
): Long? = sunEvent(day, place.latitude, place.longitude, rising = true)

/** [sunTimes]'s set alone. */
fun sunset(
    day: LocalDate,
    place: Coordinates,
): Long? = sunEvent(day, place.latitude, place.longitude, rising = false)

/**
 * One event, found by evaluating the sun's position at a guess and moving the guess to the answer.
 *
 * The first guess is local mean noon. Each pass evaluates declination and the equation of time at the
 * previous answer, so the answer is the sun's position at the moment of the event rather than at
 * midnight. NOAA's own calculator makes one such pass from 0h UT; starting from noon is at most twelve
 * hours from the event wherever the longitude is, and [PASSES] converges well inside a second.
 *
 * `null` as soon as any pass finds the sun never reaches −0.833° that day. Answering the last pass
 * that did find one would be answering about a different day.
 */
private fun sunEvent(
    day: LocalDate,
    latitude: Double,
    longitude: Double,
    rising: Boolean,
): Long? {
    val julianDayAtMidnight = day.toEpochDay() + JULIAN_DAY_AT_UNIX_EPOCH
    var minutes = MINUTES_AT_NOON - MINUTES_PER_DEGREE * longitude
    repeat(PASSES) {
        val julianDay = julianDayAtMidnight + minutes / MINUTES_PER_DAY
        minutes = eventMinutesAfterMidnightUtc(julianDay, latitude, longitude, rising) ?: return null
    }
    // Minutes from 0h UTC on [day]. Below zero or past 1440 is correct, not an overflow: Kamchatka's
    // sunrise is the previous UTC date, and Honolulu's sunset is the next one.
    return day.toEpochDay() * MILLIS_PER_DAY + minutes.roundToLong() * MILLIS_PER_MINUTE
}

/**
 * NOAA's `calcSunriseSetUTC`: the event's minutes after 0h UTC, with the sun's position taken at
 * [julianDay].
 *
 * Kotlin note: every call is `StrictMath`, not `kotlin.math` (which is `java.lang.Math`). `Math.sin`
 * may be replaced by a CPU intrinsic that differs in the last bit, and on Android an interpreted call
 * and the compiled version of the same line need not agree. Last bits matter here because an instant
 * computed at 22:00 is stored as a night's identity and compared with one computed again after a
 * reboot (ADR-0013's consequences). `StrictMath` gives the same bits on every run and every device.
 */
private fun eventMinutesAfterMidnightUtc(
    julianDay: Double,
    latitude: Double,
    longitude: Double,
    rising: Boolean,
): Double? {
    val t = (julianDay - J2000) / DAYS_PER_JULIAN_CENTURY

    val meanLongitude = normalizeDegrees(280.46646 + t * (36000.76983 + t * 0.0003032))
    val meanAnomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t)
    val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

    val m = rad(meanAnomaly)
    val equationOfCentre =
        StrictMath.sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            StrictMath.sin(2 * m) * (0.019993 - 0.000101 * t) +
            StrictMath.sin(3 * m) * 0.000289
    val trueLongitude = meanLongitude + equationOfCentre
    val omega = 125.04 - 1934.136 * t
    val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * StrictMath.sin(rad(omega))

    val meanObliquitySeconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
    val meanObliquity = 23.0 + (26.0 + meanObliquitySeconds / 60.0) / 60.0
    val obliquity = meanObliquity + 0.00256 * StrictMath.cos(rad(omega))

    val declination = StrictMath.asin(StrictMath.sin(rad(obliquity)) * StrictMath.sin(rad(apparentLongitude)))

    val y = StrictMath.tan(rad(obliquity) / 2).let { it * it }
    val l0 = rad(meanLongitude)
    val equationOfTimeRadians =
        y * StrictMath.sin(2 * l0) -
            2 * eccentricity * StrictMath.sin(m) +
            4 * eccentricity * y * StrictMath.sin(m) * StrictMath.cos(2 * l0) -
            0.5 * y * y * StrictMath.sin(4 * l0) -
            1.25 * eccentricity * eccentricity * StrictMath.sin(2 * m)
    val equationOfTimeMinutes = StrictMath.toDegrees(equationOfTimeRadians) * MINUTES_PER_DEGREE

    val phi = rad(latitude)
    val cosHourAngle =
        StrictMath.cos(rad(ZENITH_AT_SUNSET)) / (StrictMath.cos(phi) * StrictMath.cos(declination)) -
            StrictMath.tan(phi) * StrictMath.tan(declination)
    // Past ±1 the sun stays below the horizon all day (> 1) or above it (< −1): no event to find.
    if (cosHourAngle !in -1.0..1.0) return null
    val hourAngle = StrictMath.toDegrees(StrictMath.acos(cosHourAngle))

    val shift = if (rising) hourAngle else -hourAngle
    return MINUTES_AT_NOON - MINUTES_PER_DEGREE * (longitude + shift) - equationOfTimeMinutes
}

private fun rad(degrees: Double): Double = StrictMath.toRadians(degrees)

private fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

/** 90° plus 50′: 34′ of standard refraction and the sun's 16′ semi-diameter. */
private const val ZENITH_AT_SUNSET = 90.833

private const val PASSES = 3
private const val J2000 = 2451545.0
private const val DAYS_PER_JULIAN_CENTURY = 36525.0
private const val JULIAN_DAY_AT_UNIX_EPOCH = 2440587.5

/** The earth turns one degree every four minutes. */
private const val MINUTES_PER_DEGREE = 4.0
private const val MINUTES_AT_NOON = 720.0
private const val MINUTES_PER_DAY = 1440.0
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 86_400_000L
