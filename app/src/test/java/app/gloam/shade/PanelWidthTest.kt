package app.gloam.shade

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import app.gloam.ui.dim.BAR_WIDTH_DP
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
 *
 * ## What the redesign changed here
 *
 * The panel used to be a sheet 90% of the display wide, and the old version of this file asserted a
 * *floor* — that it was never a sliver — because a sheet of sliders narrower than that was unusable.
 * The edge bar inverts that: it is 84dp of bar in a corner, and the thing worth asserting is that it
 * stays small. The floor that is left is the bar itself, because a window narrower than what it
 * draws would clip the control rather than shrink it.
 */
class PanelWidthTest {
    /**
     * 240 px is narrower than any Android display has ever been and 4096 wider than any phone or
     * tablet; the sweep is every integer between, so no rounding step inside the function is skipped.
     */
    private val plausibleDisplays = 240..4096

    /** Every density Android defines a bucket for, plus the fractional ones real phones report. */
    private val plausibleDensities = listOf(0.75f, 1f, 1.5f, 2f, 2.625f, 2.75f, 3f, 3.5f, 4f)

    private fun sweep(body: (display: Int, density: Float, sectionOpen: Boolean, width: Int) -> Unit) {
        for (display in plausibleDisplays) {
            for (density in plausibleDensities) {
                for (open in listOf(false, true)) {
                    body(display, density, open, panelWidthPx(display, density, open))
                }
            }
        }
    }

    @Test
    fun `the panel never covers most of the display it is over`() {
        sweep { display, density, open, width ->
            assertTrue(
                "panelWidthPx($display, $density, open=$open) = $width, which is more than seven " +
                    "tenths of the display. A touchable window blocks every touch under it, so the " +
                    "app underneath has to keep most of its own screen.",
                width <= (display * 0.7f).toInt(),
            )
        }
    }

    @Test
    fun `the panel is always strictly narrower than the display`() {
        sweep { display, density, open, width ->
            assertTrue(
                "panelWidthPx($display, $density, open=$open) = $width, which is not strictly " +
                    "narrower than the display",
                width < display,
            )
        }
    }

    /**
     * The bar is 84dp and it is the whole control: a window narrower than that clips the foot band's
     * glyph and the pips, which are the two ways out of this surface.
     *
     * The exception is a display so narrow that seven tenths of it is less than the bar — no phone
     * is, but the ceiling has to win there, because a window wider than the display is the failure
     * this whole file exists to prevent.
     */
    @Test
    fun `the panel is never narrower than the bar it draws`() {
        sweep { display, density, open, width ->
            val bar = (BAR_WIDTH_DP * density).toInt()
            val ceiling = (display * 0.7f).toInt()
            if (bar <= ceiling) {
                assertTrue(
                    "panelWidthPx($display, $density, open=$open) = $width, narrower than the " +
                        "${bar}px bar it has to draw",
                    width >= bar,
                )
            }
        }
    }

    /** An open disclosure needs room to its left; a closed one must give it back. */
    @Test
    fun `an open section is never narrower than a closed one`() {
        for (display in plausibleDisplays) {
            for (density in plausibleDensities) {
                assertTrue(
                    "An open section at $display px / $density asked for no more room than a " +
                        "closed one, so opening the warmth column would clip it",
                    panelWidthPx(display, density, true) >= panelWidthPx(display, density, false),
                )
            }
        }
    }

    /**
     * The sentinels are negative and every real width is positive, so this cannot fail while the
     * others pass — but it is the mistake the function exists to prevent, and it is worth a reader
     * seeing it refused by name.
     */
    @Test
    fun `the panel is never added with a sentinel width`() {
        sweep { display, density, open, width ->
            assertNotEquals("panelWidthPx($display, $density, open=$open) returned MATCH_PARENT", MATCH_PARENT, width)
            assertNotEquals("panelWidthPx($display, $density, open=$open) returned WRAP_CONTENT", WRAP_CONTENT, width)
            assertTrue("panelWidthPx($display, $density, open=$open) = $width, which is not a width", width > 0)
        }
    }

    /**
     * A display width of zero or less cannot happen — but the answer is still a window that will be
     * added to a window manager, so it is positive and not a sentinel rather than undefined. The
     * same for a density the platform should never report.
     */
    @Test
    fun `a degenerate display or density still yields a width`() {
        for (display in listOf(Int.MIN_VALUE, -1, 0, 1)) {
            for (density in listOf(-1f, 0f, 1f)) {
                for (open in listOf(false, true)) {
                    val width = panelWidthPx(display, density, open)
                    assertTrue(
                        "panelWidthPx($display, $density, open=$open) = $width, which is not a width",
                        width > 0,
                    )
                }
            }
        }
    }
}
