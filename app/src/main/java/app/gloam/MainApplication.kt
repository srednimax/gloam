package app.gloam

import android.app.Application
import app.gloam.data.AppPreferences
import app.gloam.data.ThemeMode
import app.gloam.theme.applyThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

/**
 * Holds the one [AppContainer] for the process.
 *
 * **There is no schema wipe guard here any more, and that is a deliberate consequence of having no
 * database.** The template's guard existed because an app that keeps a user's records cannot open a
 * file written by a schema it does not understand without risking them. Gloam keeps settings — a dim
 * level and a warmth — in DataStore, where a key the running build does not recognise is simply not
 * read and a key it expects but does not find falls back to the default declared beside it. There is
 * nothing to migrate and nothing to lose, so there is nothing to stand in front of.
 *
 * If a future feature adds a table, the guard comes back with it: `scripts/schema-gate.py`, the
 * house rules and ADR-0001 are all still in the repository for exactly that reason.
 *
 * Kotlin note: `by lazy` computes on first read and caches — the closest JS analogue is a memoised
 * getter.
 */
class MainApplication : Application() {
    /**
     * The scope for work that outlives any screen. `SupervisorJob` so one failed child does not
     * cancel its siblings — the closest JS analogue is `Promise.allSettled` semantics rather than
     * `Promise.all`.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The preference store. Read before the first window exists, because the theme depends on it.
     */
    lateinit var preferences: AppPreferences
        private set

    /**
     * The theme mode as it was on disk at process start, so the very first composition paints in the
     * right palette rather than flashing light and repainting.
     */
    var startupThemeMode: ThemeMode = ThemeMode.SYSTEM
        private set

    val container: AppContainer by lazy { AppContainer(this, preferences, applicationScope) }

    override fun onCreate() {
        super.onCreate()

        preferences = AppPreferences(preferencesStore)

        // A blocking read, deliberately and exactly once. Everything else about DataStore is a Flow,
        // but this value has to be known before the first window is created — a Flow collected in a
        // composition arrives a frame later, which is a visible light flash for a user who chose
        // dark. One small file read at process start is the cheaper of the two costs.
        startupThemeMode = runBlocking { preferences.themeModeNow() }
        applyThemeMode(startupThemeMode)
    }
}
