package app.gloam.ui.dim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.gloam.R
import app.gloam.shade.AutoOff
import app.gloam.theme.Spacing

/**
 * Which disclosure is open, if any — **one at a time, and that is a size bound rather than taste.**
 *
 * Both compact hosts are windows sized to their own content: the panel's is `WRAP_CONTENT` in
 * height, and the compact host's dialog grows until the display stops it. Two sections open at once
 * is a window a third taller than the design, over somebody else's app, at 1.59 nits. One at a time
 * makes the tallest possible state something that can be reasoned about instead of measured.
 *
 * Kotlin note for a JS reader: an `enum class` used purely as a closed set of states — the
 * discriminated union you would write as `'none' | 'timer' | 'warmth'`, except the compiler checks
 * the `when` over it is exhaustive.
 */
private enum class CompactSection {
    None,
    Timer,
    Warmth,
}

/** Tapping the open section's own button closes it; tapping another's swaps to it. */
private fun CompactSection.toggling(target: CompactSection): CompactSection =
    if (this == target) CompactSection.None else target

/**
 * **The controls as they are met in the dark: one bar, and a row of buttons under it.**
 *
 * The shared body of both compact surfaces — `ControlsActivity`'s floating dialog *below* the shade
 * and `PanelWindow`'s overlay *above* it. `DimControls` remains the full screen's; this is not a
 * smaller version of it but a different chrome around the same two sliders (see [DimLevelSlider]),
 * which is the seam that lets a change to what dim *means* still land once.
 *
 * ## What is here, and the rule that decides it
 *
 * A dim bar with no percentage over it, then timer / start-stop / open-app, then a warmth
 * disclosure. **Everything else is the full app's.** The backlight switch is the visible casualty
 * and it is the right one: it is a setting, chosen once, and the symptom it explains — a system
 * brightness slider that moves and does nothing — is now said on the notification instead, which is
 * the surface that is already legible while the shade is up. A surface reached with one tap in the
 * dark earns its value by *not* accumulating, and "settings live in the full app" is the only
 * version of that rule which survives the next feature wanting a row here.
 *
 * ## No percentage, deliberately
 *
 * A numeral inside or above the bar was the alternative. Inside is unreadable where this lives: the
 * filled and unfilled halves of the track pass under the digits, and both surfaces sit at a couple
 * of nits. Above it costs a line of height to say what the fill already shows. The number is worth
 * having when you are choosing a value, which is the full screen's job, not when you are nudging one.
 *
 * ## The timer is a disclosure, not a dialog
 *
 * Tapping the clock expands the presets in place. A modal was the first shape and it does not
 * survive the panel: an overlay window has no Activity to host a `Dialog`, so "show a modal" would
 * have meant a real dialog in one host and a hand-rolled scrim in the other — one design, two
 * implementations, and the harder of them in the window that must never be `MATCH_PARENT`. Expanding
 * in place is one mechanism in both, and it is the same one the warmth chevron uses.
 *
 * @param onClose the panel's way out, and **`null` in the compact host, which has no equivalent.**
 *   Closing an Activity is the system's job — the back gesture does it — but the panel carries
 *   `FLAG_NOT_FOCUSABLE`, so the Back key never reaches it and this button plus the idle timeout are
 *   its only exits that do not also take the shade down. A trailing icon rather than a fifth slot
 *   with its own meaning: it is the same *Close controls* the panel has always had.
 * @param onOpenApp the cog. Both hosts route it to the full app rather than to a settings screen —
 *   that is where Settings is, and it is also the only way to reach the explainer, the support
 *   screen and everything this surface deliberately does not carry.
 */
