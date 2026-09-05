package app.gloam.ui.schedule

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gloam.R
import app.gloam.shade.Schedule
import app.gloam.shade.minutesOf
import app.gloam.theme.Spacing
import app.gloam.ui.appViewModelExtras
import app.gloam.ui.common.DetailScaffold
import app.gloam.ui.common.SwitchRow
import app.gloam.ui.common.WarningBanner
import app.gloam.ui.dim.rememberTimeText
import app.gloam.work.hasAutostartSettings
import app.gloam.work.isIgnoringBatteryOptimisations
import app.gloam.work.openBatteryOptimisationSettings
import java.time.LocalTime

/**
 * **When the shade goes up, and when it comes down: one pair of times, every night.**
 *
 * A detail screen reached from the dim screen's summary row rather than from Settings, because when
 * the shade is on is not a property of the app — it is the thing the app does, and it belongs beside
 * the other control over when the shade ends (`docs/phase-4.md` §10).
 *
 * ## What this screen does *not* do
 *
 * **It does not arm anything.** It writes two preferences; `MainApplication` collects the schedule
 * for the life of the process and arms the alarm from there. That split is why a force-stop and an
 * app update recover on their own: the thing whose lifetime matches an alarm's is the process, not
 * a screen somebody happened to open.
 *
 * **It does not gate its own toggle on the battery exemption.** The schedule can be switched on
 * without it and the banner says what is at risk — the Phase 2 precedent, where the shade starts
 * even if the notification permission was refused. Refusing to run because a permission is missing
 * is the app deciding it knows better; the honest move is to run and say plainly what was given up.
 *
 * ## The one refusal and the one warning, which are deliberately not the same treatment
 *
 * A window whose two edges are the *same time* is refused at the picker, because read as a
 * twenty-four-hour window it is a shade that never comes down — the single failure this app exists
 * to design out. `Schedule` still answers *never* for it, so the refusal here is a convenience
 * rather than the only thing standing in front of that.
 *
 * A *short* window is warned about and not refused, from the same reasoning read the other way.
 * R0–R3 measured an inexact alarm arriving at the far end of a batching window, so a window of a few
 * minutes can be missed entirely — and a schedule that quietly does nothing is the visible, harmless
 * failure, which is the one to prefer.
 */
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel =
        viewModel(factory = ScheduleViewModel.Factory, extras = appViewModelExtras()),
) {
    val schedule by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // A live read, re-taken on every resume, and never a remembered outcome: the fix for it is a
    // settings screen this screen hands the user off to, and they come back. It is the only one of
    // the plan's four asks the app can read at all — `SYSTEM_ALERT_WINDOW` and `POST_NOTIFICATIONS`
    // are the other two that can, and Xiaomi's autostart is the one that cannot (§7's table).
    var exempt by remember { mutableStateOf(context.isIgnoringBatteryOptimisations()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) exempt = context.isIgnoringBatteryOptimisations()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Which picker is open, if either. Not `rememberSaveable`: a rotation mid-pick is rare enough
    // that reopening the row costs less than carrying a half-made time across a configuration
    // change, and the values it would restore are already on disk unchanged.
    var picking by remember { mutableStateOf<Edge?>(null) }

    DetailScaffold(
        title = stringResource(R.string.schedule_title),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier =
                contentModifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            SwitchRow(
                title = stringResource(R.string.schedule_enable),
                subtitle = stringResource(R.string.schedule_enable_hint),
                checked = schedule.enabled,
                onChange = viewModel::setEnabled,
            )

            TimeRow(
                label = stringResource(R.string.schedule_on_at),
                time = schedule.onAt,
                onClick = { picking = Edge.On },
            )
            TimeRow(
                label = stringResource(R.string.schedule_off_at),
                time = schedule.offAt,
                onClick = { picking = Edge.Off },
            )

            // The one derived fact worth saying, and the single most common way a person mis-reads a
            // pair of times. Nothing at all when the window does not cross midnight, rather than a
            // line saying it does not.
            if (schedule.onAt > schedule.offAt) {
                Note(stringResource(R.string.schedule_overnight))
            }
            if (schedule.isShort()) {
                Note(stringResource(R.string.schedule_short_window))
            }

            // Only while the schedule is on: a banner about a permission a disabled feature would
            // want is the app asking for something it is not using yet (`PLAN.md` rule 4).
            if (schedule.enabled && !exempt) {
                WarningBanner(
                    title = stringResource(R.string.schedule_battery_title),
                    body = stringResource(R.string.schedule_battery_body_delegated),
                    actionLabel = stringResource(R.string.schedule_battery_action),
                    // The Boolean is dropped for the same reason the autostart row drops its own: a
                    // false here means every destination in the chain refused us, which is nothing
                    // the user could act on.
                    onAct = { context.openBatteryOptimisationSettings() },
                )
            }

            // Present on phones that have an autostart manager, and only there. It points at the row
            // in Settings rather than growing a second hand-off for one grant — two hand-offs is two
            // places to keep honest — and it makes no claim about the state, because the app cannot
            // read that one back at all (ADR-0003). Denying it costs a worse on-edge rather than the
            // whole feature: the reconcile runs when the *user* opens Gloam, which no autostart
            // policy governs.
            if (remember(context) { context.hasAutostartSettings() }) {
                Note(stringResource(R.string.schedule_rom_note))
            }
        }
    }

    picking?.let { edge ->
        TimePickerDialog(
            title = stringResource(if (edge == Edge.On) R.string.schedule_on_at else R.string.schedule_off_at),
            initial = if (edge == Edge.On) schedule.onAt else schedule.offAt,
            otherEdge = if (edge == Edge.On) schedule.offAt else schedule.onAt,
            onDismiss = { picking = null },
            onConfirm = { picked ->
                // Both edges every time, never one: the window's three shapes are properties of the
                // pair, and a setter that could move one edge alone is one that can put a
                // half-written window in front of the collector that arms the alarm from it.
                if (edge == Edge.On) {
                    viewModel.setWindow(onAt = picked, offAt = schedule.offAt)
                } else {
                    viewModel.setWindow(onAt = schedule.onAt, offAt = picked)
                }
                picking = null
            },
        )
    }
}

