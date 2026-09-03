package app.gloam

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gloam.theme.AppTheme

/**
 * AppCompatActivity rather than ComponentActivity, and for one reason only: it is where
 * `AppCompatDelegate` lives, and `AppCompatDelegate` is what applies a per-app language on the
 * pre-13 half of the supported range (ADR-0004) and what moves the night mode (ADR-0006). Nothing
 * else here uses AppCompat — no views, no action bar, no AppCompat widgets — and AppCompatActivity
 * is a ComponentActivity subclass, so `setContent`, the window itself and the activity-result APIs
 * all still work unchanged.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MainApplication

        // **Shape iii's forward** (`docs/phase-3.md` §2), in `onCreate` and in [onNewIntent] both —
        // see [forwardToCompactControls] for why one of those is not enough.
        if (forwardToCompactControls(intent)) return

        // Edge-to-edge, and **not** `enableEdgeToEdge()`. Every path in androidx.activity's version
        // of that call — `EdgeToEdgeApi35` included — reaches `Window.setStatusBarColor` and
        // `setNavigationBarColor`, both deprecated in Android 15, and Play flags apps that call
        // them. There is no version of the call that avoids it, so what it did is split in two:
        // this line, which is all Compose actually depends on, and the bar colours, which live in
        // `themes.xml` where an attribute is not a deprecated method. `values/colors.xml` carries
        // the reasoning and `AppTheme` writes the icon appearance at runtime.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // **Nobody pans this window; the content pads itself.**
        //
        // The manifest asks for `adjustResize`, and on API 26–29 that is the only thing that works:
        // `WindowInsets.ime` is not reported before API 30, so the older half of the supported range
        // depends on the window actually being resized. From API 30 the same request is inert — the
        // line above sets `decorFitsSystemWindows = false`, and the window manager downgrades the
        // resize to a *pan*. Panning is worse than doing nothing: with the keyboard open on a long
        // form it slides the top of the form under the status bar and carries the top app bar, Save
        // button and all, off the top of the screen.
        //
        // So on API 30+ the system is told to do neither, and `Modifier.imePadding()` in
        // `Navigation.kt` handles the keyboard as the inset it now is. Set here rather than in the
        // manifest because the manifest cannot say "only on new enough Android", and the old
        // behaviour is still load-bearing below 30.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        setContent {
            // Read from the application rather than through `app.container`: the theme is decided
            // before anything else exists, and preferences are the one thing that has to be
            // readable that early.
            //
            // Kotlin note: a plain `Flow` has no current value the way a `StateFlow` does, so
            // collecting one as state needs an initial. `false` is also the stored default, which is
            // what keeps the first frame from being the wrong palette and then repainting.
            val materialYou by app.preferences.materialYou.collectAsStateWithLifecycle(initialValue = false)
            // The initial value is the one `onCreate` already read off disk, not `SYSTEM`: this
            // flow's first emission arrives after the first composition, so a stored `DARK` would
            // otherwise light-flash on every cold start. `MainApplication` paid for that read before
            // any Activity existed; spending it again here is free.
            val themeMode by
                app.preferences.themeMode.collectAsStateWithLifecycle(initialValue = app.startupThemeMode)

            AppTheme(themeMode = themeMode, dynamicColor = materialYou) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainNavigation()
                }
            }
        }
    }

    /**
     * A launcher tap on a task that already exists **never calls `onCreate`**, which is the platform
     * fact this override exists for.
     *
     * Read off the phone rather than reasoned about: with the preference on and Gloam's task still
     * in the background, `am start` with `CATEGORY_LAUNCHER` logs
     * `ActivityTaskManager: moveTaskToFront … result code=2` (`START_TASK_TO_FRONT`) and the existing
     * instance is simply resumed. The forward in `onCreate` cannot run, so the icon would honour the
     * preference on a cold start and quietly ignore it for as long as the task survived — which is
     * days. `launchMode="singleTop"` in the manifest is what turns that resume into a delivered
     * intent, and this is where it lands.
     *
     * **`setIntent` before the forward, and it is not bookkeeping.** `getIntent()` otherwise keeps
     * answering with the intent that created the Activity, so a later reader — a resume, a
     * configuration change — would find `CATEGORY_LAUNCHER` on it and forward again over a user who
     * had just come back from the dialog.
     *
     * Kotlin note: `onNewIntent` takes a non-null `Intent` here, unlike `onCreate`'s nullable
     * `Bundle` — the platform never delivers a null one, and the override signature says so.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardToCompactControls(intent)
    }

    /**
     * Send a **launcher** tap to the compact controls when the preference says so, and say whether
     * it did.
     *
     * The launcher entry stays on this Activity unconditionally and the preference moves where a tap
     * on it *lands*. The alternative was an `<activity-alias>` the app enables and disables, which on
     * many launchers removes the icon from the home screen and on some does not put it back — a
     * preference that can lose the app's icon is a preference that can lose the setting that lost it
     * (`docs/phase-3.md` §2, shape iii).
     *
     * **The category test is the load-bearing half, not the preference.** `ControlsActivity` forwards
     * back here whenever it cannot draw a working dialog — no overlay permission, no escape hatch —
     * so a forward keyed on the preference alone is two activities bouncing off each other with no
     * user in the loop. A launcher tap always carries `CATEGORY_LAUNCHER` and a `startActivity` from
     * inside the app never does, so this tests for the one case the preference is actually about, and
     * every later entry — a deep link, a shortcut — is correct by default rather than correct for as
     * long as somebody remembers to opt out.
     *
     * **`finish()` on both paths, including the one where this Activity was already alive.** The user
     * asked the icon for the sliders; leaving the full app resumed behind the dialog would hand it
     * back to them the moment the dialog goes away, which `noHistory` makes a matter of seconds. What
     * is lost is a position in the app's own navigation, and the app opens on the dim screen anyway.
     *
     * The cost is one frame of this Activity's starting window before the dialog appears, which is
     * the flash shape iii buys the icon back with.
     */
    private fun forwardToCompactControls(intent: Intent): Boolean {
        val app = application as MainApplication
        if (!app.launcherCompact || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) return false
        // No `FLAG_ACTIVITY_NEW_TASK` and deliberately no categories: the dialog belongs in the task
        // this tap brought forward, and `ControlsActivity` is `noHistory`, so it leaves with the user
        // rather than waiting in the task for them to come back to something else.
        startActivity(Intent(this, ControlsActivity::class.java))
        finish()
        return true
    }
}
