package app.gloam

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.gloam.data.AppPreferences
import kotlinx.coroutines.CoroutineScope

/**
 * `internal` rather than private: [MainApplication] holds the one [AppPreferences] over this store
 * and hands it to the container, because the theme has to read a preference before the first window
 * exists.
 *
 * Kotlin note: `by preferencesDataStore(...)` is a property delegate on `Context` — it creates the
 * store once per process and hands the same instance to every caller. Creating a second DataStore
 * over the same file throws, so this must stay the only one.
 */
internal val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Manual dependency injection — deliberately not Hilt.
 *
 * At the size a single-developer app reaches, constructing things by hand is clearer than a
 * generated graph: the whole object graph is this file, you can read it top to bottom, and there is
 * no annotation processor round trip in every build. Migrating to Hilt later is mechanical, because
 * everything here is already constructor injection.
 *
 * The rule that makes it work: **nothing constructs its own dependencies.** Only this file calls a
 * constructor. The moment a class reaches for a singleton instead, it stops being testable without
 * a device.
 *
 * **It is nearly empty on purpose.** Gloam stores settings rather than records — a dim level and a
 * warmth, which DataStore holds — so there is no database, no repository and no media pipeline for
 * this to assemble. It stays because it is the seam the shade controller and anything else with a
 * lifetime longer than a screen will hang off, and an empty seam is cheaper to fill than one that
 * has to be introduced later.
 *
 * Lives at the package root with the app shell: like `Navigation.kt`, it describes how the app hangs
 * together rather than belonging to any one screen.
 */
class AppContainer(
    @Suppress("unused") private val context: Context,
    val preferences: AppPreferences,
    @Suppress("unused") private val applicationScope: CoroutineScope,
)
