package app.gloam.ui.dim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.gloam.R
import app.gloam.shade.AutoOff
import app.gloam.shade.Schedule
import app.gloam.theme.Spacing
import app.gloam.ui.common.SectionHeader
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * The dim level, the warmth, the backlight switch and the start/stop button — **state in, callbacks
 * out, and no `Context`**.
 *
 * ## Why this is a separate file now, and was not before
 *
 * Phase 1 wrote the rule and kept to it: do not extract a composable with one caller, because you
 * are guessing at the second caller's shape. Phase 3 is the phase with the second caller — the
 * compact controls, and then the panel — so the guess is over and the extraction is a `refactor:`
 * with no behaviour change in it.
 *
 * ## What it may not touch, and why that is a hosting constraint rather than taste
 *
 * A composable that takes only values and lambdas can be rendered by **any** host. One that calls
 * `LocalActivity.current`, or asks for a `ViewModel`, or assumes a `Scaffold` above it, can only be
 * rendered where those have been provided — and Phase 3's third host is a raw `WindowManager` window
 * with no Activity under it at all, where the failure is a crash rather than a missing value.
 *
 * So: no service is started here, no permission is asked here, and `backlightAvailable` arrives as a
 * parameter rather than being read from a `Context`. Starting the shade is the *host's* job, because
 * a host is the thing that has a `Context` (`CLAUDE.md`: a `ViewModel` never holds one either) —
 * which is why [onToggleRunning] is a callback and not a `startShade()` call.
 *
 * **Kotlin note for a JS reader:** this is props-down / events-up and nothing more. What is worth
 * saying is Compose's version of the rule about where side effects may live — a `CompositionLocal`
 * is React context, and reading one that the host never provided is not `undefined`, it is an
 * exception at composition time.
 */
@Composable
fun DimControls(
    dimLevel: Int,
    warmth: Int,
    lowerBacklight: Boolean,
    backlightAvailable: Boolean,
    running: Boolean,
    onDimLevel: (Int) -> Unit,
    onWarmth: (Int) -> Unit,
    onLowerBacklight: (Boolean) -> Unit,
    onToggleRunning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(stringResource(R.string.dim_level_label))
        Text(
            text = stringResource(R.string.dim_level_value, dimLevel),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )
        DimLevelSlider(
            dimLevel = dimLevel,
            onDimLevel = onDimLevel,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )

        // Warmth is the second control, not a second ramp: the amber is a child of the same window
        // and the applied tint is scaled by the headroom the dim level leaves, so this slider says
        // what was asked for rather than what the composite ended up with. It starts at 0 — a colour
        // cast nobody asked for is indistinguishable from a broken screen.
        SectionHeader(stringResource(R.string.dim_warmth_label))
        WarmthSlider(
            warmth = warmth,
            onWarmth = onWarmth,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.tight),
        ) {
            Text(
                text = stringResource(R.string.dim_backlight_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // Visible and *disabled* rather than hidden when the device cannot honour it: a control
            // that is simply absent leaves the user unable to tell Gloam from a Gloam that behaves
            // differently on their phone than on someone else's.
            Switch(
                checked = lowerBacklight,
                onCheckedChange = onLowerBacklight,
                enabled = backlightAvailable,
            )
        }
        // Supporting text, always present rather than a one-off dialog. The symptom it explains — a
        // brightness slider that moves and does nothing — recurs every time the shade goes up, and a
        // dismissed dialog is not there when it does. On a device that cannot do it at all the
        // promise is replaced rather than left standing, which would be an explanation for a symptom
        // that is not happening.
        Text(
            text =
                stringResource(
                    if (backlightAvailable) {
                        R.string.dim_backlight_hint
                    } else {
                        R.string.dim_backlight_unavailable
                    },
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )

        // **The button travels, which is not obvious.** It is the one control that also needs a
        // service started — and starting it is the host's job, so what travels is the label and the
        // tap, never the `Context` behind them. Each host wires this to its own begin/stop pair, and
        // the compact host's version is the shorter one because the permission ask never fires
        // from there.
        Button(
            onClick = onToggleRunning,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.section),
        ) {
            Text(stringResource(if (running) R.string.dim_stop else R.string.dim_start))
        }
    }
}