@Composable
fun CompactControls(
    dimLevel: Int,
    warmth: Int,
    running: Boolean,
    autoOff: AutoOff,
    offAtMillis: Long?,
    onDimLevel: (Int) -> Unit,
    onWarmth: (Int) -> Unit,
    onAutoOff: (AutoOff) -> Unit,
    onToggleRunning: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    // Not hoisted and not persisted: a disclosure is about the last few seconds, not about the user.
    // Both hosts are fresh per summon anyway — the compact host is `noHistory` and the panel builds a
    // new `PanelHost` each time — so "collapsed" is what every summon starts from by construction.
    //
    // Kotlin note: `by` delegation on `mutableStateOf` is `useState` with the setter hidden behind
    // assignment — `section = …` is the setter call, and reading `section` is what subscribes this
    // composable to it.
    var section by remember { mutableStateOf(CompactSection.None) }

    Column(modifier = modifier) {
        DimLevelSlider(
            dimLevel = dimLevel,
            onDimLevel = onDimLevel,
            modifier = Modifier.padding(horizontal = Spacing.base),
        )

        // **The three action buttons are centred as a group, and close is not one of them.** With
        // `SpaceEvenly` over all four the start/stop button drifted off-centre in the panel and sat
        // centred in the compact host — the same control in two places on two surfaces the user
        // meets as one thing. The weighted spacers pin the trio to the middle of the window whether
        // or not there is a close button, so the toggle is *the middle icon* in both hosts and the
        // one that closes the window stays out at the edge where a dismissal belongs.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.tight),
        ) {
            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { section = section.toggling(CompactSection.Timer) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer),
                        // The section header's own words, so the button and what it opens agree.
                        // Icon buttons have no visible label, so this is the only name a screen
                        // reader gets.
                        contentDescription = stringResource(R.string.dim_auto_off_label),
                    )
                }

                // Filled, and the only filled control here: it is the one button whose tap changes
                // what the screen looks like rather than what this window shows.
                // `FilledIconToggleButton` also carries its own state in its container colour, which
                // matters where the glyph itself may be the only thing a half-adapted eye resolves.
                FilledIconToggleButton(checked = running, onCheckedChange = { onToggleRunning() }) {
                    if (running) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stop),
                            contentDescription = stringResource(R.string.dim_stop),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.dim_start),
                        )
                    }
                }

                IconButton(onClick = onOpenApp) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.controls_open_app),
                    )
                }
            }

            // Kotlin note: a nullable function type is the parameter *and* the feature flag. There is
            // no separate `showClose` boolean to disagree with it — a host that passes no way to
            // close cannot render a button that would call nothing. The `Box` is claimed either way,
            // because it is what balances the spacer on the left.
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.panel_close),
                        )
                    }
                }
            }
        }

        when (section) {
            CompactSection.Timer ->
                TimerSection(
                    autoOff = autoOff,
                    offAtMillis = offAtMillis,
                    running = running,
                    onAutoOff = onAutoOff,
                )
            CompactSection.Warmth ->
                WarmthSlider(
                    warmth = warmth,
                    onWarmth = onWarmth,
                    modifier = Modifier.padding(horizontal = Spacing.base),
                )
            CompactSection.None -> Unit
        }

        // The chevron row is always present, below whatever is expanded, so its position does not
        // move when a section opens above it. Text rather than an icon because warmth has no
        // conventional glyph and the nearest ones are wrong: a sun means *brightness*, which is the
        // other mechanism entirely (CONTEXT.md), and putting it here would name the thing Gloam is
        // most often mistaken for.
        TextButton(
            onClick = { section = section.toggling(CompactSection.Warmth) },
            modifier = Modifier.padding(horizontal = Spacing.tight),
        ) {
            Text(stringResource(R.string.dim_warmth_label))
            Icon(
                imageVector =
                    if (section == CompactSection.Warmth) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                // Null rather than a description: the `Text` beside it in the same button is already
                // the button's name, and a second one would have a screen reader say it twice.
                contentDescription = null,
            )
        }
    }
}

/**
 * The auto-off presets, short.
 *
 * `AutoOffControls`' chips say *After 30 minutes*; R5 read five of those wrapping in a window this
 * wide, and these two surfaces are narrower still. Same five values, same setter — only the labels
 * are different, so a value added to [AutoOff] still appears in every host without anyone
 * remembering to come here.
 *
 * **The chips do not close the section.** Staying open is what lets the *turns off at* line below
 * them redraw under the finger that just tapped — which is the only confirmation this surface gives
 * that anything happened.
 */
@Composable
private fun TimerSection(
    autoOff: AutoOff,
    offAtMillis: Long?,
    running: Boolean,
    onAutoOff: (AutoOff) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.dim_auto_off_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.hair),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            modifier = Modifier.padding(horizontal = Spacing.base),
        ) {
            for (choice in AutoOff.entries) {
                FilterChip(
                    selected = autoOff == choice,
                    onClick = { onAutoOff(choice) },
                    label = { Text(stringResource(choice.shortLabelRes())) },
                )
            }
        }
        // The same condition the full screen uses: `Never` has nothing to say, and a stopped shade's
        // deadline went with the intent that owned it.
        if (running && offAtMillis != null) {
            Text(
                text = stringResource(R.string.dim_auto_off_at, rememberTimeText(offAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.tight),
            )
        }
    }
}

/** The short labels, beside `labelRes()`'s long ones. Same reason it is an extension: no Android in [AutoOff]. */
private fun AutoOff.shortLabelRes(): Int =
    when (this) {
        AutoOff.Never -> R.string.compact_auto_off_never
        AutoOff.Minutes30 -> R.string.compact_auto_off_30m
        AutoOff.Hour1 -> R.string.compact_auto_off_1h
        AutoOff.Hours2 -> R.string.compact_auto_off_2h
        AutoOff.Hours4 -> R.string.compact_auto_off_4h
    }
