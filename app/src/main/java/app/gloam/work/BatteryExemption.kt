package app.gloam.work

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Whether Android currently exempts this app from battery optimisation.
 *
 * **Phase 4 is what calls it.** It sat here from Phase 2 with nothing reading it, waiting for the
 * feature that needs the exemption — scheduled-on, which starts a foreground service from the
 * background — because `PLAN.md` rule 4 makes every ask wait for the moment it is switched on. That
 * wait is also why the autostart hand-off moved to `Autostart.kt`: while the two lived in one file
 * named for this half, there was no way to tell the live code from the waiting code.
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
 * restricted permission, so this is its unrestricted sibling. Best-effort in the same shape as
 * [openAutostartSettings], and for the same reason — a dead button costs the user more than one
 * extra tap does.
 *
 * **Three destinations, and the order is a reading rather than a preference** (`docs/phase-4.md`
 * section 7). R1 measured that the switch which governs the schedule is Android's own power
 * allowlist: with it off the alarm still fires and `startForegroundService` throws, and
 * [isIgnoringBatteryOptimisations] agreed with the state set from the host. So the allowlist is what
 * the copy is pointing at and it goes first. Gloam's own **App info** page is second rather than
 * last, because it is one tap from battery on both AOSP and HyperOS and needs no permission — where
 * Settings' front door leaves the user in a haystack after copy that named a specific control, which
 * is a hand-off owned only in the sense that a button exists.
 *
 * @return whether anything opened, so the caller can say so rather than guess.
 */
fun Context.openBatteryOptimisationSettings(): Boolean {
    val intents =
        listOf(
            // The whole list, with the user one search away from us.
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            // This app's own page, where battery is a row rather than a search result.
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()),
            // Every device has this, and every Settings app of this era opens on a search field.
            Intent(Settings.ACTION_SETTINGS),
        )
    for (intent in intents) {
        // NEW_TASK, so Settings opens as its own task rather than being pushed onto this app's —
        // without it the user cannot bring the app forward again while they are in there.
        if (startActivitySafely(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) return true
    }
    return false
}