/**
 * The dim slider on its own, with no label and no value above it.
 *
 * ## Why the two sliders are extracted and the rest of [DimControls] is not
 *
 * The compact surfaces do not render a smaller [DimControls]; they render a **different chrome**
 * around the same two controls — a bar and an icon row, against the full screen's headers, switch
 * and hint. Extracting along that seam is what keeps `PLAN.md`'s "one composable in every host"
 * promise where it actually pays: a change to what the dim range *means* — Phase 2b's ultra dark is
 * the one already scheduled — lands here once and reaches all three hosts. A `dense = true` flag on
 * [DimControls] would have been the other way to write this, and it is the one that rots: it makes
 * one composable answer to two layouts, and every later widget has to pick a side of the boolean.
 *
 * No `SectionHeader` and no percentage: both belong to a host that has room for them.
 */
@Composable
fun DimLevelSlider(
    dimLevel: Int,
    onDimLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = dimLevel.toFloat(),
        // The `Float`-to-`Int` narrowing lives here rather than in the callback's type, so every
        // host is handed the same whole number the preference stores.
        onValueChange = { onDimLevel(it.toInt()) },
        valueRange = 0f..100f,
        modifier = modifier,
    )
}

/**
 * The warmth slider on its own. The label is the host's, because the compact surfaces put it on the
 * disclosure row that reveals this rather than above it.
 */
@Composable
fun WarmthSlider(
    warmth: Int,
    onWarmth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = warmth.toFloat(),
        onValueChange = { onWarmth(it.toInt()) },
        valueRange = 0f..100f,
        modifier = modifier,
    )
}

/**
 * The auto-off chips and the *turns off at* line — **rendered by whichever host wants them**.
 *
 * Its own composable rather than a block inside [DimControls], and the split is the answer to a
 * question Phase 2 handed forward: whether the deadline travels to the floating host is then a
 * one-line decision *per host* rather than an extraction to be redone when twelve testers answer it.
 *
 * **`LocalContext` is the one Local this file reads, and it is safe in every host.** A `ComposeView`
 * provides it from the `View`'s own context, so it exists in the panel's raw window as much as in an
 * Activity — unlike `LocalActivity` or `LocalLifecycleOwner`, which a raw window has to be given
 * (§7). The time formatter needs it, and nothing else here does.
 */
