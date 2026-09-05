package app.gloam.shade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The auto-off deadline, proven on the JVM because nothing on a device can tell you it is right.
 *
 * `phase-2.md` §10 calls these derivations rather than readings: a phone can show you that the shade
 * came down, and it cannot show you that it came down at the instant the user was promised. The
 * wrong answer here is a shade that never lifts, or one that vanishes the moment it goes up, and
 * both are the failure this app exists to prevent rather than a bug somebody would report.
 *
 * `AutoOff.kt` has no Android imports, which is what makes a plain JVM test possible — the same
 * property `ShadeRamp.kt` has, and for the same reason.
 */
class AutoOffTest {
    /**
     * A real-shaped instant rather than `0`, and that is the point of choosing one.
     *
     * `0` is [NO_DEADLINE], so a test that starts its clock there cannot see the sentinel trap at
     * all: every arithmetic result would sit near the epoch and *absent* would be indistinguishable
     * from *a deadline in 1970*. This is 2026-09-01T20:00:00Z.
     */
    private val now = 1_756_756_800_000L

    @Test
    fun `every choice but Never lands its own number of minutes ahead`() {
        for (choice in AutoOff.entries) {
            val deadline = autoOffDeadline(now, choice)
            if (choice == AutoOff.Never) {
                assertNull("Never has no deadline", deadline)
            } else {
                assertEquals(
                    "$choice is ${choice.minutes} minutes after the start",
                    now + choice.minutes * 60_000L,
                    deadline,
                )
            }
        }
    }

    /** The one case with no deadline is the one case the enum names, not a duration of zero. */
    @Test
    fun `Never is the only choice without a deadline`() {
        assertEquals(listOf(AutoOff.Never), AutoOff.entries.filter { autoOffDeadline(now, it) == null })
    }

    /** The span the product promises: half an hour to four hours, and nothing longer by accident. */
    @Test
    fun `the longest choice is four hours and the default sits inside the span`() {
        assertEquals(240, AutoOff.entries.maxOf { it.minutes })
        assertEquals(AutoOff.Hours2, AutoOff.Default)
        assertTrue(AutoOff.Default in AutoOff.entries)
    }

    @Test
    fun `a deadline is due at the instant and after it, not before`() {
        val deadline = autoOffDeadline(now, AutoOff.Hours2)
        assertFalse("a millisecond early is not due", isDue(deadline!! - 1, deadline))
        assertTrue("exactly the deadline is due", isDue(deadline, deadline))
        assertTrue("a millisecond late is due", isDue(deadline + 1, deadline))
    }

    /**
     * The case the resume reconcile exists for: nothing was alive to fire it, hours went by, and the
     * screen has to read *that already happened* rather than *there is no deadline*.
     */
    @Test
    fun `a deadline long past is still due`() {
        assertTrue(isDue(now, now - 9 * 60 * 60_000L))
    }

    @Test
    fun `no deadline is never due, however late it gets`() {
        assertFalse(isDue(now, null))
        assertFalse(isDue(Long.MAX_VALUE, null))
    }

    /**
     * **The sentinel, in the direction that would fail silently.** A stored `0` read as an instant
     * is 1970, 1970 is in the past, and a past deadline is due — so the shade would come down the
     * moment it went up, on every start, for every user who chose `Never`.
     */
    @Test
    fun `the stored zero is absent rather than 1970`() {
        assertNull(deadlineOrNull(NO_DEADLINE))
        assertFalse("a stored zero must not read as due", isDue(now, deadlineOrNull(NO_DEADLINE)))
    }

    @Test
    fun `a stored instant survives the round trip`() {
        val deadline = autoOffDeadline(now, AutoOff.Minutes30)!!
        assertEquals(deadline, deadlineOrNull(deadline))
    }

    @Test
    fun `every choice round-trips through the minutes storage holds`() {
        for (choice in AutoOff.entries) {
            assertEquals(choice, AutoOff.ofMinutes(choice.minutes))
        }
    }

    /**
     * A number this build does not know is a later build's value — a *3 hours* that was added after
     * this one shipped — and the answer is the default rather than an exception. A preference file
     * survives a downgrade; a crash loop at startup is the worse outcome.
     */
    @Test
    fun `an unrecognised number falls back to the default`() {
        assertEquals(AutoOff.Default, AutoOff.ofMinutes(180))
        assertEquals(AutoOff.Default, AutoOff.ofMinutes(-1))
    }
}
