package app.gloam.shade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * The window, proven by sweeping time rather than by reasoning about midnight.
 *
 * **This is `ShadeRampTest`'s idiom applied to a clock**, and for the same reason: the failures here
 * are silent. A window that answers *inside* one minute after it closed leaves a shade up over
 * somebody's morning; one that answers *outside* at 22:00 is a schedule that simply never runs, on a
 * screen still saying it is on. Neither is visible from a handful of examples chosen by the person
 * who wrote the arithmetic.
 *
 * `Schedule.kt` has no Android imports, which is what makes a JVM sweep possible at all.
 *
 * **Every zone is a parameter here, and that is the point of the signature.** A pure function that
 * read `ZoneId.systemDefault()` would be a function whose test results depend on the machine CI
 * happens to run on — and the two daylight-saving rows below cannot be written at all against a zone
 * nobody chose.
 */
class ScheduleTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")

    /** A half-hour offset, as a control: an arithmetic bug that rounds to the hour survives UTC. */
    private val kolkata = ZoneId.of("Asia/Kolkata")

    private val zones = listOf(warsaw, ZoneOffset.UTC, kolkata)

    /** The overnight window the whole feature is shaped around, and a same-day one beside it. */
    private val overnight = Schedule(enabled = true, onAt = LocalTime.of(22, 0), offAt = LocalTime.of(7, 0))
    private val daytime = Schedule(enabled = true, onAt = LocalTime.of(13, 0), offAt = LocalTime.of(17, 0))

    private fun at(
        date: String,
        time: String,
        zone: ZoneId,
    ): Long =
        LocalDateTime
            .parse("${date}T$time")
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun Long.timeIn(zone: ZoneId): LocalTime = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

    private fun Long.dateIn(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    // ---------------------------------------------------------------- the three shapes, at the edges

    @Test
    fun `a same-day window is half-open at both ends`() {
        for (zone in zones) {
            val day = "2026-06-15"
            assertFalse("12:59", daytime.contains(at(day, "12:59", zone), zone))
            assertTrue("13:00 is inside", daytime.contains(at(day, "13:00", zone), zone))
            assertTrue("15:00", daytime.contains(at(day, "15:00", zone), zone))
            assertFalse("17:00 is outside", daytime.contains(at(day, "17:00", zone), zone))
            assertFalse("17:01", daytime.contains(at(day, "17:01", zone), zone))
        }
    }

    @Test
    fun `a window crossing midnight is inside on both sides of it`() {
        for (zone in zones) {
            val day = "2026-06-15"
            assertFalse("21:59", overnight.contains(at(day, "21:59", zone), zone))
            assertTrue("22:00 is inside", overnight.contains(at(day, "22:00", zone), zone))
            assertTrue("23:59", overnight.contains(at(day, "23:59", zone), zone))
            assertTrue("00:00", overnight.contains(at(day, "00:00", zone), zone))
            assertTrue("06:59", overnight.contains(at(day, "06:59", zone), zone))
            assertFalse("07:00 is outside", overnight.contains(at(day, "07:00", zone), zone))
        }
    }

    @Test
    fun `a window that starts and ends at the same moment never opens`() {
        val degenerate = Schedule(enabled = true, onAt = LocalTime.of(22, 0), offAt = LocalTime.of(22, 0))
        for (zone in zones) {
            for (hour in 0..23) {
                val now = at("2026-06-15", "%02d:30".format(hour), zone)
                assertFalse("contains at $hour", degenerate.contains(now, zone))
                assertNull("nextOn at $hour", degenerate.nextOn(now, zone))
                assertNull("windowEnd at $hour", degenerate.windowEnd(now, zone))
                assertNull("windowStart at $hour", degenerate.windowStart(now, zone))
            }
        }
    }

    @Test
    fun `a disabled schedule answers exactly as an empty one does`() {
        val off = overnight.copy(enabled = false)
        for (zone in zones) {
            for (hour in 0..23) {
                val now = at("2026-06-15", "%02d:30".format(hour), zone)
                assertFalse("contains at $hour", off.contains(now, zone))
                assertNull("nextOn at $hour", off.nextOn(now, zone))
                assertNull("windowEnd at $hour", off.windowEnd(now, zone))
                assertNull("windowStart at $hour", off.windowStart(now, zone))
            }
        }
    }

    // ------------------------------------------------------------------------------- the day sweep

    /**
     * Every minute of one ordinary day, against both shapes and all three zones.
     *
     * The two invariants are asserted **together** rather than one test each, because the useful
     * claim is the equivalence: `windowEnd` is non-null *exactly* when `contains` is true. Testing
     * either side alone passes on a build where one of them was refactored and the other was not.
     */
    @Test
    fun `a day of minutes holds every invariant at once`() {
        for (zone in zones) {
            for (schedule in listOf(overnight, daytime)) {
                val start = at("2026-06-15", "00:00", zone)
                for (minute in 0 until 24 * 60) {
                    val now = start + minute * 60_000L
                    assertInvariants(schedule, now, zone)
                }
            }
        }
    }

    /**
     * A whole year of `Europe/Warsaw` at one-minute granularity, so **both daylight-saving
     * transitions are inside the sweep** rather than being two dates somebody has to remember to
     * update. It also carries the property the reconcile depends on: `windowStart` is constant
     * across every minute of one window.
     */
    @Test
    fun `a year of Warsaw minutes, both clock changes included`() {
        for (schedule in listOf(overnight, daytime)) {
            val end = at("2027-01-01", "00:00", warsaw)
            var openWindow: Long? = null
            var now = at("2026-01-01", "00:00", warsaw)
            while (now < end) {
                assertInvariants(schedule, now, warsaw)

                // The night's identity: it may not move while the window it identifies is open.
                val here = schedule.windowStart(now, warsaw)
                if (here == null) {
                    openWindow = null
                } else {
                    if (openWindow != null) {
                        assertEquals("windowStart drifted inside one window at $now", openWindow, here)
                    }
                    openWindow = here
                }
                now += 60_000L
            }
        }
    }

    /**
     * The invariants every minute of every sweep has to satisfy.
     *
     * `nextOn > now` is the one that stops the alarm from looping — the receiver re-arms from inside
     * the broadcast it is handling, so an answer of *now* would be a broadcast loop with a
     * foreground-service start in it.
     */
    private fun assertInvariants(
        schedule: Schedule,
        now: Long,
        zone: ZoneId,
    ) {
        val inside = schedule.contains(now, zone)
        val next = schedule.nextOn(now, zone)
        assertNotNull("nextOn is null on an active schedule at $now", next)
        assertTrue("nextOn ($next) is not strictly after $now in $zone", next!! > now)
        assertOnAtOrGap(schedule, next, zone)

        val end = schedule.windowEnd(now, zone)
        val begun = schedule.windowStart(now, zone)
        assertEquals("$now in $zone: windowEnd disagrees with contains", inside, end != null)
        assertEquals("$now in $zone: windowStart disagrees with contains", inside, begun != null)
        if (inside) {
            assertTrue("$now: the window did not open before now", begun!! <= now)
        }
    }

    /**
     * An on-instant's local time is `onAt` — **or** that date's daylight-saving gap swallowed it.
     *
     * Stated as a claim about the zone's own rules rather than as a hardcoded date, so the sweep
     * keeps its strength: the only licensed disagreement is a wall-clock time that does not exist on
     * the day it landed on, which `atZone` resolves forward by the size of the gap. Anything else
     * is a bug, and asserting "some time" instead would hide it.
     */
    private fun assertOnAtOrGap(
        schedule: Schedule,
        instant: Long,
        zone: ZoneId,
    ) {
        val resolved = instant.timeIn(zone)
        if (resolved == schedule.onAt) return
        val date = instant.dateIn(zone)
        val gapped = zone.rules.getValidOffsets(date.atTime(schedule.onAt)).isEmpty()
        assertTrue("$instant resolved to $resolved rather than ${schedule.onAt} in $zone", gapped)
    }

    // ------------------------------------------------------------------------- daylight saving, named

    @Test
    fun `an on-time inside the spring gap resolves forward by the gap`() {
        // Warsaw, 2026-03-29: 02:00 to 02:59 do not exist.
        val gapped = Schedule(enabled = true, onAt = LocalTime.of(2, 30), offAt = LocalTime.of(7, 0))
        val next = gapped.nextOn(at("2026-03-29", "00:30", warsaw), warsaw)!!
        assertEquals("the shade comes on at 03:30 that one night", LocalTime.of(3, 30), next.timeIn(warsaw))
    }

    @Test
    fun `an on-time inside the autumn overlap takes the earlier offset`() {
        // Warsaw, 2026-10-25: 02:30 happens twice, at +02:00 and then at +01:00.
        val overlapped = Schedule(enabled = true, onAt = LocalTime.of(2, 30), offAt = LocalTime.of(7, 0))
        val next = overlapped.nextOn(at("2026-10-25", "00:30", warsaw), warsaw)!!
        val resolved = Instant.ofEpochMilli(next).atZone(warsaw)
        assertEquals(LocalTime.of(2, 30), resolved.toLocalTime())
        assertEquals("the first 02:30, not the second", ZoneOffset.ofHours(2), resolved.offset)
    }

    /**
     * The mirror of the row above, asserted rather than left to be discovered.
     *
     * A window *ending* inside the autumn overlap ends at the **first** 02:30, so during the second
     * pass through those minutes `contains` is still true while `windowEnd` is already in the past.
     * The deadline is therefore due the moment it is read, and the window ends an hour early on that
     * one night. It errs toward light, which is the direction this app errs in whenever the two
     * failures are asymmetric — and it is written down here so nobody debugs it as a mystery.
     */
    @Test
    fun `a window ending inside the autumn overlap ends at the first pass`() {
        val ends = Schedule(enabled = true, onAt = LocalTime.of(22, 0), offAt = LocalTime.of(2, 30))
        // The second 02:15, which is 01:15 UTC.
        val secondPass = LocalDateTime.parse("2026-10-25T01:15").toInstant(ZoneOffset.UTC).toEpochMilli()
        assertTrue("still inside by wall clock", ends.contains(secondPass, warsaw))
        assertTrue("but the end has already gone", ends.windowEnd(secondPass, warsaw)!! < secondPass)
    }

    // ----------------------------------------------------------------------------- windowStart, named

    @Test
    fun `the small hours belong to the window that opened yesterday`() {
        val night = at("2026-06-16", "02:00", warsaw)
        assertEquals(at("2026-06-15", "22:00", warsaw), overnight.windowStart(night, warsaw))
        assertEquals(at("2026-06-16", "07:00", warsaw), overnight.windowEnd(night, warsaw))
    }

    @Test
    fun `the evening belongs to the window that opened today`() {
        val evening = at("2026-06-15", "23:00", warsaw)
        assertEquals(at("2026-06-15", "22:00", warsaw), overnight.windowStart(evening, warsaw))
        assertEquals(at("2026-06-16", "07:00", warsaw), overnight.windowEnd(evening, warsaw))
    }

    // ------------------------------------------------------------------------------ sunset to sunrise

    private val oslo = ZoneId.of("Europe/Oslo")
    private val newYork = ZoneId.of("America/New_York")
    private val tromso = Coordinates(69.65, 18.96)

    /** Warsaw on its zone's estimate, which tzdata puts at Warsaw. */
    private val sunOvernight = overnight.copy(kind = ScheduleKind.SunsetToSunrise)

    /** Tromsø has to be lent. Its zone is Oslo's, and Oslo, the estimate, has a sunset every day. */
    private val sunTromso = sunOvernight.copy(lent = Location.Lent(tromso, "Europe/Oslo"))

    /**
     * A fixed pair that overlaps a polar sunrise, so the sweep reaches the clip between a sun night and
     * the fallback night after it. An overnight pair never does.
     */
    private val sunTromsoMorning = sunTromso.copy(onAt = LocalTime.of(6, 0), offAt = LocalTime.of(9, 0))

    private fun Long.isNear(
        local: String,
        zone: ZoneId,
    ) = abs(
        this -
            LocalDateTime
                .parse(local)
                .atZone(zone)
                .toInstant()
                .toEpochMilli(),
    ) <= 60_000L

    @Test
    fun `a winter night in Warsaw runs from sunset to the next sunrise`() {
        val place = sunOvernight.locationIn(warsaw)!!.coordinates
        val afternoon = at("2026-12-21", "14:00", warsaw)
        val evening = at("2026-12-21", "18:00", warsaw)

        assertFalse("before sunset", sunOvernight.contains(afternoon, warsaw))
        assertEquals(sunset(LocalDate.parse("2026-12-21"), place), sunOvernight.nextOn(afternoon, warsaw))

        val start = sunOvernight.windowStart(evening, warsaw)!!
        assertTrue(
            "USNO's 15:25, a minute either way, not ${start.timeIn(warsaw)}",
            start.isNear("2026-12-21T15:25", warsaw),
        )
        assertEquals(sunrise(LocalDate.parse("2026-12-22"), place), sunOvernight.windowEnd(evening, warsaw))

        // What the screen shows, and it shows it on a schedule that is off and still on fixed times.
        assertEquals(
            Night(
                sunOvernight.nextOn(afternoon, warsaw)!!,
                sunOvernight.windowEnd(evening, warsaw)!!,
                followsSun = true,
            ),
            overnight.copy(enabled = false).tonight(afternoon, warsaw),
        )
    }

    /** Samoa's calendar date is a day ahead of the sun's there, so this is the row that catches a mix-up. */
    @Test
    fun `Samoa's evening uses the sunset on its own calendar date`() {
        val apia = ZoneId.of("Pacific/Apia")
        val start = sunOvernight.windowStart(at("2026-06-21", "20:00", apia), apia)!!
        assertTrue(
            "USNO's 18:08, not ${start.dateIn(apia)} ${start.timeIn(apia)}",
            start.isNear("2026-06-21T18:08", apia),
        )
    }

    @Test
    fun `a location lent in Warsaw is not used in New York`() {
        val lentInWarsaw = sunOvernight.copy(lent = Location.Lent(Coordinates(52.2, 21.0), "Europe/Warsaw"))
        assertTrue(lentInWarsaw.locationIn(warsaw) is Location.Lent)

        val estimate = lentInWarsaw.locationIn(newYork)
        assertTrue("New York gets its own estimate", estimate is Location.Estimated)
        val evening = at("2026-06-21", "23:00", newYork)
        assertEquals(
            sunset(LocalDate.parse("2026-06-21"), estimate!!.coordinates),
            lentInWarsaw.windowStart(evening, newYork),
        )
    }

    /** No location at all, and every night is the fixed pair, minute for minute. */
    @Test
    fun `in UTC a sun schedule is the fixed pair`() {
        val utc = ZoneOffset.UTC
        assertNull(sunOvernight.locationIn(utc))
        val start = at("2026-06-15", "00:00", utc)
        for (minute in 0 until 24 * 60) {
            val now = start + minute * 60_000L
            assertEquals("contains at $minute", overnight.contains(now, utc), sunOvernight.contains(now, utc))
            assertEquals("nextOn at $minute", overnight.nextOn(now, utc), sunOvernight.nextOn(now, utc))
            assertEquals("windowEnd at $minute", overnight.windowEnd(now, utc), sunOvernight.windowEnd(now, utc))
            assertEquals("windowStart at $minute", overnight.windowStart(now, utc), sunOvernight.windowStart(now, utc))
        }
    }

    @Test
    fun `Tromso falls back to the fixed pair in the midnight sun and the polar night, and only then`() {
        for (day in listOf("2026-06-21", "2026-12-21")) {
            val night = at(day, "23:00", oslo)
            assertEquals("$day opens at the fixed 22:00", at(day, "22:00", oslo), sunTromso.windowStart(night, oslo))
            val next = LocalDate.parse(day).plusDays(1).toString()
            assertEquals("$day closes at the fixed 07:00", at(next, "07:00", oslo), sunTromso.windowEnd(night, oslo))
        }
        val september = at("2026-09-14", "23:00", oslo)
        assertEquals(sunset(LocalDate.parse("2026-09-14"), tromso), sunTromso.windowStart(september, oslo))
    }

    @Test
    fun `a year of sun nights in Warsaw, both clock changes included`() {
        val nights = sweepSunYear(sunOvernight, warsaw)
        assertTrue("every Warsaw night follows the sun", nights.none { it.timeIn(warsaw) == overnight.onAt })
    }

    @Test
    fun `a year at 70 degrees north, where sun nights and fallback nights take turns`() {
        val nights = sweepSunYear(sunTromso, oslo)
        val fallback = nights.count { it.timeIn(oslo) == sunTromso.onAt }
        assertTrue("midnight sun and polar night fall back ($fallback nights)", fallback > 60)
        assertTrue("spring and autumn follow the sun (${nights.size - fallback} nights)", nights.size - fallback > 120)

        sweepSunYear(sunTromsoMorning, oslo)
    }

    /**
     * A year in 13-minute steps, returning every night seen. A minute sweep costs a hundred times the
     * fixed kind's here, because every question computes a few sunsets, and 13 is prime to 60, so the
     * samples fall on every minute of the hour over the year. The edges are probed exactly instead:
     * every `nextOn` is checked as the instant a night opens, and the millisecond before it.
     */
    private fun sweepSunYear(
        schedule: Schedule,
        zone: ZoneId,
    ): Set<Long> {
        val starts = mutableSetOf<Long>()
        var previousStart: Long? = null
        var previousEnd: Long? = null
        var now = at("2026-01-01", "00:00", zone)
        val end = at("2027-01-01", "00:00", zone)
        while (now < end) {
            val inside = schedule.contains(now, zone)
            val begun = schedule.windowStart(now, zone)
            val closes = schedule.windowEnd(now, zone)
            assertEquals("$now: windowStart disagrees with contains", inside, begun != null)
            assertEquals("$now: windowEnd disagrees with contains", inside, closes != null)
            if (inside) {
                assertTrue("$now: not within [$begun, $closes)", begun!! <= now && now < closes!!)
                starts += begun
            }

            // The night's identity may not move while the night it identifies is still open.
            if (previousEnd != null && now < previousEnd) {
                assertEquals("windowStart drifted inside one night at $now", previousStart, begun)
            }
            previousStart = begun
            previousEnd = closes

            val next = schedule.nextOn(now, zone)
            assertNotNull("nextOn is null at $now", next)
            assertTrue("nextOn ($next) is not strictly after $now", next!! > now)
            assertEquals("nextOn ($next) is not the start of a night", next, schedule.windowStart(next, zone))
            val justBefore = schedule.windowStart(next - 1, zone)
            assertTrue(
                "a night other than now's is open just before nextOn ($next)",
                justBefore == null || justBefore == begun,
            )

            now += 13 * 60_000L
        }
        return starts
    }
}