@Composable
fun AutoOffControls(
    autoOff: AutoOff,
    offAtMillis: Long?,
    running: Boolean,
    onAutoOff: (AutoOff) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(stringResource(R.string.dim_auto_off_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            modifier = Modifier.padding(horizontal = Spacing.base),
        ) {
            // `FlowRow` rather than `Row`: five chips reading "After 30 minutes" do not fit one line
            // on a narrow screen in any language, and a clipped safety control is worse than a
            // wrapped one. R5 reads what that does in a floating window narrower than this one.
            for (choice in AutoOff.entries) {
                FilterChip(
                    selected = autoOff == choice,
                    onClick = { onAutoOff(choice) },
                    label = { Text(stringResource(choice.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(R.string.dim_auto_off_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.tight),
        )

        // Only with a shade up and a deadline on it — `Never` has nothing to say here, and a stopped
        // shade's deadline was cleared with the intent that owned it.
        if (running && offAtMillis != null) {
            Text(
                text = stringResource(R.string.dim_auto_off_at, rememberTimeText(offAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier.padding(
                        horizontal = Spacing.base,
                        vertical = Spacing.tight,
                    ),
            )
        }
    }
}

/**
 * A clock time rather than a countdown, **and that is a saving rather than a compromise.**
 *
 * A countdown has to tick, which is a recomposition every minute for the length of a session, and it
 * has to be formatted into words that plural correctly in every locale. A time does neither: it is
 * one string with one argument, and it does not change, so nothing ticks.
 *
 * Its one weakness, recorded so nobody re-discovers it as a bug: four hours from 23:00 reads as
 * "03:00" with no date. At a four-hour ceiling that is unambiguous enough in context.
 *
 * `internal` rather than private because the compact controls show the same deadline in their
 * timer section, and a second copy of this would be a second place for a locale bug to live.
 *
 * `DateFormat.getTimeFormat` is the platform's, so 12- or 24-hour follows the phone's own setting
 * and the locale for free — never a hand-built format string (`translator-brief.md` §4).
 */
@Composable
internal fun rememberTimeText(instant: Long): String {
    val context = LocalContext.current
    return remember(context, instant) {
        android.text.format.DateFormat
            .getTimeFormat(context)
            .format(Date(instant))
    }
}

/**
 * The same formatter, for a wall-clock time the **user set** rather than an instant the app
 * computed.
 *
 * A `LocalTime` has no date and no zone, and `DateFormat.getTimeFormat` formats a `Date` — so it
 * gets there by being put on today's date, which is the one line of glue between the two and lives
 * here rather than in each of the three callers. One weakness, recorded rather than re-discovered:
 * on the one night a year the clock springs forward, a time inside the missing hour resolves
 * forward, so a schedule set to 02:30 reads as 03:30 that day. That is also exactly when it will
 * come on (`shade/Schedule.kt`), so the reading is honest on the night and merely odd in the
 * daylight before it.
 */
@Composable
internal fun rememberTimeText(time: LocalTime): String =
    rememberTimeText(
        remember(time) {
            LocalDate
                .now()
                .atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        },
    )

/**
 * The schedule as one line, for the two surfaces that only *report* it.
 *
 * **It states the window rather than the setting** — *22:00 to 07:00*, not *On* — because the person
 * this line exists for is the one who set a schedule three weeks ago and forgot, and the window is
 * what they need to recognise. And when the battery exemption is missing it says *that* instead:
 * that user is by definition not opening the schedule screen where the banner lives, so the honest
 * state has to appear where they do go (`docs/phase-4.md` §10).
 *
 * @param title the row's own label, or `null` when [text] is a whole sentence that names the
 *   schedule itself. A caller that drew both would say "Schedule" twice.
 */
internal data class ScheduleSummary(
    val title: String?,
    val text: String,
)

@Composable
internal fun rememberScheduleSummary(
    schedule: Schedule,
    atRisk: Boolean,
): ScheduleSummary =
    when {
        schedule.enabled && atRisk ->
            ScheduleSummary(title = null, text = stringResource(R.string.dim_schedule_at_risk))
        schedule.enabled ->
            ScheduleSummary(
                title = stringResource(R.string.dim_schedule_row),
                text =
                    stringResource(
                        R.string.dim_schedule_window,
                        rememberTimeText(schedule.onAt),
                        rememberTimeText(schedule.offAt),
                    ),
            )
        else ->
            ScheduleSummary(
                title = stringResource(R.string.dim_schedule_row),
                text = stringResource(R.string.dim_schedule_off),
            )
    }

/**
 * Kotlin note: an extension on the enum rather than a field in it. The resource ids belong to the UI
 * layer and `AutoOff` has no Android in it at all — which is what lets `AutoOffTest` run on the JVM.
 */
private fun AutoOff.labelRes(): Int =
    when (this) {
        AutoOff.Never -> R.string.dim_auto_off_never
        AutoOff.Minutes30 -> R.string.dim_auto_off_30m
        AutoOff.Hour1 -> R.string.dim_auto_off_1h
        AutoOff.Hours2 -> R.string.dim_auto_off_2h
        AutoOff.Hours4 -> R.string.dim_auto_off_4h
    }
