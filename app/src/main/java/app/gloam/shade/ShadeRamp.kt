package app.gloam.shade

import kotlin.math.pow

/**
 * What the user asked for. Read from DataStore and combined into one value.
 *
 * Kotlin note: a `data class` rather than three parameters on the service, because `copy()` is
 * object spread with the compiler checking the field names — and because the ramp below takes the
 * whole triple, so a caller cannot pass the dim level where the warmth was meant.
 *
 * @param dimLevel 0–100, the one value the product is about (CONTEXT.md: **dim level**).
 * @param warmth 0–100, how far the shade is tinted amber (CONTEXT.md: **warmth**). A separate
 *   control from the dim level, but not an independent one: [shadeValuesFor] scales it by the
 *   headroom the dim level leaves.
 * @param lowerBacklight whether Gloam may take the **backlight** down before it draws the shade.
 */
data class DimSettings(
    val dimLevel: Int,
    val warmth: Int,
    val lowerBacklight: Boolean,
)

/**
 * What the shade's window and its children carry.
 *
 * @param backlight the window brightness override, or **null to release it** and hand the screen
 *   back to the user's own brightness. `Float?` rather than Android's `-1f`
 *   (`BRIGHTNESS_OVERRIDE_NONE`) sentinel: Java had no other way to say *absent*, Kotlin does, and
 *   `null` cannot be accidentally arithmetic'd into a ramp the way `-1f` can. It becomes the
 *   sentinel at the one place that talks to `LayoutParams`.
 * @param shadeAlpha the black child's alpha, `0f … MAX_SHADE_ALPHA`.
 * @param warmthAlpha the amber child's alpha, `0f … MAX_WARMTH_ALPHA`.
 */
data class ShadeValues(
    val backlight: Float?,
    val shadeAlpha: Float,
    val warmthAlpha: Float,
)

/**
 * The darkest the black child is allowed to be, and — through the composite bound below — the
 * darkest the whole shade is allowed to be.
 *
 * 1.0 is a black rectangle with every way out of it behind it: the notification shade, the app, the
 * Stop action. The cap is the difference between a very dark screen and a phone the user believes is
 * broken. A constant rather than a preference on purpose — it is a safety floor, not a taste.
 */
const val MAX_SHADE_ALPHA = 0.95f

/**
 * The lowest window brightness override Gloam will ask for. **Never `0f`**, which is
 * `BRIGHTNESS_OVERRIDE_OFF` — the backlight *off*, over a live touchscreen.
 *
 * **This is a floor on the escape hatch, not on the display driver.** The driver's own floor is a
 * hard lower bound on the search, not the answer to it: what this number has to buy is that the
 * notification shade can be pulled down and **Stop** tapped, in a dark room *and* under normal room
 * light, while the override is live. That matters here and did not matter before the backlight half
 * existed — [MAX_SHADE_ALPHA] never dimmed the escape hatches, because all three sit above
 * `TYPE_APPLICATION_OVERLAY`, whereas the backlight override dims everything including them, on a
 * value the user can no longer change while it is applied.
 *
 * **`0.01f`, and the phone is what set it (R2, R5).** On the development panel that is **6.64 nits**
 * — `nits = 498.3 × override + 1.66`, R1's fit, which reproduced to three decimal places at every
 * level checked. For scale, the *system's own* slider bottoms out at 19 nits on this device, so the
 * floor Gloam stops at is already 2.9× below anything Android will hand the user.
 *
 * **R5 is why it cannot go much lower, and it went the unhelpful way.** The question was whether the
 * system's own surfaces carry their own brightness above ours. They do not: the notification shade,
 * quick settings and the volume dialog all read back our override unchanged, so this one number
 * carries the whole escape-hatch argument rather than sharing it. Verified by eye at maximum dim in
 * a lit room *and* a dark one — the criterion is *with margin*, not *I managed it*.
 *
 * The one surface that does lift it is the **keyguard**, which releases the override outright and
 * comes up at the user's own brightness. That is a free escape hatch this constant does not pay for,
 * and it is a platform behaviour rather than something Gloam arranges: a `TYPE_APPLICATION_OVERLAY`
 * window is hidden behind the lock screen, so it is not there to own the brightness. It returns on
 * its own after unlocking.
 */
const val MIN_BACKLIGHT = 0.01f

/**
 * The most amber the warmth child may ever carry.
 *
 * It is half of the pair that keeps the composite legible — see [shadeValuesFor]'s two invariants —
 * and it is what caps the amber's own luminance at `0.10`, which is why the shade's colour cannot
 * come from `MaterialTheme`.
 */
const val MAX_WARMTH_ALPHA = 0.5f