/**
 * Which end of the window a picker is open for.
 *
 * Kotlin note: an enum used as the nullable state itself — `Edge?` is *closed, one open, or the
 * other*, three states in one value. Two booleans would make "both pickers open" representable, and
 * a state that cannot be drawn should not be expressible.
 */
private enum class Edge {
    On,
    Off,
}

/**
 * Is the window short enough that an alarm could step over it entirely?
 *
 * **Measured rather than invented, and it is not the rate limit this warning was first written
 * about.** Checkpoint A read every cell arriving at `maxWhenElapsed` exactly, and `dumpsys alarm`
 * gives an inexact alarm a batching window of 75% of its futurity capped at an hour — so with §4's
 * chain closing to a final hop of a few minutes, the worst arrival is a few minutes past `onAt`. Ten
 * minutes is the zone where that lateness is a comparable size to the window itself.
 *
 * Deliberately **not** in `shade/Schedule.kt`. This is the nominal wall-clock length, and on the two
 * nights a year the clock jumps the real window is an hour longer or shorter than this says — which
 * is fine for deciding whether to show a sentence and wrong for anything that computes an instant. A
 * function called `length` in the domain would be borrowed for the second job within a phase.
 */
private fun Schedule.isShort(): Boolean {
    if (!enabled || onAt == offAt) return false
    val minutes = Math.floorMod(minutesOf(offAt) - minutesOf(onAt), MINUTES_PER_DAY)
    return minutes < SHORT_WINDOW_MINUTES
}

private const val SHORT_WINDOW_MINUTES = 10
private const val MINUTES_PER_DAY = 24 * 60

/** A row that states a time and opens the picker that changes it. */
@Composable
private fun TimeRow(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.base, vertical = Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = rememberTimeText(time),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Supporting text under the pair: the overnight line, the short-window warning, the ROM note. */
@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.tight),
    )
}

/**
 * A Material 3 dial in a dialog, with the degenerate window refused at the *Set* button.
 *
 * **`is24Hour` comes from the phone and not from the locale**, because those two disagree and the
 * phone is the one the user set. Same call `rememberTimeText` makes on the way back out, so what the
 * picker shows and what the row shows cannot drift apart.
 *
 * **A raw `Dialog` rather than an `AlertDialog`**: the dial is wider than a platform dialog's default
 * width, so `usePlatformDefaultWidth = false` is what stops it being clipped at the sides — and the
 * `Surface` under it is then ours to shape, which is why it carries the app's own corner radius.
 *
 * The refusal is shown rather than the picker being closed: a dialog that vanished and left the old
 * time in place would look like the tap missed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: LocalTime,
    otherEdge: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val state =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
    var sameTime by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = Spacing.hair,
            modifier = Modifier.padding(Spacing.base),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.base).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .align(Alignment.Start)
                            .padding(bottom = Spacing.base),
                )
                TimePicker(state = state)
                if (sameTime) {
                    Text(
                        text = stringResource(R.string.schedule_same_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = Spacing.tight),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.tight),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(onClick = {
                        val picked = LocalTime.of(state.hour, state.minute)
                        if (picked == otherEdge) sameTime = true else onConfirm(picked)
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}
