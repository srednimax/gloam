package app.gloam.shade

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bound that stands in for `FLAG_NOT_TOUCHABLE`.
 *
 * The shade is safe because of a flag, which is a constant; the panel is safe because of its size,
 * which is a computation — a weaker kind of guarantee, and the reason `PLAN.md` rule 3 spends a
 * fourth test here. [panelWidthPx] is a pure function that fails in the direction the app exists to
 * prevent, so it is swept across every plausible display the way `ShadeRampTest` sweeps the ramp,
 * rather than checked at the one width the development phone happens to have.
 *
 * **An assertion that the params say `WRAP_CONTENT` would not have been this test.** `WRAP_CONTENT`
 * is the permissive value: such an assertion passes identically whether the real bound is present or
 * missing, because the bound would then live inside a composition where nothing can see it.
 */
class PanelWidthTest {
    /**
     * 240 px is narrower than any Android display has ever been and 4096 wider than any phone or
     * tablet; the sweep is every integer between, so no rounding step inside the function is skipped.
     */
    private val plausibleDisplays = 240..4096

    @Test
    fun `the panel never covers the display it is over`() {
        for (display in plausibleDisplays) {
            val width = panelWidthPx(display)
            assertTrue(
                "panelWidthPx($display) = $width, which is not strictly narrower than the display. " +
                    "A touchable window that spans the display blocks every touch under it, which " +
                    "is the trap the panel is bounded to avoid.",
                width < display,
            )
        }
    }

    @Test
    fun `the panel is never a sliver`() {
        for (display in plausibleDisplays) {
            val width = panelWidthPx(display)
            // Nine tenths of the display, or the cap on a display wide enough for the cap to bite.
            // Below either, the sliders inside would be too narrow to set a dim level with.
            val floor = minOf(PANEL_MAX_WIDTH_PX, 9 * display / 10)
            assertTrue(
                "panelWidthPx($display) = $width, below the usable floor of $floor",
                width >= floor,
            )
        }
    }

    /**
     * The sentinels are negative and every real width is positive, so this cannot fail while the two
     * above pass — but it is the mistake the function exists to prevent, and it is worth a reader
     * seeing it refused by name.
     */
    @Test
    fun `the panel is never added with a sentinel width`() {
        for (display in plausibleDisplays) {
            val width = panelWidthPx(display)
            assertNotEquals("panelWidthPx($display) returned MATCH_PARENT", MATCH_PARENT, width)
            assertNotEquals("panelWidthPx($display) returned WRAP_CONTENT", WRAP_CONTENT, width)
            assertTrue("panelWidthPx($display) = $width, which is not a width", width > 0)
        }
    }

    /**
     * A display width of zero or less cannot happen — but the answer is still a window that will be
     * added to a window manager, so it is positive and not a sentinel rather than undefined.
     */
    @Test
    fun `a degenerate display still yields a width`() {
        for (display in listOf(Int.MIN_VALUE, -1, 0, 1)) {
            val width = panelWidthPx(display)
            assertTrue("panelWidthPx($display) = $width, which is not a width", width > 0)
        }
    }
}
