package app.gloam.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gloam.R
import app.gloam.data.ThemeMode
import app.gloam.theme.Spacing
import app.gloam.ui.appViewModelExtras
import app.gloam.ui.common.SectionHeader
import app.gloam.work.hasAutostartSettings
import app.gloam.work.openAutostartSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenSupport: () -> Unit,
    onOpenLicences: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_appearance))

            Row(modifier = Modifier.padding(horizontal = Spacing.base)) {
                for (mode in ThemeMode.entries) {
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(stringResource(mode.labelRes())) },
                        modifier = Modifier.padding(end = Spacing.tight),
                    )
                }
            }

            SettingsSwitch(
                title = stringResource(R.string.settings_material_you),
                subtitle = stringResource(R.string.settings_material_you_hint),
                checked = state.materialYou,
                onChange = viewModel::setMaterialYou,
            )

            SectionHeader(stringResource(R.string.settings_language))
            LanguageRow()

            AutostartRow()

            SectionHeader(stringResource(R.string.settings_about))
            // Above the licences, because it is the row a tester needs and the other is the row a
            // licence obligation needs. Both are in *About* for the same reason: it is where someone
            // looks when they want to know something about the app rather than change it.
            SettingsRow(stringResource(R.string.settings_support), onClick = onOpenSupport)
            SettingsRow(stringResource(R.string.settings_licences), onClick = onOpenLicences)

            // The developer-only section. In a release build this composable is a no-op that
            // renders nothing — see `src/release/…/DebugSettings.kt`. It is a source-set seam
            // rather than an `if (BuildConfig.DEBUG)` because with `isMinifyEnabled = false` a
            // statically-false branch is still compiled into the AAB, and its strings are still
            // inside the translation gate. A hide is not a strip.
            DebugSettings()
        }
    }
}

/**
 * The autostart hand-off: **a permanent row, not a prompt, and no key remembers it** (ADR-0003).
 *
 * On HyperOS, autostart is what decides whether the process is started for a broadcast at all — not
 * `BOOT_COMPLETED` specifically, any broadcast — so reboot restore is a feature the ROM allows or
 * does not. Three reasons this is a row that is always here rather than a one-off ask:
 *
 * - **The app cannot know whether it worked.** There is no appop, the setting has no public state,
 *   and the OEM screen's own checkbox reports `checked=false` for granted rows (`device-gate.py`
 *   infers it from where the app's name sits relative to a divider, over `adb`, which is a
 *   host-side capability the app will never have). A confirmation checkbox here would be the app
 *   repeating the user's guess back to them as its own assurance. A row that is always present
 *   makes no claim at all.
 * - **A once-only prompt needs a stored "offered" flag** — a written DataStore key, frozen the day
 *   a stranger's phone holds it, bought to answer a question this row does not ask. This phase
 *   deleted one such key; adding another in the same phase would be a poor trade.
 * - **The failure it explains is invisible and recurring**, and needs explaining on the day the
 *   shade does not come back, which is not the day the app was installed.
 *
 * Gated on [hasAutostartSettings], which is honest only because the manifest names
 * `com.miui.securitycenter` in `<queries>` — without that, package-visibility filtering answers
 * "no such activity" on a phone where it exists. So the row is absent on every phone with no such
 * screen, rather than being a button that does nothing.
 *
 * `remember` rather than a re-read on resume, unlike the dim screen's permission checks: whether the
 * ROM *has* an autostart screen is a property of the phone, not a switch the user can flip.
 */
@Composable
private fun AutostartRow() {
    val context = LocalContext.current
    if (!remember(context) { context.hasAutostartSettings() }) return

    SectionHeader(stringResource(R.string.settings_restart))
    Text(
        text = stringResource(R.string.settings_restart_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.base),
    )
    Button(
        // The Boolean it returns is dropped rather than shown: the row only exists because
        // `resolveActivity` already answered, so a false here means the screen was resolvable and
        // then refused us — nothing the user could act on, and a message about it would be about
        // Gloam rather than about their phone.
        onClick = { context.openAutostartSettings() },
        modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.snug),
    ) {
        Text(stringResource(R.string.settings_restart_open))
    }
}

@Composable
private fun LanguageRow() {
    // Read on every composition rather than held in state: `setAppLanguage` recreates the Activity,
    // so this composable is rebuilt from scratch and the fresh read is always correct.
    val current = currentAppLanguage()
    Row(modifier = Modifier.padding(horizontal = Spacing.base)) {
        FilterChip(
            selected = current == null,
            onClick = { setAppLanguage(null) },
            label = { Text(stringResource(R.string.settings_language_system)) },
            modifier = Modifier.padding(end = Spacing.tight),
        )
        for (language in AppLanguage.entries) {
            FilterChip(
                selected = current == language,
                onClick = { setAppLanguage(language) },
                label = { Text(stringResource(language.labelRes)) },
                modifier = Modifier.padding(end = Spacing.tight),
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // The whole row toggles, not just the switch: a 48dp target the width of the screen
                // is far easier to hit than a 32dp one at the edge of it.
                .clickable { onChange(!checked) }
                .padding(horizontal = Spacing.base, vertical = Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.base, vertical = Spacing.base),
    )
}

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
