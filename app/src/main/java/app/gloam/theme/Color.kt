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
//   primary   #B26A2E  — brand: filled buttons, FAB, selected tab
//   secondary #8A7561  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #8C7BA6  — accents and caution markers, used sparingly
//   neutral   #DED6CB  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF8F4E13),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC3),
        onPrimaryContainer = Color(0xFF301400),
        inversePrimary = Color(0xFFFFB77F),
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
        background = Color(0xFFFFF8ED),
        onBackground = Color(0xFF1E1B14),
        surface = Color(0xFFFFF8ED),
        onSurface = Color(0xFF1E1B14),
        surfaceVariant = Color(0xFFECE1CF),
        onSurfaceVariant = Color(0xFF4D4639),
        surfaceDim = Color(0xFFE1D9CE),
        surfaceBright = Color(0xFFFFF8ED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFBF2E7),
        surfaceContainer = Color(0xFFF6EDE2),
        surfaceContainerHigh = Color(0xFFEFE7DC),
        surfaceContainerHighest = Color(0xFFE9E1D6),
        inverseSurface = Color(0xFF343028),
        inverseOnSurface = Color(0xFFF8EFE4),
        outline = Color(0xFF7E7667),
        outlineVariant = Color(0xFFCFC5B3),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFFFFB77F),
        onPrimary = Color(0xFF4F2500),
        primaryContainer = Color(0xFF703700),
        onPrimaryContainer = Color(0xFFFFDCC3),
        inversePrimary = Color(0xFF8F4E13),
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
        background = Color(0xFF16130D),
        onBackground = Color(0xFFE9E1D6),
        surface = Color(0xFF16130D),
        onSurface = Color(0xFFE9E1D6),
        surfaceVariant = Color(0xFF4D4639),
        onSurfaceVariant = Color(0xFFCFC5B3),
        surfaceDim = Color(0xFF16130D),
        surfaceBright = Color(0xFF3D3931),
        surfaceContainerLowest = Color(0xFF110E08),
        surfaceContainerLow = Color(0xFF1E1B14),
        surfaceContainer = Color(0xFF231F18),
        surfaceContainerHigh = Color(0xFF2D2A22),
        surfaceContainerHighest = Color(0xFF38342D),
        inverseSurface = Color(0xFFE9E1D6),
        inverseOnSurface = Color(0xFF343028),
        outline = Color(0xFF999080),
        outlineVariant = Color(0xFF4D4639),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