/**
 * The shade alpha at which warmth starts to yield, so that it has reached zero by
 * [MAX_SHADE_ALPHA].
 *
 * The binding constraint is `(1 - WARMTH_EASE_FROM) × (1 - MAX_WARMTH_ALPHA) ≥ 1 - MAX_SHADE_ALPHA`,
 * which caps this at `0.90`. `0.88f` leaves margin — `0.06` against a `0.05` bound — and puts the
 * onset at dim level ≈ 90 with the backlight toggle on and ≈ 71 with it off.
 *
 * **The ease is defined over the shade alpha rather than over the dim level**, because the invariant
 * it protects is stated over the shade alpha. Easing over the dim level instead would mean re-proving
 * the bound separately for each toggle state and each user's starting brightness.
 */
const val WARMTH_EASE_FROM = 0.88f

/**
 * The amber the warmth child is painted with, ARGB — and **deliberately not a `MaterialTheme`
 * colour**, the one exception to the house rule that every colour comes from the palette.
 *
 * Three things separate it from every other colour in the app: the shade is not a surface but a
 * physical quantity chosen for its effect on light; it must not change when the user switches the
 * app's own light or dark theme; and it is bounded by [relativeLuminance] rather than by the
 * palette's contrast checks.
 *
 * **The bound is what picked it, and taste only chose between the survivors.** Source-over lays
 * veiling light *on top of* the content — `w x amber + (1 - w) x content` — so a bright amber passes
 * the composite's signal bound and still produces a screen nothing can be read through. `#FFB000`
 * has relative luminance `0.523`: at half alpha that is four times more veil than content. This one
 * is `0.073`, so `MAX_WARMTH_ALPHA x 0.073 = 0.036` against a bound of `1 - MAX_SHADE_ALPHA = 0.05`.
 *
 * The brand seed the palette is generated from — `B0763C`, dusk amber — is `0.224` and fails that by
 * more than twice. That is the number behind the exception rather than an assertion of it, and
 * `ShadeRampTest` holds both ends of it.
 */
const val SHADE_AMBER = 0xFF7A3B00.toInt()

/**
 * WCAG relative luminance of an ARGB colour, alpha ignored: how much light the colour itself
 * carries, which is exactly the term the composite's signal bound cannot see.
 *
 * Arithmetic over three floats rather than `ColorUtils.calculateLuminance`, and that is the point —
 * a method call on `android.jar` throws *"not mocked"* in a JVM unit test, so reaching for the
 * platform here would move [SHADE_AMBER]'s bound onto a device. See this file's note on Android
 * imports below.
 */
fun relativeLuminance(color: Int): Float {
    val red = linearise((color shr 16 and 0xFF) / 255f)
    val green = linearise((color shr 8 and 0xFF) / 255f)
    val blue = linearise((color and 0xFF) / 255f)
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

/** sRGB's transfer function, undone: the stored byte is gamma-encoded and light is not. */
private fun linearise(channel: Float): Float =
    if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)

