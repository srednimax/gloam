package app.gloam.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Which colour scheme applies, regardless of what the phone is set to. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Everything the app remembers that is not a row in the database.
 *
 * **DataStore, not SharedPreferences.** SharedPreferences' `commit()` blocks and its `apply()` can
 * still write on the main thread at the wrong moment; DataStore is `suspend`-only and transactional,
 * so there is no API here that can stall a frame.
 *
 * Kotlin note: every read is a `Flow`, not a value. There is no "current value" to ask for
 * synchronously — the first emission arrives after a disk read. That matters at startup: a screen
 * collecting `themeMode` gets `SYSTEM` for one frame and the stored value on the next, which is a
 * visible flash. [themeModeNow] exists for the one caller that must have the answer *before* the
 * first frame, and it is `suspend` because reading a file is.
 *
 * ## Adding a preference
 *
 * A key, a `Flow` that reads it with a default, and a `suspend` setter. Keep the default in the
 * `Flow` rather than writing it on first launch — a default that has been written to disk cannot be
 * changed in a later version without a migration, and a default that has not can.
 */
class AppPreferences(
    private val store: DataStore<Preferences>,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DIM_LEVEL = intPreferencesKey("dim_level")
        val SHADE_RUNNING = booleanPreferencesKey("shade_running")
    }

    /**
     * Light, dark or follow-the-system.
     *
     * Unknown stored values fall back to [ThemeMode.SYSTEM] rather than throwing: a preference file
     * survives a downgrade, and a crash loop at startup is a far worse outcome than the wrong theme.
     * This is the opposite of the rule for enums stored in the *database*, where an unknown value
     * means the schema gate missed something and should be loud.
     */
    val themeMode: Flow<ThemeMode> =
        store.data.map { prefs ->
            prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { enumValueOf<ThemeMode>(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        }

    /**
     * Whether to take the wallpaper's palette instead of the app's own (ADR-0006).
     *
     * Defaults **off**. With it on, nothing on Android 12+ reads the generated scheme at all, so the
     * app's identity would be invisible on almost every device that runs it.
     */
    val materialYou: Flow<Boolean> = store.data.map { it[Keys.MATERIAL_YOU] ?: false }

    /** Whether first-run setup has been completed. */
    val onboardingDone: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    /**
     * How dark the shade is, 0–100. **Not a brightness** — it runs the other way, and the backlight
     * is Android's value rather than ours (CONTEXT.md).
     *
     * Clamped on read as well as on write. A value outside the range can only arrive from a build
     * that used a different range, and the screen it feeds draws a slider that would throw on one.
     * Coercing is the behaviour that degrades rather than crashes.
     */
    val dimLevel: Flow<Int> = store.data.map { (it[Keys.DIM_LEVEL] ?: DEFAULT_DIM_LEVEL).coerceIn(0, 100) }

    /**
     * Whether the shade should be on screen.
     *
     * The user's intent, not the live state of the service — those are different questions and only
     * this one survives the process being killed. Xiaomi will kill the service; what it cannot do is
     * change this, which is what lets the shade come back rather than silently staying off.
     */
    val shadeRunning: Flow<Boolean> = store.data.map { it[Keys.SHADE_RUNNING] ?: false }

    /**
     * The theme mode, read once, before any Activity exists.
     *
     * The one place a `suspend` read is worth its cost: `MainApplication.onCreate` needs the answer
     * to call `applyThemeMode` *before* the first window is created, and a `Flow` collected in a
     * composition arrives a frame too late — which is a light flash on every cold start for a user
     * who chose dark.
     */
    suspend fun themeModeNow(): ThemeMode = themeMode.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        store.edit { it[Keys.MATERIAL_YOU] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setDimLevel(level: Int) {
        store.edit { it[Keys.DIM_LEVEL] = level.coerceIn(0, 100) }
    }

    suspend fun setShadeRunning(running: Boolean) {
        store.edit { it[Keys.SHADE_RUNNING] = running }
    }
}

/**
 * Where the slider starts on a first run.
 *
 * Deliberately modest. Someone who installs a screen dimmer and lands on a nearly-black screen has
 * no way to tell a working app from a broken phone, and the control to fix it is the thing they can
 * no longer see.
 */
const val DEFAULT_DIM_LEVEL = 40
