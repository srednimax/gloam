package app.gloam.shade

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.gloam.data.ThemeMode
import app.gloam.theme.AppTheme
import app.gloam.ui.dim.BAR_GROUP_GAP_DP
import app.gloam.ui.dim.BAR_SECTION_WIDTH_DP
import app.gloam.ui.dim.BAR_WIDTH_DP
import app.gloam.ui.dim.CompactControls
import kotlinx.coroutines.flow.StateFlow

/**
 * The flags the **panel**'s window is added with — and the one that is missing is the point.
 *
 * `CLAUDE.md` calls the shade's pair load-bearing: `FLAG_NOT_TOUCHABLE` so every touch passes
 * through, `FLAG_NOT_FOCUSABLE` so key events stay with the app underneath. The panel deliberately
 * drops the first of those, because a control the user cannot touch is a picture of a control. That
 * is the single riskiest line in this phase, and [panelWidthPx] is what bounds it.
 *
 * **These are two properties, not one emphatic one.** *Touchable* is whether touch events land on
 * the window; *focusable* is whether it takes input focus and the key events and IME that come with
 * it. A panel that took focus would swallow the Back key from the app underneath and could raise a
 * keyboard for a field it does not have — so `FLAG_NOT_FOCUSABLE` stays exactly as the shade has it.
 *
 * **`FLAG_LAYOUT_NO_LIMITS` is absent, unlike the shade's.** The shade needs to extend past the
 * system bars because a bright strip across the top of an otherwise dimmed screen reads as a bug.
 * The panel is sized to its own content and has no business outside the display's bounds.
 *
 * `const` for the same reason [SHADE_WINDOW_FLAGS] is: a `const val` is inlined into its callers at
 * compile time, so `PanelWindowFlagsTest` reads a number on the JVM instead of calling into
 * `android.jar`, where an unmocked method throws.
 */
const val PANEL_WINDOW_FLAGS =
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

/**
 * How long the panel survives without being touched, before the service takes it down.
 *
 * A constant rather than a preference, on `CLAUDE.md`'s line between the two: this is a safety floor,
 * not a taste. Thirty seconds is long enough to move two sliders and look at the result, and short
 * enough that a panel drawn off-screen by a layout bug is gone before the user concludes the phone
 * is broken.
 */
const val PANEL_IDLE_TIMEOUT_MS = 30_000L

/**
 * The most of the display the panel may ever cover, whatever it is showing.
 *
 * **Taste is not what this bounds.** A touchable window blocks every touch inside it, so the panel's
 * width is the thing standing in for the shade's `FLAG_NOT_TOUCHABLE` (ADR-0011): the app underneath
 * stops answering a finger anywhere the window reaches. A fraction rather than a pixel count, so
 * "there is always more display beside the panel than under it" holds at every display size.
 */
private const val PANEL_MAX_DISPLAY_FRACTION = 0.7f

/**
 * How far in from the display's edges the panel floats. The bar is anchored to a bottom corner —
 * where a thumb already is — rather than centred over what the user is reading.
 */
internal const val PANEL_SIDE_MARGIN_DP = 16

/**
 * How far above the navigation bar the panel floats.
 *
 * Small: the panel is bottom-anchored so the controls land under the thumb, and pushing it further
 * up only moves it over more of what the user is reading. **The navigation bar is not in this
 * number and must not be added to it** — the panel carries no `FLAG_LAYOUT_NO_LIMITS`, so the window
 * manager lays it out inside a display frame that already stops above the bar, whichever navigation
 * mode the phone is in. R6 read that off the phone after a first attempt added the inset by hand and
 * floated the panel five times too high.
 */
internal const val PANEL_BOTTOM_MARGIN_DP = 24

/**
 * How wide to add the panel's window: the bar, plus the room a disclosure needs when one is open.
 *
 * **This is the redesign's biggest safety change and it reads like a layout detail.** The old panel
 * was a sheet 90% of the display wide, so while it was up almost nothing underneath it answered a
 * touch. The edge bar is 84dp, which is what the window is added at while nothing is open; opening
 * the warmth column or the timer chips widens it, and closing them narrows it again
 * (`ShadeService.onPanelSectionOpen`).
 *
 * Kept pure, with no `Context` and no `Density` object, so `PanelWidthTest` can sweep it the way
 * `ShadeRampTest` sweeps the ramp: every plausible display, every density, both states, in one JVM
 * test. The gap it adds is [BAR_GROUP_GAP_DP], which must stay in step with the `Spacing` step the
 * composition actually lays the group out with — they are two views of the same 8dp and only this
 * one is testable.
 *
 * The fraction wins over the content when the two disagree, which on a narrow display means an open
 * section is squeezed rather than the window growing over the app: content that does not fit wraps
 * (`BAR_SECTION_WIDTH_DP`), and a wrapped chip is a far better failure than a phone that has stopped
 * responding down one side.
 */
