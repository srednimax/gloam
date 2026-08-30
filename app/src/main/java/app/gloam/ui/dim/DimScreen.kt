package app.gloam.ui.dim

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gloam.R
import app.gloam.shade.canDrawShade
import app.gloam.shade.readBacklightTop
import app.gloam.shade.shadePermissionIntent
import app.gloam.shade.startShade
import app.gloam.shade.stopShade
import app.gloam.theme.Spacing
import app.gloam.ui.appViewModelExtras
import app.gloam.ui.common.SectionHeader
import app.gloam.work.AppChannel
import app.gloam.work.NotificationPermissionOutcome
import app.gloam.work.channelCanAppear
import app.gloam.work.notificationRationaleAvailable
import app.gloam.work.notificationsAllowed
import app.gloam.work.openAppNotificationSettings
import app.gloam.work.openChannelNotificationSettings
import app.gloam.work.rememberNotificationPermissionAsk

/**
 * What the escape-hatch warning says, and which button it carries.
 *
 * **Derived on every resume from what the system currently reports, not from how an ask ended.** An
 * outcome-driven banner is stale exactly when it matters: the user taps *Open notification settings*,
 * grants the permission there, comes back — and a remembered `PermanentlyDenied` still tells them
 * they have no Stop button. It would also miss the opposite case entirely, a permission revoked while
 * the app sat in the background with the shade up.
 *
 * Kotlin note: an enum rather than a handful of booleans threaded through the composable. The three
 * bad states each need a different button, and a discriminated set of cases is what stops a fourth
 * combination being drawn by accident — the same reason you would reach for a union type in
 * TypeScript instead of `{ denied?: boolean; muted?: boolean }`.
 */
private enum class ShadeWarning {
    /** The notification can appear. There is a Stop button outside the app, so nothing to say. */
    None,

    /** Permission off, and the system dialog is still worth firing. */
    AskPermission,

    /** Permission off and the dialog spent — the app's own settings page is the only way back. */
    OpenAppSettings,

    /** Permission on, the channel muted — the fix is one screen further down than the page above. */
    OpenChannelSettings,
}

