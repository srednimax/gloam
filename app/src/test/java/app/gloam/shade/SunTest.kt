package app.gloam.shade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * The sun, checked against somebody else's answer rather than against its own arithmetic.
 *
 * **Every expected time comes from the US Naval Observatory** (`aa.usno.navy.mil/api/rstt/oneday`,
 * read 2026-09-14). That is an independent implementation, not NOAA's equations run a second time.
 * USNO prints whole minutes, and NOAA's stated accuracy is a minute, so the tolerance is one minute
 * each way.
 *
 * The rows are chosen for what breaks, not for coverage of the map:
 *
 * - both solstices at the latitude the app is used at, plus the spring-forward day;
 * - a western longitude, whose sunset falls on the next UTC date;
 * - the southern hemisphere, where December is summer;
 * - a zone drawn far from its meridian (Kashgar on Beijing time);
 * - Samoa, where the calendar date and the sun's date differ by one;
 * - Tromsø, where the answer is that there is no answer.
 */
class SunTest {
    private fun assertNear(
        what: String,
        expectedLocal: String,
        zone: ZoneId,
        actual: Long?,
    ) {
        assertNotNull("$what: expected $expectedLocal, got no event", actual)
        val expected =
            LocalDateTime
                .parse(expectedLocal)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        val off = actual!! - expected
        assertTrue("$what: ${off / 1000}s from USNO's $expectedLocal in $zone", abs(off) <= 60_000L)
    }

    private fun assertEvents(
        place: String,
        day: String,
        latitude: Double,
        longitude: Double,
        zone: String,
        rise: String,
        set: String,
    ) {
        val times = sunTimes(LocalDate.parse(day), latitude, longitude)
        assertNear("$place sunrise", rise, ZoneId.of(zone), times.rise)
        assertNear("$place sunset", set, ZoneId.of(zone), times.set)
    }

    @Test
    fun `Warsaw at both solstices and on the spring-forward day`() {
        assertEvents("Warsaw", "2026-12-21", 52.25, 21.0, "Europe/Warsaw", "2026-12-21T07:43", "2026-12-21T15:25")
        assertEvents("Warsaw", "2026-06-21", 52.25, 21.0, "Europe/Warsaw", "2026-06-21T04:14", "2026-06-21T21:01")
        assertEvents("Warsaw", "2026-03-29", 52.25, 21.0, "Europe/Warsaw", "2026-03-29T06:18", "2026-03-29T19:05")
    }

    @Test
    fun `a western longitude whose sunset is on the next UTC date`() {
        assertEvents(
            "Honolulu",
            "2026-06-21",
            21.31,
            -157.86,
            "Pacific/Honolulu",
            "2026-06-21T05:50",
            "2026-06-21T19:16",
        )
    }

    @Test
    fun `the southern hemisphere in its summer`() {
        assertEvents(
            "Sydney",
            "2026-12-21",
            -33.87,
            151.21,
            "Australia/Sydney",
            "2026-12-21T05:41",
            "2026-12-21T20:05",
        )
    }

    @Test
    fun `a zone drawn far from its meridian`() {
        assertEvents("Kashgar", "2026-06-21", 39.47, 75.99, "Asia/Shanghai", "2026-06-21T07:29", "2026-06-21T22:27")
    }

    /**
     * The row that pins down what `day` means. Samoa's calendar 21 June is the sun's 20 June at 172° W,
     * so asking for the calendar date would return the day after the one on the clock.
     */
    @Test
    fun `in Samoa the sun's day is the calendar date before`() {
        assertEvents("Apia", "2026-06-20", -13.83, -171.76, "Pacific/Apia", "2026-06-21T06:49", "2026-06-21T18:08")
    }

    @Test
    fun `Tromso has no sunset in the midnight sun and no sunrise in the polar night`() {
        for (day in listOf("2026-05-20", "2026-06-21", "2026-12-21")) {
            val times = sunTimes(LocalDate.parse(day), 69.65, 18.96)
            assertNull("Tromsø sunrise on $day", times.rise)
            assertNull("Tromsø sunset on $day", times.set)
        }
    }

    /**
     * Every day of a year at Warsaw's latitude: both events exist, sunrise comes first, and both are
     * whole minutes. Whole minutes matter because the screen prints the instant the shade uses.
     */
    @Test
    fun `a year at 52 degrees north has an ordinary day every day`() {
        var day = LocalDate.parse("2026-01-01")
        while (day.year == 2026) {
            val times = sunTimes(day, 52.25, 21.0)
            val rise = times.rise
            val set = times.set
            assertNotNull("no sunrise on $day", rise)
            assertNotNull("no sunset on $day", set)
            assertTrue("sunrise after sunset on $day", rise!! < set!!)
            assertEquals("sunrise on $day is not a whole minute", 0L, rise % 60_000L)
            assertEquals("sunset on $day is not a whole minute", 0L, set % 60_000L)
            day = day.plusDays(1)
        }
    }
}
