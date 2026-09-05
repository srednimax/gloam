package app.gloam.shade

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.gloam.work.isIgnoringBatteryOptimisations

/**
 * `adb logcat -s GloamGate:*` is the whole of what checkpoint A reads, and it is deliberately not
 * `GloamSchedule` — the real receiver takes that tag in checkpoint C, and a gate reading found in a
 * log beside the feature it was meant to decide is a reading nobody can date.
 */
private const val TAG = "GloamGate"

/**
 * The instant the alarm was armed for, carried on the intent so the receiver can report its own
 * lateness rather than a developer subtracting two clock readings by hand.
 */
private const val EXTRA_TARGET = "app.gloam.debug.gate.TARGET"

/**
 * Checkpoint A's apparatus: **a bare alarm and a bare receiver, and nothing else**.
 *
 * ## What it is measuring
 *
 * Three things have to hold before scheduled-on is buildable on this phone, and they fail in
 * different places (`docs/phase-4.md` section 1):
 *
 * 1. **The alarm fires while the device is in Doze.** `setAndAllowWhileIdle` is the documented
 *    Doze-capable inexact alarm, rate-limited by the platform to roughly one fire per nine minutes
 *    per app while idle. That limit is the honest ceiling on the feature's promise — *within a few
 *    minutes of 22:00*, never *at* 22:00 — so what this reads is the **lateness**, not the fact.
 * 2. **The `startForegroundService` is allowed.** Starting a foreground service from a background
 *    broadcast is refused by default with `ForegroundServiceStartNotAllowedException`; the exemption
 *    being leaned on is the power allowlist, which is what a user-granted battery-optimisation
 *    exemption puts the app on. The `runCatching` below is there to *name* that refusal, because at
 *    22:00 on a stranger's phone it is invisible.
 * 3. **HyperOS starts the process at all.** An alarm's `PendingIntent` is a broadcast, and ADR-0003
 *    records that without autostart this ROM does not start the process for one. Documentation says
 *    nothing about this; only the phone can.
 *
 * **Nothing here reads a preference, and nothing writes one.** No schedule, no window arithmetic, no
 * decision logic — because the question is about the platform and the ROM and nothing else, and
 * every line written before the answer would be a line written on a bet.
 *
 * **Careful: this measures `…gloam.debug`, not the release id.** The power allowlist and HyperOS's
 * autostart list are both per-package, so the grants have to be given to the debug package and the
 * verdict transfers on that basis. A night spent against grants sitting on the release id reads
 * exactly like a ROM that refused.
 */
class GateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val now = System.currentTimeMillis()
        val target = intent.getLongExtra(EXTRA_TARGET, 0L)
        val lateness = if (target == 0L) "unknown" else "${now - target}ms"

        // The app's own read of the allowlist, beside the state a developer set from the host.
        // Section 7's whole banner rests on those two agreeing on this ROM, and nothing has ever
        // checked it.
        val app = context.applicationContext
        Log.i(TAG, "fired at $now, $lateness late, exempt=${app.isIgnoringBatteryOptimisations()}")

        // The real route, not an approximation of it: `startShade()` is what the schedule's own
        // receiver will call. `runCatching` because the refusal this is looking for is an exception,
        // and a debug apparatus that crashes the process loses the log line that was the point.
        val outcome = runCatching { app.startShade() }
        val threw = outcome.exceptionOrNull()
        Log.i(
            TAG,
            "startForegroundService: " +
                if (threw == null) "allowed" else "${threw.javaClass.simpleName}: ${threw.message}",
        )
    }
}

/**
 * Arm the gate's alarm [millisOut] from now, and record what the app believes about the exemption at
 * the moment it is armed.
 *
 * **`FLAG_UPDATE_CURRENT` is load-bearing here rather than idiomatic.** A `PendingIntent`'s identity
 * is its request code and component — the extras are *not* part of it — so without the flag a second
 * arming would reuse the first one's [EXTRA_TARGET] and every lateness after the first would be
 * nonsense. `FLAG_IMMUTABLE` is required from API 31 and correct anyway.
 *
 * **`RTC_WAKEUP` rather than `RTC`**, for the reason the real alarm will use it: a non-waking alarm
 * fires when the device next wakes, and turning the screen on *is* a device wake — so the race would
 * be between the broadcast and the first frame the user sees, and losing it is a bright flash
 * followed by the shade, on a phone picked up in a dark room.
 */
fun Context.armGateAlarm(millisOut: Long) {
    val target = System.currentTimeMillis() + millisOut
    val alarms = getSystemService(AlarmManager::class.java) ?: return
    val pending =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GateReceiver::class.java).putExtra(EXTRA_TARGET, target),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pending)
    Log.i(
        TAG,
        "armed for $target (${millisOut}ms out), exempt=${isIgnoringBatteryOptimisations()}",
    )
}