/**
 * The one screen the app is about: a slider and a switch for the shade.
 *
 * The screen owns starting and stopping the service rather than the `ViewModel` doing it — a
 * `ViewModel` has no `Context`, and handing it one is how it starts outliving its own scope.
 *
 * ## The entry gate
 *
 * Starting the shade for the first time asks for `POST_NOTIFICATIONS`, because the ongoing
 * notification's **Stop** action is the documented way out of a very dark screen and on Android 13+
 * that notification does not exist without the permission. The ask fires from the start button
 * rather than at first launch — the moment the feature that needs it is switched on — and **only
 * ever with no shade on screen**.
 *
 * That last rule is a platform constraint rather than a preference. The system hides or refuses
 * touches on non-system overlay windows while a permission dialog is up, precisely so that an app
 * cannot draw over the dialog and trick somebody into tapping *Allow*. Our shade is
 * `FLAG_NOT_TOUCHABLE`, which may or may not exempt it, and nobody should assume either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimScreen(
    modifier: Modifier = Modifier,
    viewModel: DimViewModel =
        viewModel(factory = DimViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Re-read on every resume rather than remembering the answer. `SYSTEM_ALERT_WINDOW` is a
    // special permission the user grants on a settings screen we send them to, and they can revoke
    // it there just as easily while the app is in the background — a cached `true` becomes a
    // service that starts and then cannot add its window.
    var canDraw by remember { mutableStateOf(context.canDrawShade()) }

    // The same property, three more times. Every one of these is a switch on a settings screen this
    // screen deliberately hands the user off to, so every one of them can change while the app is
    // backgrounded and none of them may be cached across that trip.
    var notificationsOn by remember { mutableStateOf(context.notificationsAllowed()) }
    var channelOn by remember { mutableStateOf(context.channelCanAppear(AppChannel.Shade)) }
    var rationaleLeft by remember { mutableStateOf(activity.notificationRationaleAvailable()) }

    // Whether this device hands Gloam a brightness it can trust. Answerable with no service and no
    // override applied — the resource resolves or it does not, the setting reads or it does not, the
    // decoded float is in range or it is not — and re-read on resume with the rest, because the user
    // can change their brightness mode on a settings screen and come back.
    var backlightAvailable by remember { mutableStateOf(readBacklightTop(context) != null) }

    // **Session state, deliberately not a DataStore key.** The only thing a stored flag would buy is
    // telling "never asked" apart from "asked twice and refused", and those two want the same
    // behaviour anyway — fire the launcher and let it answer. It flips the moment the launcher comes
    // back with `PermanentlyDenied`, which is Android saying the dialog is gone for good.
    var dialogSpent by remember { mutableStateOf(false) }

    fun reread() {
        canDraw = context.canDrawShade()
        notificationsOn = context.notificationsAllowed()
        channelOn = context.channelCanAppear(AppChannel.Shade)
        rationaleLeft = activity.notificationRationaleAvailable()
        backlightAvailable = readBacklightTop(context) != null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) reread()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The system reports no result for this screen, so the contract is only how we learn the user
    // came back. The answer itself comes from re-reading, above and here.
    val requestOverlay =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            reread()
        }

    /**
     * The intent and the service, together, in one place.
     *
     * They are written together on purpose: if `setRunning(true)` landed on the button press and the
     * user backgrounded the app mid-dialog, DataStore would say *running* with no service alive, and
     * that flag is exactly what Phase 2's reboot restore reads back.
     */
    fun beginDimming() {
        viewModel.setRunning(true)
        context.startShade()
    }

    // Asynchronous, and that changes the call order rather than adding a callback to the end of it —
    // closer to `await` than to a fire-and-forget `.then()`, because everything after the ask depends
    // on its answer. **The shade starts in all three outcomes, refusal included.** Refusing to dim
    // somebody's screen because they refused a notification is the app deciding it knows better; the
    // honest move is to run and to say plainly what they have given up.
    val requestNotifications =
        rememberNotificationPermissionAsk { outcome ->
            dialogSpent = outcome == NotificationPermissionOutcome.PermanentlyDenied
            beginDimming()
            reread()
        }

    /**
     * **The one place the system dialog is raised, and it takes the shade down first.**
     *
     * §1's rule is that the ask is only ever made with no shade on screen, and it is a platform
     * constraint rather than a preference: the system hides or refuses touches on non-system overlay
     * windows while a permission dialog is up, so that an app cannot draw over the dialog and trick
     * somebody into tapping *Allow*. `FLAG_NOT_TOUCHABLE` may or may not exempt ours, and a user who
     * cannot tap the dialog is stuck at a very dark screen — R7 measures which it is, but the rule
     * holds either way, so it is enforced here rather than trusted to the answer.
     *
     * From the start button the shade is already down and `stopShade()` is a no-op. From the
     * warning's *Allow notifications* button it is not, which is the case this exists for: down
     * before the dialog, and back up in the outcome, because [beginDimming] runs in all three.
     *
     * **`shadeRunning` is deliberately not written here.** The stored intent is what the user asked
     * for and the live service is what is on screen — different questions — and clearing the intent
     * for the length of a dialog is what Phase 2's reboot restore would read back as *they turned it
     * off*.
     */
    fun askForNotifications() {
        context.stopShade()
        requestNotifications()
    }

    val warning =
        when {
            // Never asked and spent are indistinguishable from here, and both want the launcher.
            !notificationsOn && (rationaleLeft || !dialogSpent) -> ShadeWarning.AskPermission
            !notificationsOn -> ShadeWarning.OpenAppSettings
            !channelOn -> ShadeWarning.OpenChannelSettings
            else -> ShadeWarning.None
        }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dim_title)) }) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets)) {
            if (!canDraw) {
                PermissionExplainer(
                    onGrant = { requestOverlay.launch(context.shadePermissionIntent()) },
                )
                return@Column
            }

            // Only while the shade is actually up. A first-launch user who has started nothing is
            // not missing an escape hatch, because there is nothing to escape from yet.
            if (state.running && warning != ShadeWarning.None) {
                NotificationWarning(
                    warning = warning,
                    onAct = {
                        when (warning) {
                            ShadeWarning.AskPermission -> askForNotifications()
                            ShadeWarning.OpenAppSettings -> context.openAppNotificationSettings()
                            ShadeWarning.OpenChannelSettings ->
                                context.openChannelNotificationSettings(AppChannel.Shade)
                            ShadeWarning.None -> Unit
                        }
                    },
                )
            }

            // **Kept free of anything but state and callbacks**, deliberately and without being
            // extracted yet. Phase 3a renders these same controls in a second host that is not an
            // Activity, so nothing here may reach for a `Context`, start the service, or assume it
            // sits inside a `Scaffold`'s insets — but pulling out a composable with one caller is
            // guesswork about the second caller's shape, so the extraction waits for it.
            SectionHeader(stringResource(R.string.dim_level_label))
            Text(
                text = stringResource(R.string.dim_level_value, state.dimLevel),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = Spacing.base),
            )
            Slider(
                value = state.dimLevel.toFloat(),
                onValueChange = { viewModel.setDimLevel(it.toInt()) },
                valueRange = 0f..100f,
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
                // Visible and *disabled* rather than hidden when the device cannot honour it: a
                // control that is simply absent leaves the user unable to tell Gloam from a Gloam
                // that behaves differently on their phone than on someone else's.
                Switch(
                    checked = state.lowerBacklight,
                    onCheckedChange = viewModel::setLowerBacklight,
                    enabled = backlightAvailable,
                )
            }
            // Supporting text, always present rather than a one-off dialog. The symptom it explains
            // — a brightness slider that moves and does nothing — recurs every time the shade goes
            // up, and a dismissed dialog is not there when it does. On a device that cannot do it at
            // all the promise is replaced rather than left standing, which would be an explanation
            // for a symptom that is not happening.
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

            Button(
                onClick = {
                    when {
                        state.running -> {
                            viewModel.setRunning(false)
                            context.stopShade()
                        }
                        // Granted, or one denial already spent. Android permits two denials before
                        // the dialog stops appearing, and the second is spent only by a deliberate
                        // tap on a control that says *Allow notifications* — never by somebody who
                        // came here to dim their screen.
                        notificationsOn || rationaleLeft -> beginDimming()
                        else -> askForNotifications()
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.base, vertical = Spacing.section),
            ) {
                Text(stringResource(if (state.running) R.string.dim_stop else R.string.dim_start))
            }
        }
    }
}

