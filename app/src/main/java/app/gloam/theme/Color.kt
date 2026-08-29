package app.gloam.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #B0763C  — brand: filled buttons, FAB, selected tab
//   secondary #8A7561  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #8C7BA6  — accents and caution markers, used sparingly
//   neutral   #E3DCD3  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF86531C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCBD),
        onPrimaryContainer = Color(0xFF2D1600),
        inversePrimary = Color(0xFFFDB978),
        secondary = Color(0xFF6E5B48),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF9DEC6),
        onSecondaryContainer = Color(0xFF26190A),
        tertiary = Color(0xFF675780),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEDDCFF),
        onTertiaryContainer = Color(0xFF221439),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFFF8EF),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFFFF8EF),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFECE1CF),
        onSurfaceVariant = Color(0xFF4C4639),
        surfaceDim = Color(0xFFE0D9D0),
        surfaceBright = Color(0xFFFFF8EF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFAF3EA),
        surfaceContainer = Color(0xFFF4EDE4),
        surfaceContainerHigh = Color(0xFFEEE7DE),
        surfaceContainerHighest = Color(0xFFE8E1D8),
        inverseSurface = Color(0xFF33302A),
        inverseOnSurface = Color(0xFFF7F0E7),
        outline = Color(0xFF7E7667),
        outlineVariant = Color(0xFFCFC5B4),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFFFDB978),
        onPrimary = Color(0xFF4B2700),
        primaryContainer = Color(0xFF6A3C04),
        onPrimaryContainer = Color(0xFFFFDCBD),
        inversePrimary = Color(0xFF86531C),
        secondary = Color(0xFFDBC2AB),
        onSecondary = Color(0xFF3D2D1D),
        secondaryContainer = Color(0xFF554332),
        onSecondaryContainer = Color(0xFFF9DEC6),
        tertiary = Color(0xFFD1BEED),
        onTertiary = Color(0xFF37294F),
        tertiaryContainer = Color(0xFF4F4067),
        onTertiaryContainer = Color(0xFFEDDCFF),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF15130E),
        onBackground = Color(0xFFE8E1D8),
        surface = Color(0xFF15130E),
        onSurface = Color(0xFFE8E1D8),
        surfaceVariant = Color(0xFF4C4639),
        onSurfaceVariant = Color(0xFFCFC5B4),
        surfaceDim = Color(0xFF15130E),
        surfaceBright = Color(0xFF3C3933),
        surfaceContainerLowest = Color(0xFF100E09),
        surfaceContainerLow = Color(0xFF1E1B16),
        surfaceContainer = Color(0xFF221F1A),
        surfaceContainerHigh = Color(0xFF2D2A24),
        surfaceContainerHighest = Color(0xFF37342E),
        inverseSurface = Color(0xFFE8E1D8),
        inverseOnSurface = Color(0xFF33302A),
        outline = Color(0xFF989080),
        outlineVariant = Color(0xFF4C4639),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
