package app.gloam.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.net.toUri
import app.gloam.BuildConfig
import app.gloam.R
import app.gloam.work.startActivitySafely

/**
 * Where a report goes, and it is **frozen the day a build leaves this machine**.
 *
 * A dedicated account rather than a `…+gloam@` alias on a personal one, because a sideloaded APK is
 * never forced to update: whatever string is compiled in here keeps being mailed for as long as any
 * build survives. An alias is welded to one mailbox forever; a real address can be forwarded
 * anywhere, and handed to another maintainer without handing over your own mail.
 *
 * The Play listing's contact field is the same address — two routes is one more than anybody needs.
 */
private const val SUPPORT_ADDRESS = "gloam.dimmer@gmail.com"

/**
 * The two things a user can send, as a lookup table.
 *
 * An enum rather than two `@StringRes` parameters on [sendSupportMail], because the subject and the
 * prompt are a **pair**: a bug subject over a feature prompt is a mail that sorts into the wrong
 * folder and reads as a mistake, and separate parameters are exactly the shape that lets it happen.
 * Same idiom as `AppChannel` and `AutoOff` — an enum carrying its own constants instead of a `when`
 * mapping two of them together at the call site.
 *
 * **The subject is `translatable="false"` and the prompt is not.** The subject carries a filter tag
 * the developer sorts on and the user never reads; the prompt is a sentence addressed to them. A
 * Polish bug report arriving under `Gloam #bug` is the correct outcome of an app with a Polish
 * locale, not a bug in this pairing.
 */
enum class SupportRequest(
    @param:StringRes val subject: Int,
    @param:StringRes val prompt: Int,
) {
    Bug(R.string.support_subject_bug, R.string.support_body_bug),
    Feature(R.string.support_subject_feature, R.string.support_body_feature),
}

/**
 * Hand the user off to their own mail app with a report half-written.
 *
 * **The subject and body travel in the `mailto:` query string, not in `EXTRA_SUBJECT`.** That is a
 * device finding rather than a design: Gmail silently ignores `EXTRA_SUBJECT` for `ACTION_SENDTO`,
 * so nothing about the intent fails — the composer just opens with an empty subject, which is the
 * sort of thing found by a user rather than by a build.
 *
 * `Uri.encode` per component, then the pieces concatenated: a `mailto:` URI is **opaque** (there is
 * no `//authority`), so `Uri.Builder` would percent-encode the `?` and `&` that separate the fields
 * and produce one long unparseable opaque part. Encoding each value and joining them by hand is the
 * only shape that survives. `Uri.encode` also writes a space as `%20` rather than `+`, which matters
 * here — RFC 6068 does not read `+` as a space, so a `+`-encoded body arrives full of plus signs.
 *
 * **No `resolveActivity` pre-check.** The manifest's `<queries>` declares the `mailto:` intent so one
 * added later would answer honestly, but the launch itself is already the check — and a pre-check
 * that disagreed with the launch would be a row disabled on a phone that can mail perfectly well.
 *
 * @return whether a mail app opened, so the caller can say so rather than leave a tap doing nothing.
 */
fun Context.sendSupportMail(request: SupportRequest): Boolean {
    val uri =
        (
            "mailto:$SUPPORT_ADDRESS" +
                "?subject=" + Uri.encode(getString(request.subject)) +
                "&body=" + Uri.encode(supportMailBody(getString(request.prompt)))
        ).toUri()
    return startActivitySafely(Intent(Intent.ACTION_SENDTO, uri))
}

/**
 * The prompt, room to write, and the six facts a bug report always needs and a reporter never has to
 * hand.
 *
 * Composed from [BuildConfig] and [Build] — nothing read here that the app does not already know
 * about itself, and nothing that needs a permission. It is not a privacy question in the ordinary
 * sense either: Gloam declares no `INTERNET` permission and has no route off the device, so this is
 * the *user's own* mail, composed in their client, visible to them before they send it and deletable
 * line by line.
 *
 * `app_name` rather than a literal, because the debug source set overrides it — a report from a
 * sideloaded debug build says *Gloam debug* and is told apart from a Play one at a glance.
 *
 * The version block is deliberately **not** in `strings.xml`: it is data rather than prose, there is
 * nothing in it a translator could improve, and `translator-brief.md` §4 keeps exactly this kind of
 * string out of their hands.
 */
private fun Context.supportMailBody(prompt: String): String =
    buildString {
        appendLine(prompt)
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        append("${Build.MANUFACTURER} ${Build.MODEL}")
    }