/**
 * The shade is up and the notification that stops it cannot appear.
 *
 * Stated as what the user has lost rather than as which permission is missing — "no Stop button
 * outside Gloam" is the consequence; "notifications are denied" is trivia they would have to
 * translate themselves while looking at a very dark screen.
 */
@Composable
private fun NotificationWarning(
    warning: ShadeWarning,
    onAct: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.base, end = Spacing.base, top = Spacing.base),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                text = stringResource(R.string.dim_notification_warning_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    stringResource(
                        if (warning == ShadeWarning.OpenChannelSettings) {
                            R.string.dim_notification_warning_channel_body
                        } else {
                            R.string.dim_notification_warning_body
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.tight),
            )
            TextButton(onClick = onAct, modifier = Modifier.padding(top = Spacing.tight)) {
                Text(
                    stringResource(
                        if (warning == ShadeWarning.AskPermission) {
                            R.string.dim_notification_warning_ask
                        } else {
                            R.string.dim_notification_warning_settings
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PermissionExplainer(onGrant: () -> Unit) {
    Column(modifier = Modifier.padding(Spacing.base)) {
        Text(
            text = stringResource(R.string.dim_permission_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.dim_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.tight),
        )
        Button(onClick = onGrant, modifier = Modifier.padding(top = Spacing.base)) {
            Text(stringResource(R.string.dim_permission_grant))
        }
    }
}
