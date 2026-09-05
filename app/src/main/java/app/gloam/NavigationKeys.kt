package app.gloam

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every route in the app, as a key.
 *
 * **Decide the navigation structure before the first screen.** Routes that exist from the start —
 * even where the screen behind them is still a stub — are what stop navigation from being reverse
 * engineered out of whatever the screens happened to need.
 *
 * Kotlin note: `data object` is a singleton with a sensible `toString`/`equals` — the right shape
 * for a route with no arguments. A route that carries arguments is a `data class` instead, and
 * `@Serializable` is what lets Nav3 save the whole back stack across process death.
 */
@Serializable data object Dim : NavKey

@Serializable data object Settings : NavKey

/**
 * Help and feedback. A detail screen, not a tab: it is somewhere you go once, from Settings' *About*
 * section, rather than a place the bottom bar switches to.
 */
@Serializable data object Support : NavKey

@Serializable data object Licences : NavKey

/**
 * The nightly window: on at one time, off at another. **A detail screen, not a tab** — somewhere you
 * go once, rather than a place the bottom bar switches to — reached from the summary row on the dim
 * screen rather than from Settings, because when the shade is on is not a property of the app, it is
 * the thing the app does (`docs/phase-4.md` §10).
 *
 * Kotlin note: this is a *route*, and `app.gloam.shade.Schedule` is the *value* it edits. Two things
 * named for one idea in two packages, which Kotlin resolves by an explicit import beating a
 * same-package declaration — so a file in this package that means the value has to import it, and
 * `MainApplication` does.
 */
@Serializable data object Schedule : NavKey

@Serializable
data class LicenceText(
    val spdxId: String,
) : NavKey

/**
 * The tabs, in bar order.
 *
 * An enum rather than a list of tuples so the bar, the back-stack root logic and any test asserting
 * the set all read the same declaration. Adding a tab is one entry here.
 *
 * The label is a `@StringRes` id, not a `String`: a bottom bar is composed once and would otherwise
 * cache the label from whichever locale was current at the time, which is exactly the bug an in-app
 * language switcher creates (ADR-0004).
 */
enum class TopLevelDestination(
    val key: NavKey,
    @param:StringRes val label: Int,
    val icon: ImageVector,
) {
    DimTab(Dim, R.string.tab_dim, Icons.Filled.Home),
    SettingsTab(Settings, R.string.tab_settings, Icons.Filled.Settings),
}
