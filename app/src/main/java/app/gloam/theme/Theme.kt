package app.gloam.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.gloam.data.ThemeMode

/**
 * The app's theme.
 *
 * **The scheme is always the app's own**, [LightColors] or [DarkColors] — there is no Material You
 * (wallpaper-derived) branch any more. It was an opt-in toggle until testers found wallpaper palettes
 * looked worse and differed on every phone, which is ADR-0006's own argument against it: a palette
 * you do not control is a contrast guarantee you cannot check. See ADR-0006's amendment.
 *
 * [themeMode] is the one Settings lever. It decides which of the two schemes applies; the window
 * background and the system-bar scrim are outside Compose's reach and are moved by [applyThemeMode]
 * instead. Resolved here rather than read back from the configuration so the scheme is right on the
 * *first* composition — AppCompat's `onConfigurationChanged` arrives a beat later, and one frame in
 * the wrong palette is what this whole setting exists to stop.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme = if (darkTheme) DarkColors else LightColors

    SystemBarAppearance(darkTheme)

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

/**
 * Whether the *app* is in its dark scheme — which is not the same question as whether the phone is.
 *
 * `isSystemInDarkTheme()` reads the configuration, and this app overrides the configuration
 * (ADR-0006): a user who chose Light on a phone in dark mode has to get the light column, and the
 * panel's overlay window has no Activity configuration to consult in the first place. So the answer
 * is published by [AppTheme], which is the one place that already resolved it.
 *
 * The default is `true`, matching the app's own default theme mode — a composable that renders
 * outside [AppTheme] is a bug, and this makes it a *dark* bug rather than a bright flash on a
 * screen somebody dimmed.
 *
 * Kotlin/Compose note: a `CompositionLocal` is React context — an implicit value passed down the
 * tree rather than through every call. `staticCompositionLocalOf` is the variant that does not track
 * reads individually: changing it re-composes everything under the provider instead of only the
 * readers. That is the right trade for a value that changes when the whole palette changes anyway,
 * and it costs nothing at every read in between.
 */
val LocalDarkTheme = staticCompositionLocalOf { true }

/**
 * Dark icons on the status and navigation bars under a light scheme, light icons under a dark one.
 *
 * **This is the runtime half of the edge-to-edge window**, and it lives here rather than in
 * `themes.xml` because the answer follows the app's own palette rather than the system's, and a
 * theme attribute is fixed at inflation. The other half — the scrim behind the bars — is static and
 * does live in the theme; `values/colors.xml` explains why both halves exist at all.
 *
 * Kotlin/Compose note: `DisposableEffect` is the "run this side effect and clean up after it" hook
 * — the closest analogue is `useEffect` with a cleanup return. Its keys are the dependency array:
 * the body re-runs when any of them changes and not otherwise, so flipping the phone into dark mode
 * repaints the icons while an ordinary recomposition does not touch the window at all. Nothing is
 * owed on the way out — the window is the activity's and outlives this composition — so `onDispose`
 * is empty rather than absent, which the API requires.
 *
 * `LocalActivity` is null in a `@Preview`, where there is no window to write to; returning early is
 * what keeps the previews rendering.
 */
@Composable
private fun SystemBarAppearance(darkTheme: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(activity, view, darkTheme) {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        onDispose {}
    }
}
