package app.gloam

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gloam.shade.canDrawShade
import app.gloam.shade.escapeHatchLive
import app.gloam.shade.readBacklightTop
import app.gloam.shade.startShade
import app.gloam.shade.stopShade
import app.gloam.theme.AppTheme
import app.gloam.theme.Spacing
import app.gloam.ui.dim.AutoOffControls
import app.gloam.ui.dim.DimControls
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

        // Read once rather than on every resume, and the difference from `DimScreen` is the window
        // rather than the value: this activity is `noHistory`, so it does not survive the trip to a
        // settings screen and back — every summon is a fresh read by construction.
        val backlightAvailable = readBacklightTop(this) != null

        setContent {
            val materialYou by app.preferences.materialYou.collectAsStateWithLifecycle(initialValue = false)
            val themeMode by
                app.preferences.themeMode.collectAsStateWithLifecycle(initialValue = app.startupThemeMode)

            AppTheme(themeMode = themeMode, dynamicColor = materialYou) {
                // `fillMaxWidth` and **not** `fillMaxSize`: the window's height is wrap-content, so
                // filling it would grow the dialog to whatever the window manager allows and hand
                // back a full screen with rounded corners.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompactControls(
                        backlightAvailable = backlightAvailable,
                        onStart = ::startShade,
                        onStop = ::stopShade,
                        onOpenApp = ::openFullApp,
                    )
                }
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
     * The way out to the full app, used by the guard and by the *Open Gloam* button.
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
 * The body of the floating window: the extracted controls, and one way out.
 *
 * **The start button's wiring is the short version of `DimScreen`'s**, and the reason is the guard
 * above: the notification permission is a precondition of this window existing at all, so the ask
 * can never fire from here. What is left is the pair that must stay together — the stored intent and
 * the service — written the same way round as on the full screen.
 *
 * No `extras` on the `viewModel()` call, unlike every screen inside `NavDisplay`: those resolve to a
 * bare per-entry `ViewModelStoreOwner` with no default extras, and this one resolves to the Activity,
 * which provides `APPLICATION_KEY` itself (`ui/AppViewModelExtras.kt`). Same `DimViewModel` as the
 * full screen, a different instance — it is scoped to whichever host is showing it, and both read
 * the same DataStore keys, so the two agree without talking to each other.
 */
@Composable
private fun CompactControls(
    backlightAvailable: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenApp: () -> Unit,
    viewModel: DimViewModel = viewModel(factory = DimViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Scrollable because the window's height is bounded by the display and the content is not: five
    // auto-off chips wrapping in a narrow window is what R5 reads, and a clipped safety control is
    // worse than a scrolled one.
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.base),
    ) {
        DimControls(
            dimLevel = state.dimLevel,
            warmth = state.warmth,
            lowerBacklight = state.lowerBacklight,
            backlightAvailable = backlightAvailable,
            running = state.running,
            onDimLevel = viewModel::setDimLevel,
            onWarmth = viewModel::setWarmth,
            onLowerBacklight = viewModel::setLowerBacklight,
            onToggleRunning = {
                if (state.running) {
                    viewModel.endShade()
                    onStop()
                } else {
                    viewModel.beginShade()
                    onStart()
                }
            },
        )

        // The deadline travels, per `docs/phase-3.md` §1: this is by construction the surface reached
        // while the shade is up, in the dark, without opening the app — which is exactly where
        // "turns off at 23:40" is worth reading and "give me two more hours" is worth tapping.
        AutoOffControls(
            autoOff = state.autoOff,
            offAtMillis = state.offAtMillis,
            running = state.running,
            onAutoOff = viewModel::setAutoOff,
        )

        // Without this the dialog is a dead end: there is no Settings behind it, no support screen
        // and no way to reach the explainer for anything it cannot do itself.
        Row(
            horizontalArrangement = Arrangement.End,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.tight),
        ) {
            TextButton(onClick = onOpenApp) {
                Text(stringResource(R.string.controls_open_app))
            }
        }
    }
}
