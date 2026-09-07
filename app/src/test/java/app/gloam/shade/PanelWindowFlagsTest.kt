package app.gloam.shade

import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
import android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mirror of `ShadeWindowFlagsTest`, and **it asserts a difference rather than a property**.
 *
 * The shade's test says the two safety flags are both present. This one says the panel has exactly
 * one of them, because the panel exists to catch touches and a `FLAG_NOT_TOUCHABLE` on it would be a
 * silent reversion to a picture of a control — a window that draws sliders nobody can move, with no
 * crash and nothing in logcat.
 *
 * The flag it must *keep* matters just as much and for the opposite reason: with focus, the panel
 * would take the Back key away from the app underneath it.
 *
 * What no constant can say is what happens to touches that land *beside* the panel, which is the
 * half the size bound in `PanelWidthTest` covers and R7 reads on a device.
 */
class PanelWindowFlagsTest {
    @Test
    fun `the panel catches touches`() {
        assertTrue(
            "PANEL_WINDOW_FLAGS has gained FLAG_NOT_TOUCHABLE: the panel would draw controls that " +
                "cannot be touched, and every touch would fall through to the app underneath",
            PANEL_WINDOW_FLAGS and FLAG_NOT_TOUCHABLE == 0,
        )
    }

    @Test
    fun `the panel never takes focus`() {
        assertTrue(
            "PANEL_WINDOW_FLAGS has lost FLAG_NOT_FOCUSABLE: the panel would take the Back key " +
                "and the IME away from the app the user is actually in",
            PANEL_WINDOW_FLAGS and FLAG_NOT_FOCUSABLE != 0,
        )
    }

    /**
     * Dismissal, and the reason this is a *flag* test rather than a behaviour one.
     *
     * `TouchReportingLayout` turns `ACTION_OUTSIDE` into a dismissal, but the window manager only
     * ever sends that event to a window that asked for it. Drop this flag and the code that handles
     * it still compiles, still reads correctly and is simply never called — the panel would keep its
     * close button and lose the tap-anywhere-else that a floating control is expected to have, with
     * nothing failing anywhere. So the flag is asserted where it cannot be quietly lost.
     */
    @Test
    fun `a tap outside the panel reaches the panel`() {
        assertTrue(
            "PANEL_WINDOW_FLAGS has lost FLAG_WATCH_OUTSIDE_TOUCH: ACTION_OUTSIDE would never be " +
                "delivered, and tapping beside the panel would no longer put it away",
            PANEL_WINDOW_FLAGS and FLAG_WATCH_OUTSIDE_TOUCH != 0,
        )
        assertTrue(
            "FLAG_WATCH_OUTSIDE_TOUCH only reports a touch it does not take: the panel must still " +
                "refuse focus, or the outside tap would stop here instead of reaching the app below",
            PANEL_WINDOW_FLAGS and FLAG_NOT_FOCUSABLE != 0,
        )
    }

    /**
     * The two windows differ in exactly one bit of the safety pair, stated once so a reader sees the
     * whole relationship rather than two halves of it.
     */
    @Test
    fun `the panel differs from the shade in touchability alone`() {
        val safety = FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE
        assertEquals(
            "The shade must refuse both; the panel must refuse focus only",
            safety,
            SHADE_WINDOW_FLAGS and safety,
        )
        assertEquals(
            "PANEL_WINDOW_FLAGS must carry FLAG_NOT_FOCUSABLE and nothing else from the safety pair",
            FLAG_NOT_FOCUSABLE,
            PANEL_WINDOW_FLAGS and safety,
        )
    }
}
