package app.gloam.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.gloam.ControlsActivity
import app.gloam.MainApplication
import app.gloam.shade.readBacklight
import app.gloam.shade.startShade
import app.gloam.theme.Spacing
import app.gloam.ui.common.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * ## The two-minute deadline — Phase 2, checkpoint B
 *
 * R7, R8a and R8b all need a deadline shorter than thirty minutes, and **nothing in the shipped app
 * can make one**: `AutoOff`'s smallest value is `Minutes30`, and a debug-only entry in that enum
 * would live in `main/`, drive the chip row, and put its label through the translation gate — the
 * exact failure this source-set seam exists to prevent. `adb` cannot do it either, because DataStore
 * sits in credential-encrypted storage and the live process holds its state in memory.
 *
 * So the same justification the sweep above carries applies here: **only the app can do this to
 * itself**, which is why the button is in the app and behind the seam rather than in a script.
 *
 * ## Opening the compact controls — Phase 3, checkpoint C
 *
 * `ControlsActivity` is `exported="false"`, because the only things that legitimately open it are
 * this app's own notification and 2b's tile, and a dialog any installed app could raise over the
 * foreground app is a different thing entirely. That is also why `adb shell am start -n` cannot
 * reach it — it runs as uid 2000 and the activity manager refuses with `not exported from uid …`,
 * which `docs/phase-3.md` §12 did not allow for when it wrote that command down.
 *
 * So the same justification the two above carry applies a third time: **only the app can do this to
 * itself**. R2, R3 and R5 are all taken through this button until checkpoint D builds the routes a
 * user will actually use.
 *
 * None of this reaches a release binary, and none of its strings reach the translation gate — which
 * is why the text here is hardcoded English rather than a string resource.
 */
@Composable
fun DebugSettings() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { (context.applicationContext as MainApplication).preferences }

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

        Row(modifier = Modifier.padding(bottom = Spacing.base)) {
            // `beginShade` rather than a deadline setter, because there is no deadline setter: the
            // intent and the deadline are written as a pair so that neither can be stale beside the
            // other. Starting the service alongside it keeps the two agreeing when the button is
            // tapped with no shade up — the arrival is the thing being measured, and a deadline with
            // nothing running measures the resume reconcile instead.
            Button(
                onClick = {
                    scope.launch {
                        val deadline = System.currentTimeMillis() + ARM_MILLIS
                        preferences.beginShade(deadline)
                        context.startShade()
                        Log.i(TAG, "armed auto-off for $deadline (${ARM_MILLIS}ms out)")
                    }
                },
            ) {
                Text("Arm 2-minute deadline")
            }
        }

        Row(modifier = Modifier.padding(bottom = Spacing.base)) {
            // `FLAG_ACTIVITY_NEW_TASK` so this reproduces the route checkpoint D will build rather
            // than a convenient one: the notification's `PendingIntent` starts the activity from
            // outside any task of ours, and `ControlsActivity` declares an empty `taskAffinity`, so
            // the window under measurement is the one that lands in its own task. Without the flag
            // it would open inside Settings' task instead, which is a different window ordering
            // from the one R2 is reading.
            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, ControlsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) {
                Text("Open compact controls")
            }
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

/**
 * Two minutes: short enough to sit through with the phone in hand, and — deliberately — **longer
 * than one** `DEADLINE_RECHECK_MS`, so R7 measures the re-check loop waking up rather than a single
 * `delay` that happened to be shorter than the cap.
 */
private const val ARM_MILLIS = 120_000L
