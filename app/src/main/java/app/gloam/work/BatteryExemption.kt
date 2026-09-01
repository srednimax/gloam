package app.gloam.work

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

/**
 * Whether Android currently exempts this app from battery optimisation.
 *
 * **Nothing calls this file yet — it is Phase 4's**, where scheduled-on needs the exemption to start
 * a foreground service from the background. That is uncalled on purpose rather than by neglect, and
 * it is the whole reason the autostart hand-off moved to `Autostart.kt`: while the two lived in one
 * file named for this half, there was no way to tell the live code from the waiting code.
 *
 * **Readable without any permission**, which is the whole reason the honest state can lean on it.
 * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — the permission that would let the app pop its own
 * grant dialog — is deliberately **not declared**: Play restricts it to apps whose core function is
 * the exemption itself, and `scripts/aab-permissions.py` asserts it absent from the built artifact.
 * So the app reads the state, explains it, and opens the screen where the user decides.
 *
 * Spelled the British way like the rest of this codebase; the platform's own American spelling stays
 * where the platform put it. Pick one and be consistent — a codebase with both is a codebase where
 * every call site is a guess.
 */
fun Context.isIgnoringBatteryOptimisations(): Boolean {
    val power = getSystemService(PowerManager::class.java) ?: return false
    return power.isIgnoringBatteryOptimizations(packageName)
}

/**
 * Opens Android's battery-optimisation list, where the user can exempt this app themselves.
 *
 * The app-specific action (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) is the one that needs the
 * restricted permission, so this is its unrestricted sibling: the whole list, with the user one
 * search away from us. Best-effort in the same shape as [openAutostartSettings], and for the same
 * reason — a dead button costs the user more than one extra tap does.
 *
 * @return whether anything opened, so the caller can say so rather than guess.
 */
fun Context.openBatteryOptimisationSettings(): Boolean {
    val actions =
        listOf(
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            // Every device has this, and every Settings app of this era opens on a search field.
            Settings.ACTION_SETTINGS,
        )
    for (action in actions) {
        // NEW_TASK, so Settings opens as its own task rather than being pushed onto this app's —
        // without it the user cannot bring the app forward again while they are in there.
        if (startActivitySafely(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) return true
    }
    return false
}