fun panelWidthPx(
    displayWidthPx: Int,
    density: Float,
    sectionOpen: Boolean,
): Int {
    val contentDp = BAR_WIDTH_DP + if (sectionOpen) BAR_GROUP_GAP_DP + BAR_SECTION_WIDTH_DP else 0f
    val wanted = (contentDp * density.coerceAtLeast(0.1f)).toInt()
    val ceiling = (displayWidthPx * PANEL_MAX_DISPLAY_FRACTION).toInt()
    return wanted.coerceAtMost(ceiling).coerceAtLeast(1)
}

/**
 * The three owners an Activity would have provided, built by hand for a window that has none.
 *
 * ## What this is for
 *
 * A `ComposeView` walks up the view tree looking for a `LifecycleOwner`, a `SavedStateRegistryOwner`
 * and — only if something calls `viewModel()` — a `ViewModelStoreOwner`. In a window added straight
 * to the `WindowManager` there is no Activity above it, so nothing has set them. **An Activity is
 * those owners wearing a screen**, and this class is the two of them the panel actually needs.
 *
 * There is no `ViewModelStoreOwner` here, and that falls out of the panel's design rather than being
 * a saving: `DimControls()` takes state and callbacks as parameters, so nothing in the panel calls
 * `viewModel()`. Had it read its own state, this class would be three owners and a `ViewModelStore`
 * whose clearing nobody owns.
 *
 * ## The two ways this fails, neither of which looks like what it is
 *
 * - **`performRestore(null)` must run before the lifecycle passes `CREATED`.** There is nothing to
 *   restore — the panel has no saved state — but the registry insists on being told so, and the
 *   exception it throws otherwise names neither this class nor the reason.
 * - **`RESUMED` is not a formality.** The `Recomposer` Compose installs for a window is tied to this
 *   lifecycle and stops applying recompositions below `STARTED`. A host left at `CREATED` draws its
 *   first frame correctly and then never changes again: the slider will not move under your finger,
 *   and nothing in logcat says why. It is the most likely way the panel goes wrong.
 *
 * ## One host per summon
 *
 * **`DESTROYED` is terminal, not "hidden".** `LifecycleRegistry` has no upward event out of it and
 * throws when asked to move up, and `SavedStateRegistryController.performRestore` runs once per
 * controller — so the `init` block cannot be re-run either. A `show()` / `hide()` pair on one
 * long-lived host therefore works exactly once, and the panel has three ways to close and one to
 * reopen, which makes the *second* summon the ordinary case. `ShadeService` builds one of these when
 * it adds the window and drops the reference when it removes it.
 *
 * **Kotlin note for a JS reader:** the JS instinct — an object with `show()` and `hide()` you call as
 * often as you like — is exactly what does not survive here. A lifecycle host is a one-shot object,
 * closer to a `Promise` than to an event emitter: it moves one way, and past the end there is no
 * going back.
 *
 * @param onTouch called for **every** touch the panel receives, including the ones a child consumes.
 *   `dispatchTouchEvent` is the only place that sees all of them — a Compose `pointerInput` would
 *   see a consumed touch only on `PointerEventPass.Initial`. It is what re-arms the idle timeout.
 */
internal class PanelHost(
    context: Context,
    onTouch: () -> Unit,
) : LifecycleOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val composeView = ComposeView(context)

    /**
     * The root handed to the window manager: the wrapper, not the `ComposeView`.
     *
     * **A wrapper because `ComposeView` is `final`** — there is no subclass to override
     * `dispatchTouchEvent` on, so the interception happens one level up, where a parent sees every
     * touch on its way down to the children regardless of who consumes it.
     */
    val view: View =
        TouchReportingLayout(context, onTouch).apply {
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        // Set on the root rather than on the `ComposeView`: the lookup walks *up* the tree, so the
        // root is the one place that answers for every view added under it later.
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** What the window draws. Set once, before the window is added. */
    fun setContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }

    /** Move to `RESUMED`, which is what starts recomposition. Called once the window is added. */
    fun show() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Terminal — the next summon builds a new host. */
    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

