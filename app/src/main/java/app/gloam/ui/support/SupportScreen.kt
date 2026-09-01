package app.gloam.ui.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.gloam.R
import app.gloam.theme.Spacing
import app.gloam.ui.common.DetailScaffold

/**
 * The route out of the app, for a tester who has found something.
 *
 * It exists at the door rather than in the polish phase for one reason: **a tester with no way to
 * report is a tester whose fourteen days produce nothing.** The rate-on-Play link and the tip are
 * Phase 5's, where there is a listing to rate and an audience to tip.
 *
 * A detail screen pushed from Settings' *About* section, beside the licences row — not a third tab.
 * The bottom bar switches between roots, and support is not a root; it is somewhere you go once,
 * from the place you already go to look things up.
 *
 * No `ViewModel`, because there is no state to hold: two rows, each of which builds an intent from
 * constants and hands it to the system. The one piece of state is whether the last hand-off found a
 * mail app, which is about this composition and nothing else.
 */
@Composable
fun SupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Not `rememberSaveable`: this says "the tap you just made went nowhere", which is only true of
    // the tap you just made. A rotation clears it, and the next tap says it again if it is still so.
    var noMailApp by remember { mutableStateOf(false) }

    DetailScaffold(
        title = stringResource(R.string.support_title),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier =
                contentModifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            for (request in SupportRequest.entries) {
                SupportRow(
                    title = stringResource(request.titleRes()),
                    hint = stringResource(request.hintRes()),
                    // Inverted rather than assigned from the return value directly, so a second
                    // hand-off that works clears a message the first one left behind.
                    onClick = { noMailApp = !context.sendSupportMail(request) },
                )
            }

            // Inline rather than a snackbar: the app has no snackbar host anywhere, and the failure
            // is about the row that was just tapped rather than about the screen. Stated as what is
            // missing on the phone — the app is not broken and neither is the address.
            if (noMailApp) {
                Text(
                    text = stringResource(R.string.support_no_mail_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.snug),
                )
            }
        }
    }
}

/**
 * A tappable row: what it does, and what will happen if you tap it.
 *
 * The hint is not decoration. Both of these rows leave Gloam for another app, and a row that opens
 * somebody's mail composer with no warning is the kind of thing a user backs out of and never taps
 * again.
 */
@Composable
private fun SupportRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.base, vertical = Spacing.snug),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Kotlin note: extensions on the enum rather than two more fields in it, the same split
 * `AutoOff.labelRes()` makes. [SupportRequest] carries what the *mail* is made of and has no Android
 * UI in it; what the row is labelled is this screen's business.
 */
private fun SupportRequest.titleRes(): Int =
    when (this) {
        SupportRequest.Bug -> R.string.support_report
        SupportRequest.Feature -> R.string.support_feature
    }

private fun SupportRequest.hintRes(): Int =
    when (this) {
        SupportRequest.Bug -> R.string.support_report_hint
        SupportRequest.Feature -> R.string.support_feature_hint
    }
