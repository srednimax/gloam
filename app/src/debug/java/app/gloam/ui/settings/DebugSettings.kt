package app.gloam.ui.settings

import androidx.compose.runtime.Composable

/**
 * The developer-only section of Settings. **This file exists only in the debug source set** — see
 * the release half for why that is a source set and not a `BuildConfig.DEBUG` branch.
 *
 * Empty at the moment. The template's rows seeded and wiped sample records, and Gloam has no records
 * to seed — its whole state is a dim level and a warmth, both reachable from the screen itself.
 *
 * Add things here freely as the app grows: a "start the shade at 90% and finish" button for
 * screenshots, an overlay-permission stamp, a reading of what the backlight floor actually is on
 * this device. None of it reaches a release binary, and none of its strings reach the translation
 * gate.
 */
@Composable
fun DebugSettings() = Unit
