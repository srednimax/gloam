package app.gloam.shade

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The refusal that keeps another phone's shade off this one, proven on the JVM.
 *
 * **Why this is a test and not a reading.** Phase 5's R4 could show that the shade *did* come up
 * from a restored intent, because that took a real backup, a real `pm clear` and a real restore. It
 * cannot show the refusal holding in the cases nobody can stage on a phone: a clock that ran ahead
 * on the old device, an intent written the same millisecond the app was installed, or the absent
 * value that every build older than `shade_began_at` leaves behind. Those are comparisons, so they
 * belong here.
 *
 * [startedThisInstall] has no Android imports, the same property `AutoOffTest` relies on.
 */
class RestoredIntentTest {
    /** When this install appeared on this phone. 2026-09-17T16:00:00Z. */
    private val firstInstall = 1_789_660_800_000L

    @Test
    fun `an intent written after this install is honoured`() {
        assertTrue(
            "a shade started a minute after the install is this install's",
            startedThisInstall(firstInstall + 60_000L, firstInstall),
        )
    }

    @Test
    fun `an intent written before this install is refused`() {
        // What a restore looks like: the old phone wrote it days before this install existed.
        assertFalse(
            "a shade started on the previous phone is not this install's",
            startedThisInstall(firstInstall - 3 * 24 * 60 * 60 * 1000L, firstInstall),
        )
    }

    @Test
    fun `an unknown start is refused`() {
        // Every build before `shade_began_at` leaves the key absent, and so does a restore from one.
        // The safe answer is the same for both: do not dim a screen on an undatable value.
        assertFalse("absent is refused", startedThisInstall(null, firstInstall))
    }

    @Test
    fun `the install instant itself counts as this install`() {
        // The boundary is inclusive because the first write can land in the same millisecond on a
        // fast install-then-start, and refusing it would drop a shade the user just asked for.
        assertTrue("the boundary belongs to this install", startedThisInstall(firstInstall, firstInstall))
    }

    @Test
    fun `a clock that ran ahead on the old phone is the one case this cannot see`() {
        // Stated as a test so the limit is recorded rather than discovered: an old phone whose clock
        // was ahead of this one writes an intent that looks like it came from here. Nothing on the
        // device can tell those apart, and the exposure is the skew between two clocks that both
        // follow the network — seconds, against an install that happened before the restore.
        assertTrue(
            "a skewed clock passes, and that is known",
            startedThisInstall(firstInstall + 1, firstInstall),
        )
    }
}
