package app.gloam.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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

    // Read from [SecondWindow] rather than owned here, because the window deliberately outlives this
    // composition: the whole point is to leave the app with it up. `remember` seeds the label from
    // the real state on re-entry; the buttons keep it in step after that.
    var secondWindowUp by remember { mutableStateOf(SecondWindow.isUp) }

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

        Text(
            text = "second window: " + if (secondWindowUp) "up" else "down",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )

        Row(modifier = Modifier.padding(top = Spacing.tight, bottom = Spacing.base)) {
            Button(
                onClick = {
                    SecondWindow.add(context)
                    secondWindowUp = SecondWindow.isUp
                },
                enabled = !secondWindowUp,
                modifier = Modifier.padding(end = Spacing.tight),
            ) {
                Text("Add second window")
            }
            OutlinedButton(
                onClick = {
                    SecondWindow.remove(context)
                    secondWindowUp = SecondWindow.isUp
                },
                enabled = secondWindowUp,
            ) {
                Text("Remove second window")
            }
        }
    }
}

/**
 * R1's whole apparatus — Phase 3, checkpoint A.
 *
 * A second `TYPE_APPLICATION_OVERLAY` window of our own, put up over the live shade so that
 * `dumpsys` can be asked the two questions the panel is a bet on (`docs/phase-3.md` §5):
 *
 * - **Does it sit above the shade?** Both windows are the same type from the same uid, and ordering
 *   within a type is the window manager's business — the expectation is insertion order, which is an
 *   expectation rather than a documented guarantee. `dumpsys window windows` prints the stack in
 *   order, and the rectangle is a loud colour so a `screencap` says the same thing a second way: at
 *   dim 100 a magenta square *above* the shade stays magenta, and one below it is nearly black.
 * - **Who owns the backlight override?** This window sets no `screenBrightness` at all, so it leaves
 *   the field at `BRIGHTNESS_OVERRIDE_NONE` — *not asking*, rather than asking for nothing. By the
 *   rule Phase 1's R5 and R9 measured, the shade underneath should keep its `0.01`.
 *   `dumpsys display` is where that is read rather than assumed.
 *
 * **Bare on purpose.** No Compose, no lifecycle, no controls, nothing that could be blamed if the
 * answer is surprising. Every line the panel needs beyond this is a line written on the bet this
 * window is here to settle, which is why checkpoint A runs before any of them exist.
 *
 * **It carries `FLAG_NOT_TOUCHABLE`, which the real panel will not.** Touch is R7's question against
 * the real thing, not R1's, and a touchable rectangle dropped over an unknown screen is the trap
 * `docs/phase-3.md` §6 exists to bound — a debug button must not need its own way out. The flag
 * should not reach the answer either way: what a window asks for is a `LayoutParams` field, and
 * whether it can be touched is not one of the terms. R1 reads the result rather than trusting that.
 *
 * **A file-scoped object rather than composition state**, because the reading is taken with the app
 * in the background: `DisposableEffect` would take the window down at exactly the moment the phone
 * became worth looking at. The window dies with the process, which is the only cleanup a debug
 * surface owes.
 */
private object SecondWindow {
    private var view: View? = null

    val isUp: Boolean get() = view != null

    fun add(context: Context) {
        if (view != null) return
        val square = View(context).apply { setBackgroundColor(android.graphics.Color.MAGENTA) }
        val side = (SIDE_DP * context.resources.displayMetrics.density).toInt()
        val params =
            WindowManager
                .LayoutParams(
                    side,
                    side,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.OPAQUE,
                ).apply {
                    // Centred, so the square lands over neither the status bar's pull-down nor the
                    // gesture bar. The phone stays drivable with it up, which is what lets the
                    // reading be taken from other apps rather than from Gloam's own Settings screen.
                    gravity = Gravity.CENTER
                }
        runCatching { context.windowManager().addView(square, params) }
            .onSuccess {
                view = square
                Log.i(TAG, "second window added: ${side}px square, no screenBrightness set")
            }.onFailure { Log.w(TAG, "second window refused", it) }
    }

    fun remove(context: Context) {
        val square = view ?: return
        runCatching { context.windowManager().removeView(square) }
        view = null
        Log.i(TAG, "second window removed")
    }

    /**
     * The **application**'s window manager, not the Activity's. An Activity's carries that
     * activity's window token, and a window added with it is torn down with the activity — which is
     * the one thing this window must not do.
     */
    private fun Context.windowManager(): WindowManager = applicationContext.getSystemService(WindowManager::class.java)
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

/** Big enough to see and to land a `screencap` on, small enough to obscure nothing that matters. */
private const val SIDE_DP = 200
