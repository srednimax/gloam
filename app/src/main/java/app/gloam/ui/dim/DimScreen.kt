package app.gloam.ui.dim

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.gloam.shade.shadePermissionIntent
import app.gloam.shade.startShade
import app.gloam.shade.stopShade
import app.gloam.theme.Spacing
import app.gloam.ui.appViewModelExtras
import app.gloam.ui.common.SectionHeader

/**
 * The one screen the app is about: a slider and a switch for the shade.
 *
 * The screen owns starting and stopping the service rather than the `ViewModel` doing it — a
 * `ViewModel` has no `Context`, and handing it one is how it starts outliving its own scope.
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

    // Re-read on every resume rather than remembering the answer. `SYSTEM_ALERT_WINDOW` is a
    // special permission the user grants on a settings screen we send them to, and they can revoke
    // it there just as easily while the app is in the background — a cached `true` becomes a
    // service that starts and then cannot add its window.
    var canDraw by remember { mutableStateOf(context.canDrawShade()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) canDraw = context.canDrawShade()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The system reports no result for this screen, so the contract is only how we learn the user
    // came back. The answer itself comes from re-reading, above and here.
    val requestPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canDraw = context.canDrawShade()
        }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dim_title)) }) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets)) {
            if (!canDraw) {
                PermissionExplainer(
                    onGrant = { requestPermission.launch(context.shadePermissionIntent()) },
                )
                return@Column
            }

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

            Button(
                onClick = {
                    val next = !state.running
                    viewModel.setRunning(next)
                    if (next) context.startShade() else context.stopShade()
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
