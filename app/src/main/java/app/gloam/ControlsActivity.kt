package app.gloam

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gloam.shade.PANEL_BOTTOM_MARGIN_DP
import app.gloam.shade.PANEL_SIDE_MARGIN_DP
import app.gloam.shade.ShadeEnd
import app.gloam.shade.canDrawShade
import app.gloam.shade.escapeHatchLive
import app.gloam.shade.startShade
import app.gloam.shade.stopShade
import app.gloam.theme.AppTheme
import app.gloam.ui.dim.CompactControls
import app.gloam.ui.dim.DimViewModel

/**
 * **The compact controls: the same sliders in a floating window, for the shade that is already up.**
 *
 * This is the surface the notification and (from Phase 2b) the tile reach — the two doors that can
 * be opened *while the shade is on screen*, without going back to the launcher and without the full
 * app's first-run framing. `docs/phase-3.md` §2 is the argument for it; what follows is only the
 * part that is not obvious from reading the code.
 *
 * ## It is a dialog because a theme said so, before any of this ran
 *
 * `Theme.App.Controls` is what makes this window float, and the platform reads it out of the
 * manifest when the window is created — earlier than `onCreate`, earlier than any code of ours.
 * There is no JS analogue: nothing in a browser decides your document's shape from a declaration it
 * read before your script existed. That is also why the compact host is a *second activity* rather
 * than a flag on [MainActivity]: one activity has one manifest theme, and the starting window is
 * drawn from it either way.
 *
 * `AppCompatActivity` for the same single reason [MainActivity] is one — `AppCompatDelegate` is what
 * applies the in-app language (ADR-0004) and moves the night mode (ADR-0006), and it throws on a
 * theme that does not descend from `Theme.AppCompat`.
 *
 * ## What this host deliberately does not have
 *
 * No overlay explainer, no notification warning banner, no permission hand-off, no top bar. Every
 * one of those is first-run framing, and a dialog summoned over somebody else's app in the dark is
 * the wrong place to meet Gloam for the first time. So when either precondition is missing this host
 * does not draw a degraded version of itself — it *becomes* the full app, which is the guard below.
 *
 * **Nor does it have the settings**, which is [CompactControls]' rule rather than this class's: the
 * backlight switch and everything after it belong to the full app, and the cog is how they are
 * reached. This host no longer reads `readBacklightTop` at all, because nothing it draws asks.
 *
 * **It is not an escape hatch** (`docs/phase-2.md` §2): reaching it still needs sight of Gloam's own
 * UI, under the shade. `EscapeHatch.kt`'s inventory does not gain a row for it.
 */
class ControlsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before anything is composed: a dialog with a broken start button on it is worse than no
        // dialog. `finish()` from `onCreate` skips straight to `onDestroy` without composing.
        if (forwardIfUnusable()) return

        val app = application as MainApplication

        // **The same corner the panel uses, for the same reason.** These two surfaces are one thing
        // to the user — the notification opens whichever the shade's state allows — so the bar has
        // to be in the same place in both. A floating window's `x`/`y` are offsets from the edges
        // its gravity names, not absolute coordinates.
        val density = resources.displayMetrics.density
        window.setGravity(Gravity.BOTTOM or Gravity.END)
        window.attributes =
            window.attributes.apply {
                x = (PANEL_SIDE_MARGIN_DP * density).toInt()
                y = (PANEL_BOTTOM_MARGIN_DP * density).toInt()
            }

        setContent {
            val materialYou by app.preferences.materialYou.collectAsStateWithLifecycle(initialValue = false)
            val themeMode by
                app.preferences.themeMode.collectAsStateWithLifecycle(initialValue = app.startupThemeMode)

            AppTheme(themeMode = themeMode, dynamicColor = materialYou) {
                // **No `Surface` behind it.** The edge bar paints its own shape on a window
                // `Theme.App.Controls` already made transparent; a surface here would be a
                // rectangle of theme colour around a control designed to float. `fillMaxSize` is
                // safe for the same reason it was not before: the window is floating and wrapped to
                // the size the theme's minimum-width items allow, and what fills it is a
                // transparent box that aligns the bar into its bottom corner.
                ControlsBody(
                    onStart = ::startShade,
                    onStop = ::stopShade,
                    onOpenApp = ::openFullApp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    /**
     * The second half of the guard, on the rule `DimScreen` already follows: both preconditions are
     * switches on settings screens, so both can move while this window is in the background.
     *
     * `isFinishing` because [forwardIfUnusable] may already have fired from `onCreate` — starting
     * the full app twice is a second task animation for one tap.
     */
    override fun onResume() {
        super.onResume()
        if (!isFinishing) forwardIfUnusable()
    }

    /**
     * **Two live reads, and neither may be cached.** `SYSTEM_ALERT_WINDOW` decides whether the start
     * button can do anything at all; `escapeHatchLive()` decides whether the shade this window
     * starts would have a way out that is not this window. The full app has copy for both cases —
     * the explainer and the warning banner — and this one has none, so being the full app instead is
     * the honest response rather than a fallback.
     *
     * `escapeHatchLive()` gets the second caller `docs/phase-2.md` §2 predicted, from 3a rather than
     * from 2b, with the predicate unchanged — which is the whole point of having written it as a
     * function in `shade/` instead of inline in the screen that first needed it.
     */
    private fun forwardIfUnusable(): Boolean {
        if (canDrawShade() && escapeHatchLive()) return false
        openFullApp()
        return true
    }

    /**
     * The way out to the full app, used by the guard and by the cog.
     *
     * **`FLAG_ACTIVITY_NEW_TASK` is load-bearing rather than boilerplate.** This activity declares an
     * empty `taskAffinity` and `excludeFromRecents`, so without the flag [MainActivity] would be
     * pushed onto *this* task — the full app, running inside a task the user cannot find in recents.
     * The flag sends it to the task its own affinity names, which is the app's, where it belongs and
     * where it can be come back to.
     *
     * **And the intent deliberately carries no categories.** A launcher tap carries
     * `CATEGORY_LAUNCHER` and a `startActivity` from inside the app carries none, which is how
     * checkpoint D's launcher forward will tell the two apart. Testing for the category rather than
     * passing a "do not bounce me" extra is what stops these two activities forwarding into each
     * other, and it is correct by default for every later caller instead of correct for as long as
     * each of them remembers to opt out.
     */
    private fun openFullApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

/**
 * The body of the floating window: the shared compact controls, wired to this host's `Context`.
 *
 * **The start button's wiring is the short version of `DimScreen`'s**, and the reason is the guard
 * above: the notification permission is a precondition of this window existing at all, so the ask
 * can never fire from here. What is left is the pair that must stay together — the stored intent and
 * the service — written the same way round as on the full screen.
 *
 * `onClose` is left at its default `null`: an Activity is closed by the back gesture, so a button
 * that did the same thing would be a fourth icon buying nothing. The panel, which the Back key never
 * reaches, is the host that passes one.
 *
 * No `extras` on the `viewModel()` call, unlike every screen inside `NavDisplay`: those resolve to a
 * bare per-entry `ViewModelStoreOwner` with no default extras, and this one resolves to the Activity,
 * which provides `APPLICATION_KEY` itself (`ui/AppViewModelExtras.kt`). Same `DimViewModel` as the
 * full screen, a different instance — it is scoped to whichever host is showing it, and both read
 * the same DataStore keys, so the two agree without talking to each other.
 */
@Composable
private fun ControlsBody(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DimViewModel = viewModel(factory = DimViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CompactControls(
        dimLevel = state.dimLevel,
        warmth = state.warmth,
        running = state.running,
        autoOff = state.autoOff,
        onDimLevel = viewModel::setDimLevel,
        onWarmth = viewModel::setWarmth,
        onAutoOff = viewModel::setAutoOff,
        onToggleRunning = {
            if (state.running) {
                viewModel.endShade(ShadeEnd.ByHand)
                onStop()
            } else {
                viewModel.beginShade()
                onStart()
            }
        },
        onOpenApp = onOpenApp,
        modifier = modifier,
    )
}
