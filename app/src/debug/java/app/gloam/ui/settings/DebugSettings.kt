package app.gloam.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.gloam.shade.readBacklight
import app.gloam.theme.Spacing
import app.gloam.ui.common.SectionHeader
import kotlinx.coroutines.delay

/**
 * The developer-only section of Settings. **This file exists only in the debug source set** — see
 * the release half for why that is a source set and not a `BuildConfig.DEBUG` branch.
 *
 * ## The backlight sweep — Phase 1, checkpoint B
 *
 * Two questions the phone has to answer before the backlight half of the ramp can be written, and
 * neither can be answered from outside the app: **the window brightness override is per-window and
 * only its owner can set it**, so there is no `adb` command that applies one on our behalf.
 *
 * - *What does a given override float actually do to the panel?* The sweep walks a descending list
 *   of floats, holding each long enough to be read. `dumpsys display` keeps a hundred
 *   `BrightnessEvent` lines with `brt=` and `nits=` on each, so one dump after the run captures the
 *   whole curve — no polling, and nits without a light meter.
 * - *Is what the app computes from `Settings.System` a safe starting point?* The readout prints
 *   [app.gloam.shade.readBacklight]'s own answer beside the naive one an app would reach for, so
 *   both can be compared against what `dumpsys` says is really on the panel.
 *
 * **The sweep holds the screen on while it runs, and that is load-bearing rather than a courtesy.**
 * A screen approaching its inactivity timeout enters the DIM policy and is pinned to the panel's
 * floor regardless of any setting or override — which reads exactly like "the override did nothing"
 * and is how an earlier session mis-measured this phone by three orders of magnitude.
 *
 * None of this reaches a release binary, and none of its strings reach the translation gate — which
 * is why the text here is hardcoded English rather than a string resource.
 */
@Composable
fun DebugSettings() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Bumping this re-reads the settings; the read is cheap and there is nothing to observe, so a
    // button beats a ContentObserver for a developer row.
    var readCount by remember { mutableIntStateOf(0) }
    val reading = remember(readCount) { readBacklight(context) }

    var applied by remember { mutableFloatStateOf(RELEASED) }
    var sweepRun by remember { mutableIntStateOf(0) }

    LaunchedEffect(reading) { Log.i(TAG, "reading: $reading") }

    // The write itself. Keyed on the value so each step of the sweep lands, while the release on
    // leaving the screen is a separate effect — keying the release on `applied` too would release
    // and immediately re-apply on every step, and the panel would ramp between the two.
    LaunchedEffect(applied) { activity?.applyOverride(applied) }
    DisposableEffect(Unit) { onDispose { activity?.applyOverride(RELEASED) } }

    LaunchedEffect(sweepRun) {
        if (sweepRun == 0) return@LaunchedEffect
        Log.i(TAG, "sweep start: ${SWEEP.size} steps, ${STEP_MILLIS}ms each")
        SWEEP.forEachIndexed { index, value ->
            applied = value
            Log.i(TAG, "sweep step ${index + 1}/${SWEEP.size} screenBrightness=$value")
            delay(STEP_MILLIS)
        }
        applied = RELEASED
        Log.i(TAG, "sweep done, override released")
    }

    SectionHeader("Developer")

    Column(modifier = Modifier.padding(horizontal = Spacing.base)) {
        Text(
            text = reading.readout(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text =
                "override: " + if (applied < 0f) "released" else applied.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = Spacing.tight),
        )

        Row(modifier = Modifier.padding(top = Spacing.tight)) {
            Button(
                onClick = { sweepRun++ },
                modifier = Modifier.padding(end = Spacing.tight),
            ) {
                Text("Run sweep")
            }
            OutlinedButton(
                onClick = { applied = FLOOR },
                modifier = Modifier.padding(end = Spacing.tight),
            ) {
                Text("Hold floor")
            }
            OutlinedButton(onClick = { applied = RELEASED }) { Text("Release") }
        }

        Row(modifier = Modifier.padding(top = Spacing.tight, bottom = Spacing.base)) {
            // R3 in one tap: hold the app's own computed backlightTop, so `dumpsys` can be asked
            // whether engaging the override at dim level zero changes the panel at all.
            Button(
                onClick = { reading.top?.let { applied = it } },
                enabled = reading.top != null,
                modifier = Modifier.padding(end = Spacing.tight),
            ) {
                Text("Hold top")
            }
            OutlinedButton(onClick = { readCount++ }) { Text("Re-read") }
        }
    }
}

/** The computed number beside every ingredient that went into it, so a wrong one is visible. */
private fun app.gloam.shade.BacklightReading.readout(): String =
    buildString {
        appendLine("raw=$raw  adaptive=$adaptive  float=$settingFloat")
        appendLine("setting range=[$minSetting, $maxSetting]  (min unused: see Backlight.kt)")
        appendLine("float range=[$minFloat, $maxFloat]")
        appendLine("top=$top")
        append("($note)")
    }

/**
 * Apply — or release — this window's brightness override.
 *
 * `Window.getAttributes()` hands back the live `LayoutParams`, so the idiom is mutate-then-set:
 * assigning them back is what pushes the change to the window manager. `FLAG_KEEP_SCREEN_ON` rides
 * along with the override for the reason in [DebugSettings]'s notes.
 */
private fun Activity.applyOverride(value: Float) {
    window.attributes = window.attributes.apply { screenBrightness = value }
    if (value >= 0f) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

/** Compose hands out a `Context`, and the window belongs to the Activity behind it. */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val TAG = "GloamSweep"

/** `BRIGHTNESS_OVERRIDE_NONE`: hand the window back to the user's own brightness. */
private const val RELEASED = -1f

/**
 * The panel floor read off this phone (`mScreenBrightnessRangeMinimum`), as a one-tap hold for
 * looking at the darkest state with both eyes rather than through `dumpsys`.
 */
private const val FLOOR = 6.83661E-4f

/**
 * Descending, and resolved finely near the bottom because that is where this product lives.
 *
 * **Never `0f`.** `screenBrightness = 0.0f` is `BRIGHTNESS_OVERRIDE_OFF` — the backlight *off*, over
 * a live touchscreen. The bottom of this list is below the panel's floor on purpose, to see where
 * the driver clamps, which is a different question from where the app should stop.
 */
private val SWEEP =
    listOf(
        1.0f,
        0.75f,
        0.5f,
        0.4999f,
        0.25f,
        0.125f,
        0.0625f,
        0.03f,
        0.015f,
        0.008f,
        0.004f,
        0.002f,
        0.001f,
        6.83661E-4f,
        0.0003f,
        0.0001f,
    )

private const val STEP_MILLIS = 2500L
