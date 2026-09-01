package app.gloam.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.gloam.shade.AutoOff
import app.gloam.shade.NO_DEADLINE
import app.gloam.shade.deadlineOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The two values that say what the shade should be doing, read together because they are written
 * together (see [AppPreferences.beginShade]).
 *
 * **They are one fact, not two.** A caller that can read *running* without the deadline beside it is
 * a caller that can believe a shade is up nine hours after it should have come down — which is the
 * one way this app's stored values can disagree with each other. Phase 2's boot receiver wants
 * exactly this pair in one read, and so does the screen's resume reconcile.
 *
 * @param running what the user asked for, **not** whether the service is alive. The ROM kills the
 *   service without the user changing their mind; this is the value that survives that.
 * @param offAtMillis the instant the shade next comes down, or `null` for no deadline. Storage's `0`
 *   is collapsed to `null` here, at the one boundary that knows the sentinel.
 */
data class ShadeIntent(
    val running: Boolean,
    val offAtMillis: Long?,
)

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
        val DIM_LEVEL = intPreferencesKey("dim_level")
        val SHADE_RUNNING = booleanPreferencesKey("shade_running")
        val LOWER_BACKLIGHT = booleanPreferencesKey("lower_backlight")
        val WARMTH = intPreferencesKey("warmth")
        val AUTO_OFF_MINUTES = intPreferencesKey("auto_off_minutes")
        val OFF_AT_MILLIS = longPreferencesKey("off_at_millis")
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
     * Whether the shade should be on screen, and when it next comes down.
     *
     * The user's intent, not the live state of the service — those are different questions and only
     * this one survives the process being killed. Xiaomi will kill the service; what it cannot do is
     * change this, which is what lets the shade come back rather than silently staying off.
     *
     * One `Flow` for both keys rather than one each, because [beginShade] and [endShade] write them
     * in a single transaction and a reader that could take them apart is a reader that can see a
     * half-written intent.
     */
    val shadeIntent: Flow<ShadeIntent> =
        store.data.map { prefs ->
            ShadeIntent(
                running = prefs[Keys.SHADE_RUNNING] ?: false,
                offAtMillis = deadlineOrNull(prefs[Keys.OFF_AT_MILLIS] ?: NO_DEADLINE),
            )
        }

    /**
     * How long a hand-started shade stays up before Gloam takes it down (CONTEXT.md: **auto-off**).
     *
     * Stored as **minutes rather than the enum's name**, which is the opposite of what [themeMode]
     * does; `AutoOff.ofMinutes` carries the reason and the fallback. Defaults to `AutoOff.Default`
     * — on rather than to `Never`, because until Phase 2b's tile ships this is the only way out of
     * the shade that needs no gesture at all.
     */
    val autoOff: Flow<AutoOff> =
        store.data.map { AutoOff.ofMinutes(it[Keys.AUTO_OFF_MINUTES] ?: AutoOff.Default.minutes) }

    /**
     * Whether Gloam may take the **backlight** down before it draws the shade (ADR-0010).
     *
     * **Defaults on**, because it is the larger of the two mechanisms — on the development phone the
     * backlight alone is a factor of 250 in nits before a single pixel is drawn over anything, and
     * the whole product thesis is the light below the system slider's own floor. A user who wants
     * the shade alone switches it off; a user who wants the app to work does nothing.
     *
     * Kept as a preference rather than a constant because it has a visible cost the user may not
     * want: while it is on, their own brightness slider moves and does not apply. That is a taste,
     * not a safety floor, which is the line CLAUDE.md draws between the two.
     */
    val lowerBacklight: Flow<Boolean> = store.data.map { it[Keys.LOWER_BACKLIGHT] ?: true }

    /**
     * How far the shade is tinted amber, 0–100 (CONTEXT.md: **warmth**).
     *
     * **Defaults to 0**, for the same reason [DEFAULT_DIM_LEVEL] is modest: a colour cast nobody
     * asked for is indistinguishable from a broken screen, and warmth is only worth having because
     * someone chose it.
     *
     * Coerced on read as well as on write, like [dimLevel] — a value outside the range can only come
     * from a build with a different one, and it feeds a slider that would throw on it.
     *
     * A separate control from the dim level, but not an independent one: the applied tint is scaled
     * by the headroom the dim level leaves, because the two together decide whether anything
     * underneath stays legible. That arithmetic is `shadeValuesFor`'s, not this key's.
     */
    val warmth: Flow<Int> = store.data.map { (it[Keys.WARMTH] ?: 0).coerceIn(0, 100) }

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

    suspend fun setDimLevel(level: Int) {
        store.edit { it[Keys.DIM_LEVEL] = level.coerceIn(0, 100) }
    }

    suspend fun setAutoOff(choice: AutoOff) {
        store.edit { it[Keys.AUTO_OFF_MINUTES] = choice.minutes }
    }

    /**
     * The user wants the shade up, and this is when it comes down. **One transaction, both keys.**
     *
     * There is deliberately no `setShadeRunning` beside this and no `setDeadline` under it. A caller
     * that can write *running* without the deadline is a caller that can leave a stale deadline
     * behind a fresh start — the shade would come down minutes after going up, on a deadline set
     * hours ago — and `DataStore.edit` being transactional is what makes writing them together free
     * rather than a lock.
     *
     * **Also the "give me two more hours" path**, called with `running` already `true` when the user
     * taps a different auto-off chip. Writing the same `true` back costs nothing and keeps the
     * invariant, which is why that case did not need a third method.
     *
     * @param offAtMillis `null` for no deadline, stored as [NO_DEADLINE].
     */
    suspend fun beginShade(offAtMillis: Long?) {
        store.edit {
            it[Keys.SHADE_RUNNING] = true
            it[Keys.OFF_AT_MILLIS] = offAtMillis ?: NO_DEADLINE
        }
    }

    /**
     * The shade is down, however it got there — the user, the notification's Stop action, or the
     * deadline arriving.
     *
     * Clears the deadline with the flag, so the next read cannot find one belonging to a shade that
     * is no longer up.
     */
    suspend fun endShade() {
        store.edit {
            it[Keys.SHADE_RUNNING] = false
            it[Keys.OFF_AT_MILLIS] = NO_DEADLINE
        }
    }

    suspend fun setLowerBacklight(enabled: Boolean) {
        store.edit { it[Keys.LOWER_BACKLIGHT] = enabled }
    }

    suspend fun setWarmth(warmth: Int) {
        store.edit { it[Keys.WARMTH] = warmth.coerceIn(0, 100) }
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
