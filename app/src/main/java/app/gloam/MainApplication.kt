package app.gloam

import android.app.Application
import app.gloam.data.AppPreferences
import app.gloam.data.ThemeMode
import app.gloam.theme.applyThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    /**
     * Whether a launcher tap opens the compact controls instead of this app's own screen
     * (`docs/phase-3.md` §3). `MainActivity.onCreate` reads it before it composes anything.
     *
     * **Seeded by the blocking startup read and then kept current by a collector**, which is one
     * more moving part than [startupThemeMode] needs and the reason is that nothing corrects this
     * one afterwards. The theme mode is an initial value for a `Flow` the first composition
     * collects, so a stale seed is wrong for a frame; this value is read once, before there is a
     * composition, and then acted on irrevocably by finishing the Activity. A user who turns the
     * preference on and leaves the app has killed no process — so without the collector the next tap
     * on the icon would be decided by what was on disk when the process started, which is the wrong
     * answer until something happens to kill it.
     *
     * Kotlin note: `@Volatile` because the collector writes from a background dispatcher and
     * `MainActivity` reads on the main thread. It makes the write visible to other threads; it is
     * not a lock, and none is wanted here — the value is a single boolean and a reader that catches
     * the previous one is a reader that ran before the user's tap.
     */
    @Volatile
    var launcherCompact: Boolean = false
        private set

    val container: AppContainer by lazy { AppContainer(this, preferences, applicationScope) }

    override fun onCreate() {
        super.onCreate()

        preferences = AppPreferences(preferencesStore)

        // A blocking read, deliberately and exactly once. Everything else about DataStore is a Flow,
        // but this value has to be known before the first window is created — a Flow collected in a
        // composition arrives a frame later, which is a visible light flash for a user who chose
        // dark. One small file read at process start is the cheaper of the two costs.
        // Both reads in one `runBlocking`, and the second is not a second disk read: DataStore
        // serves `store.data` from its in-memory cache once anything has collected it, which the
        // first line just did.
        runBlocking {
            startupThemeMode = preferences.themeModeNow()
            launcherCompact = preferences.launcherCompactNow()
        }
        applyThemeMode(startupThemeMode)

        // And from here on the preference keeps itself current — see [launcherCompact]. A `Flow`
        // collected in a scope that never ends is the closest thing here to a subscription that is
        // never unsubscribed; it is deliberate, because the thing being kept alive is one boolean
        // for the life of the process.
        preferences.launcherCompact
            .onEach { launcherCompact = it }
            .launchIn(applicationScope)
    }
}
