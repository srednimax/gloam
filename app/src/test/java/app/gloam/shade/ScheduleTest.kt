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
}
