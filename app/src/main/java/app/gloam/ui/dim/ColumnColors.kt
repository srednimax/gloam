package app.gloam.ui.dim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import app.gloam.theme.LocalDarkTheme

/**
 * The colours the column control is *made of* — and the one documented exception to the house rule
 * that colours come from `MaterialTheme`.
 *
 * ## Why these are literals
 *
 * The column is not chrome. It is a **picture of the mechanism**: a plate standing for the light the
 * screen is putting out, and a shade drawn down over it. Two consequences follow, and neither is
 * taste:
 *
 * - **The plate has to read as light.** In the dark theme it is the brightest thing on the screen,
 *   because that is what it depicts; there is no `MaterialTheme` role that means "the light being
 *   covered", and the nearest ones (`primaryContainer`, `inversePrimary`) mean something else and
 *   move when the palette does.
 * - **It must survive dynamic colour.** With Material You on (ADR-0006 makes it opt-in, so this is
 *   the user's choice rather than the default) every role in the scheme is replaced by wallpaper
 *   colours. A plate painted from a role would then be blue or green — a picture of light that is
 *   not the colour of light — while the shade this app actually draws stays amber. `shade/` already
 *   holds its amber as a constant for a related reason (ADR-0010, the second luminance bound); this
 *   is the same argument one layer up.
 *
 * **What is *not* here is as deliberate.** Everything *around* the column — buttons, chips, the
 * switch, every piece of text, the warmth ramp's warm end — takes `MaterialTheme` roles as usual, so
 * the palette still decides everything a palette should. What is fixed here is the picture: the
 * light, the shade over it, the line between them, and the ink that has to be legible on each.
 *
 * Kotlin note: `@Immutable` is a promise to Compose that no field of this ever changes value after
 * construction. It is not `readonly` — the compiler does not check it — it is an instruction to the
 * runtime that instances can be compared by reference when deciding whether to recompose. Lying
 * about it produces a UI that stops updating, which is why it is only ever put on a data class of
 * `val`s like this one.
 */
@Immutable
data class ColumnColors(
    /** The uncovered light, top to bottom. */
    val plateTop: Color,
    val plateBottom: Color,
    /** A hairline around the plate, so it reads as an object rather than a hole. */
    val plateOutline: Color,
    /** The shade over it, top to bottom. */
    val coveredTop: Color,
    val coveredBottom: Color,
    /**
     * The shade edge, and the handle centred on it.
     *
     * It is here rather than taken from `primary` for the same reason the plate is: **what it has to
     * contrast with is the plate, not the app's surface.** The generated scheme's dark `primary` is
     * a pale amber that sits within a few tones of the plate's own top, so an edge painted from it
     * is a line you have to look for — and this is the one line in the control the design calls out
     * as having to stay crisp, because it is the shade's own boundary.
     */
    val edge: Color,
    /** Ink that sits on the bare plate: "FLOOR", the foot band's glyph, a shallow percentage. */
    val plateInk: Color,
    /** Ink that sits on the covered area. */
    val shadeInk: Color,
    /** The run/stop band at the foot of the compact bar, and its top border. */
    val foot: Color,
    val footBorder: Color,
    /** The warmth ramp's cool end and its middle; the warm end is the theme's `primary`. */
    val warmthCool: Color,
    val warmthMid: Color,
)

/**
 * The dark set (design 2a / 4a). The plate is the light; the shade is nearly black over it.
 */
private val DarkColumnColors =
    ColumnColors(
        plateTop = Color(0xFFF7D9AE),
        plateBottom = Color(0xFFF0BE81),
        plateOutline = Color(0x59F5A85E),
        coveredTop = Color(0xFF0A0906),
        coveredBottom = Color(0xFF1B1509),
        edge = Color(0xFFF5A85E),
        plateInk = Color(0xFF5C3403),
        shadeInk = Color(0xFFF7F1E8),
        foot = Color(0xFFF2C489),
        footBorder = Color(0x4D5C3403),
        warmthCool = Color(0xFF3D3529),
        warmthMid = Color(0xFF8A5A1E),
    )

/**
 * The light set (design 6a / 6b), and it is an **inversion rather than a tint**: the plate becomes
 * paper and the covered area becomes the dark thing on it, because on a white ground the shade is
 * what has to be visible. Three of these are forced by the pale ground rather than chosen —
 * the plate's hairline goes opaque (a translucent glow is invisible on paper), the warmth ramp runs
 * cool-to-warm so it ends at `primary` instead of starting near it, and the foot band is one step
 * darker than the plate's own bottom so a 72dp band reads without a heavier border.
 */
private val LightColumnColors =
    ColumnColors(
        plateTop = Color(0xFFFFFFFF),
        plateBottom = Color(0xFFFFEAD1),
        plateOutline = Color(0xFFC9B79C),
        coveredTop = Color(0xFF332512),
        coveredBottom = Color(0xFF54391A),
        edge = Color(0xFF8A5216),
        plateInk = Color(0xFF54300A),
        shadeInk = Color(0xFFFFF3E4),
        foot = Color(0xFFFFCE96),
        footBorder = Color(0x525C3403),
        warmthCool = Color(0xFFEFE7DC),
        warmthMid = Color(0xFFD8A45E),
    )

/**
 * The set for whichever scheme is showing.
 *
 * Reads [LocalDarkTheme] rather than `isSystemInDarkTheme()`: the app's theme mode is a preference
 * that can disagree with the phone (ADR-0006), and the panel's window has no configuration of its
 * own to ask anyway.
 *
 * Kotlin note: `@ReadOnlyComposable` says this reads composition state but writes none — no
 * `remember`, no side effect — which lets the compiler skip the bookkeeping it would otherwise
 * generate around the call. It is only correct because the whole body is one local read.
 */
@Composable
@ReadOnlyComposable
fun columnColors(): ColumnColors = if (LocalDarkTheme.current) DarkColumnColors else LightColumnColors