/**
 * One dim level, one ramp, in a fixed order (ADR-0010): spend the **backlight** first, then draw the
 * **shade** over what is left, then tint it.
 *
 * ## The shape, and why there is no breakpoint constant
 *
 * An earlier draft split the slider at a fixed point. The phone says that point is not a property of
 * the device at all — it is user state, and it moves from dim 43 to dim 65 across one phone's own
 * brightness range, with the worst case falling on exactly Gloam's user: someone already at their
 * system minimum, for whom a fixed breakpoint spends most of the slider going almost nowhere.
 *
 * So the ramp is stated once, over the quantity that matters, and the breakpoint falls out of it:
 *
 * ```
 * ratio = backlightTop / MIN_BACKLIGHT      // 1.0 when there is no backlight to spend
 * span  = 1 / (1 - MAX_SHADE_ALPHA)         // 20 — what the shade alone can take
 * light = (ratio * span) ^ -t               // fraction of the top's light still reaching the eye
 * ```
 *
 * **Total light falls at a constant ratio per point from 0 to 100, and the backlight is spent
 * before the shade is touched.** Below the breakpoint `light × ratio ≥ 1`, so the shade alpha is 0
 * and there is no overlay over the user's content at all — the concrete thing ADR-0010 bought by
 * sequencing the two mechanisms rather than blending them. Above it the backlight is pinned at
 * [MIN_BACKLIGHT] and the shade supplies the remainder.
 *
 * Geometric rather than linear because perception of brightness is roughly logarithmic: equal slider
 * travel should buy an equal *ratio* of light, not an equal subtraction of it. Half-way through the
 * shade's stretch a linear ramp still transmits 52% of the content and a geometric one 22%, which is
 * the difference between a slider whose useful range is its last few points and one whose whole
 * length does something.
 *
 * Four things this shape gets for free that a fixed breakpoint had to assert: there is no dead zone
 * for any user, the toggle-off case is the same expression with `ratio = 1` rather than a branch,
 * monotonicity and continuity are true by construction, and the perceived rate never kinks because
 * there is no seam to kink at.
 *
 * ## Geometric in the override float, not in nits — and what that costs
 *
 * The panel's response to the override is **affine, not proportional**: on the development device
 * `nits = 498.3 × override + 1.66`, so halving the override near the bottom of its range does not
 * halve the light. A ramp that is geometric in the float is therefore slightly *flatter* than
 * geometric in what the eye receives, and it flattens where this product lives.
 *
 * **Measured, it is small enough to leave alone.** Over the whole slider the shortfall in perceptual
 * travel is under 4% at either end of the user's own brightness range — the backlight stretch
 * delivers 94% of the ratio it promises at maximum, 80% for a user already at their system minimum,
 * and the shade half is exactly geometric because alpha genuinely multiplies. The correction would
 * cost a fifth constant whose value (`498.3` and `1.66`) is **this panel's and is not readable from
 * an app** — a device-specific number carried for every device, to fix a distortion smaller than the
 * ramp's own first step. That trade is not worth taking, and the reason it is cheap to decline is
 * [MIN_BACKLIGHT]: the divergence only bites in the float's bottom decade, and the escape-hatch floor
 * stops the ramp well above it.
 *
 * ## What it deliberately does not promise
 *
 * **The same dim level is not the same darkness under the two toggle settings, and it is not the
 * same darkness for two users sitting at different brightness.** That is what *"100 means as dark as
 * currently allowed"* already committed to, said out loud rather than hidden.
 *
 * ## The two invariants, which the tests hold it to
 *
 * ```
 * (1 - shadeAlpha) * (1 - warmthAlpha)  ≥  1 - MAX_SHADE_ALPHA
 * MAX_WARMTH_ALPHA * relativeLuminance(amber)  ≤  1 - MAX_SHADE_ALPHA
 * ```
 *
 * The first says the composite may never take more signal than the black child alone was allowed to
 * take: two layers each inside its own cap still multiply, and black at `0.95` under amber at `0.5`
 * leaves 2.5% of the content — half of what was ever allowed. The second says the amber may never
 * *add* more light than the black child was allowed to leave, because source-over lays veiling light
 * on top of the content and the first invariant cannot see that term at all.
 *
 * ## Why this file has no Android imports
 *
 * It is the whole reason the safety values are proven by a JVM test rather than by looking at a
 * screen. Compile-time constants are safe in a JVM unit test; method calls on `android.jar` are not
 * — the stub throws *"not mocked"*. Keeping this file free of Android makes that a non-question.
 *
 * @param backlightTop the backlight Gloam is taking over from, as a fraction of the user's own
 *   maximum — captured at the moment the override is applied and held until it is released, never
 *   re-read while it is live. **Null means the backlight half does nothing this session**, which is
 *   also what a failed or untrustworthy read decays to; see [readBacklight].
 */
fun shadeValuesFor(
    settings: DimSettings,
    backlightTop: Float?,
): ShadeValues {
    val t = settings.dimLevel.coerceIn(0, 100) / 100f

    // A top below our own floor is a user already darker than we would set them, so there is nothing
    // to spend and applying the override could only brighten the screen. Releasing is the darker
    // answer as well as the safe one.
    val top =
        backlightTop
            ?.takeIf { settings.lowerBacklight && it >= MIN_BACKLIGHT && it <= 1f }

    val ratio = if (top == null) 1f else top / MIN_BACKLIGHT
    val span = 1f / (1f - MAX_SHADE_ALPHA)
    val light = (ratio * span).pow(-t)

    // Dim level 0 is the one special case, and it is one line: the override is *released* rather
    // than set to the top. CONTEXT.md already says a dim level of zero is still running; this is
    // what that means physically — the shade is transparent, the backlight is the user's own, and
    // their brightness slider works again. Everything else falls out of the expression above.
    val backlight = if (top == null || settings.dimLevel <= 0) null else maxOf(top * light, MIN_BACKLIGHT)
    val shadeAlpha = (1f - minOf(1f, light * ratio)).coerceIn(0f, MAX_SHADE_ALPHA)

    // Something has to give at the very top, and it cannot be the dim level — that is the one value
    // the product is about. So the applied warmth is the user's warmth scaled by the headroom the
    // dim level leaves. A hard clamp instead would put a visible hue cliff in the last two points,
    // which reads as a bug rather than as a limit.
    val headroom =
        ((MAX_SHADE_ALPHA - shadeAlpha) / (MAX_SHADE_ALPHA - WARMTH_EASE_FROM)).coerceIn(0f, 1f)
    val warmthAlpha = settings.warmth.coerceIn(0, 100) / 100f * MAX_WARMTH_ALPHA * headroom

    return ShadeValues(backlight = backlight, shadeAlpha = shadeAlpha, warmthAlpha = warmthAlpha)
}