/**
 * A `FrameLayout` whose only job is to report touches it is about to hand down.
 *
 * `dispatchTouchEvent` rather than `onTouchEvent` or an `OnTouchListener`, because only dispatch
 * sees the touches a child consumes — and a finger on the dim slider is exactly the touch that must
 * re-arm the idle timeout. It reports and then does nothing else: `super` still delivers the event,
 * so this layout changes no behaviour it observes.
 */
private class TouchReportingLayout(
    context: Context,
    private val onTouch: () -> Unit,
) : FrameLayout(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouch()
        return super.dispatchTouchEvent(ev)
    }
}

/**
 * Everything the panel draws, as one immutable value.
 *
 * **Hoisted into `ShadeService` rather than collected in the composition**, because the service is
 * already collecting most of it for the shade and because a `Flow` collected inside a composition
 * arrives *after* the first frame — which here would be a slider visibly jumping from its default to
 * the user's real dim level the instant the panel appears. The service reads a snapshot before it
 * adds the window, so the first frame is already right, and keeps this updated afterwards so a
 * change made anywhere else still moves the panel.
 *
 * **Kotlin note for a JS reader:** `copy()` is object spread — `state.copy(warmth = 40)` is
 * `{...state, warmth: 40}`. It is how each of the service's little collectors writes its one field
 * back without any of them needing to know about the others.
 *
 * **It carries less than it did, because the edge bar draws less.** The schedule summary, the
 * battery-exemption warning and the deadline read-out are gone from this surface: what is left is
 * the level, the warmth, run/stop and the auto-off chips. Everything dropped is a *read* rather than
 * a control, and the pips at the top of the bar are one tap from the screen that has all of them.
 * A window that must stay legible at 6.64 nits, block as little of the app underneath as it can and
 * be dismissible by somebody who cannot see the rest of the screen cannot also be a dashboard.
 */
data class PanelState(
    val dimLevel: Int,
    val warmth: Int,
    val running: Boolean,
    val autoOff: AutoOff,
    val themeMode: ThemeMode,
    val materialYou: Boolean,
)

/**
 * What the panel's window draws: the same controls the app has, over whatever the user is reading.
 *
 * **The one control surface in Gloam that stays legible at maximum dim.** It is a second
 * `TYPE_APPLICATION_OVERLAY` window, so it is drawn *above* the shade rather than under it, at the
 * full backlight the shade left the panel at — where every Activity-hosted control of ours sits
 * beneath the shade's own alpha and cannot escape it by any flag, theme or `LayoutParams` field.
 *
 * `AppTheme` needs no Activity — it is a function over `MaterialTheme`, and the one piece of it that
 * does want a window checks for it and returns. The palette therefore comes out of the same generated
 * scheme the app uses, from values read out of `AppPreferences` rather than from the configuration,
 * so the panel cannot disagree with the screen the user just left.
 *
 * The window is translucent and sized to this content, so what the user sees is the bar itself
 * floating over their own app rather than a panel welded to the bottom of the display.
 */
@Composable
internal fun PanelContent(
    state: StateFlow<PanelState>,
    onDimLevel: (Int) -> Unit,
    onWarmth: (Int) -> Unit,
    onAutoOff: (AutoOff) -> Unit,
    onToggleRunning: () -> Unit,
    onOpenApp: () -> Unit,
    onSectionOpen: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    // Collected here rather than passed as a value, so that a preference written from anywhere —
    // the full app under the shade, the compact controls, this panel's own sliders — moves it. The
    // lifecycle this reads comes from `PanelHost`, which is the whole reason that class exists.
    val current by state.collectAsStateWithLifecycle()

    AppTheme(themeMode = current.themeMode, dynamicColor = current.materialYou) {
        // **No `Surface` around it any more.** The old panel was a card with controls on it, so it
        // needed one; the edge bar paints its own shape on a translucent window, and a surface
        // behind it would be a rectangle of theme colour standing between the bar and the app the
        // user is actually reading.
        //
        // **`onClose` is not optional here, and it is the reason the parameter is nullable at all.**
        // With `FLAG_NOT_FOCUSABLE` the Back key never reaches this window, so there is no system
        // gesture that closes it: this button and the service's idle timeout are the only two ways
        // out that do not also take the shade down. The compact host passes nothing, because an
        // Activity already has a back gesture.
        CompactControls(
            dimLevel = current.dimLevel,
            warmth = current.warmth,
            running = current.running,
            autoOff = current.autoOff,
            onDimLevel = onDimLevel,
            onWarmth = onWarmth,
            onAutoOff = onAutoOff,
            onToggleRunning = onToggleRunning,
            onOpenApp = onOpenApp,
            onSectionOpen = onSectionOpen,
            onClose = onClose,
        )
    }
}
