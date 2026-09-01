package app.gloam.shade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Whether this app may draw the shade over other apps right now.
 *
 * `SYSTEM_ALERT_WINDOW` is a **special** permission, not a runtime one. There is no dialog the app
 * can raise and no `requestPermissions` call that covers it: the only path is sending the user to a
 * system settings screen and reading the answer when they come back. That shape is why this is two
 * functions rather than one — an ask and a read — and why the read has to happen again on every
 * resume rather than being remembered.
 *
 * **Read fresh at every use, never cached.** The user can revoke it from system settings while the
 * app is in the background, and a cached `true` becomes a service that starts and then throws when
 * it tries to add its window.
 *
 * Below API 23 the permission was granted at install time. `minSdk` is 26, so that branch does not
 * exist here and the answer is always the live one.
 */
fun Context.canDrawShade(): Boolean = Settings.canDrawOverlays(this)

/**
 * The settings screen where the user grants it, scoped to this app.
 *
 * `package:` in the Uri asks for Gloam's own row rather than the full list of every installed app.
 * **HyperOS ignores it** — measured while taking Phase 2's restore readings, where this landed on the
 * whole *Display over other apps* list and the user has to find themselves in it alphabetically. The
 * Uri stays because it is the documented behaviour and it is honoured elsewhere; what it must not do
 * is make the copy that sends people here promise a screen with one switch on it.
 *
 * Returning the Intent rather than launching it keeps this callable from a composable that owns an
 * activity-result launcher — the result is how the caller knows to re-read [canDrawShade], since the
 * system reports no result of its own.
 */
fun Context.shadePermissionIntent(): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", packageName, null),
    )
