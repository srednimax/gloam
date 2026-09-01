package app.gloam.shade

import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two flags that keep the shade from trapping the user, asserted where nothing can drift.
 *
 * **This is the cheap half of a two-layer check, and the layers fail differently.** Here the flag
 * set is a compile-time constant, so an edit to [SHADE_WINDOW_FLAGS] reddens this test on the JVM in
 * milliseconds with no device in the loop. What it cannot see is whether the window the service
 * actually adds was built from the constant at all — `ShadeWindowTest` reads that off a running
 * window manager, on a device, where the text it parses is version-shaped and will need attention
 * one day. Neither layer substitutes for the other.
 *
 * **Why these two and not the layout pair.** `FLAG_NOT_TOUCHABLE` is what makes every touch pass
 * through to the app underneath and `FLAG_NOT_FOCUSABLE` leaves key events with that app. Lose
 * either and the shade — a window over everything, at up to `MAX_SHADE_ALPHA` — becomes a phone the
 * user cannot operate, with the escape hatch behind the thing they need to escape. That is the worst
 * thing this app could ship, and it is one deleted line away at all times.
 *
 * Kotlin note: `and` here is Java's `&`, an infix function on `Int` rather than an operator, and
 * `flags and BIT != 0` is the idiom for "this bit is set" — there is no `includes` on a bitmask.
 */
class ShadeWindowFlagsTest {
    @Test
    fun `the shade lets every touch through`() {
        assertTrue(
            "SHADE_WINDOW_FLAGS has lost FLAG_NOT_TOUCHABLE: the shade would swallow touches " +
                "meant for the app underneath it",
            SHADE_WINDOW_FLAGS and FLAG_NOT_TOUCHABLE != 0,
        )
    }

    @Test
    fun `the shade never takes focus`() {
        assertTrue(
            "SHADE_WINDOW_FLAGS has lost FLAG_NOT_FOCUSABLE: key events would be delivered to a " +
                "window the user cannot see",
            SHADE_WINDOW_FLAGS and FLAG_NOT_FOCUSABLE != 0,
        )
    }

    /**
     * The pair is asserted together as well as apart, because the failure that matters is *either*
     * bit going missing and a reader scanning two green tests should see the whole property stated
     * once.
     */
    @Test
    fun `both safety flags are set at once`() {
        val safety = FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE
        assertEquals(
            "SHADE_WINDOW_FLAGS must carry both safety flags",
            safety,
            SHADE_WINDOW_FLAGS and safety,
        )
    }
}
