package app.gloam.work

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Xiaomi's autostart manager. The activity that decides, on HyperOS, whether this app's background
 * work is allowed to run at all (ADR-0003) — and the one with no readable state anywhere. Named
 * explicitly in the manifest's `<queries>` so it can be looked up at all.
 *
 * **This file was split out of `BatteryExemption.kt`**, which now holds only the battery-optimisation
 * half. Reboot restore made the autostart hand-off live while that half stays Phase 4's, and a file
 * named for the dead half is exactly the ambiguity a reader cannot resolve — an unused defence and a
 * working one look identical from the outside. [startActivitySafely] lives here rather than there
 * because both halves need it.
 */
private const val MIUI_SECURITY_CENTER = "com.miui.securitycenter"
private const val MIUI_AUTOSTART_ACTIVITY = "com.miui.permcenter.autostart.AutoStartManagementActivity"

/**
 * Whether this phone has Xiaomi's autostart screen at all.
 *
 * Answerable only because the manifest declares the package in `<queries>`; without that,
 * package-visibility filtering on API 30+ returns null for an activity that would have launched
 * perfectly well. It is a **live read**, which is what lets Settings' *After a restart* row be absent
 * on a phone with no such screen rather than being a dead button on every other vendor's device.
 */
fun Context.hasAutostartSettings(): Boolean = autostartIntent().resolveActivity(packageManager) != null

/**
 * Opens Xiaomi's autostart manager, and that is the end of the app's involvement.
 *
 * **Offered once and claimed never** (ADR-0003's amendment). Launching this returns no result and
 * the setting has no public state, so the app cannot know afterwards what the user did — and a
 * checkbox asking them to confirm would have the app repeating the user's guess back to them as its
 * own assurance, which is this ADR's central hazard sourced from a new place.
 *
 * There is no in-app re-read either, and there never will be: `AUTO_START` is not in `cmd appops`'
 * vocabulary, and the OEM screen's own `checked` attribute reads false on granted rows. The state is
 * recoverable only host-side, by `scripts/device-gate.py` scraping that screen over `adb`. What
 * stands behind the claim that the grant matters is `phase-2.md`'s R1 and R4 — two real reboots with
 * autostart granted, after which the shade came back.
 */
fun Context.openAutostartSettings(): Boolean =
    startActivitySafely(autostartIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

private fun autostartIntent(): Intent =
    Intent().setComponent(ComponentName(MIUI_SECURITY_CENTER, MIUI_AUTOSTART_ACTIVITY))

/**
 * `startActivity`, reporting failure rather than taking the app down with it.
 *
 * An OEM screen that is present but not exported to us throws `SecurityException`, which is the same
 * outcome from here as it not being there at all.
 *
 * `internal` rather than private because three hand-offs across two packages launch something they
 * cannot be sure exists — this file's autostart screen, [openBatteryOptimisationSettings]'s fallback
 * list, and `ui/support/SupportHandoff.kt`'s `mailto:`. Three copies of a two-catch try/catch is how
 * one of them quietly stops catching something.
 */
internal fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
