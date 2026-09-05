package app.gloam.shade

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.gloam.MainApplication
import app.gloam.work.armScheduleAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * `adb logcat -s GloamSchedule:*` is how checkpoint E reads which branch below was taken, and
 * `work/ScheduleAlarm.kt` logs under the same tag on purpose — an arm and the fire it produced are
 * two halves of one story. Not `BootReceiver`'s tag, and not the gate's: `GloamGate` is checkpoint
 * A's and stays separate, so a gate reading is never found in a log beside the feature it decided.
 */
private const val TAG = "GloamSchedule"

/**
 * Raises the shade when the nightly window opens, and re-arms the next hop whatever it decides.
 *
 * ## Why this is allowed to start a foreground service
 *
 * It is not one of the three broadcasts that are documented exemptions from the background start
 * ban — `BootReceiver` has those. What licenses this one is the **power allowlist**: a user-granted
 * battery-optimisation exemption, which section 7 asks for and which checkpoint A proved is
 * load-bearing rather than defensive. R1 read the failure exactly — without the exemption the alarm
 * still fires, the process runs, this receiver's first log line lands, and then
 * `startForegroundService` throws `ForegroundServiceStartNotAllowedException: … mAllowStartForeground
 * false`. The allowlist buys the *service start*, not the alarm.
 *
 * **And on this ROM there is a second gate above that one.** R3 read autostart denied with the
 * exemption granted: the alarm did not fire at all in fifteen minutes and was still armed in
 * `dumpsys alarm` at the end. Armed and run are ADR-0003's two different things, and neither the app
 * nor any documentation can see that switch — only `scripts/device-gate.py` and the phone.
 *
 * ## What it refuses to do, which is most of it
 *
 * Three refusals, and every one is a real state rather than defensive noise:
 *
 * - **The schedule is off.** An alarm survives the preference that armed it being switched off —
 *   section 6 cancels, but a cancel racing a fire is a real ordering and cheap to make harmless.
 * - **The alarm arrived after the window closed.** The one refusal `BootReceiver` does not have, and
 *   the direct consequence of an inexact alarm: a 22:00-to-22:05 window with an alarm delivered at
 *   22:07 must not open a window that has already ended. Refusing is also what makes a hop that
 *   arrives late harmless without any special handling — the window is either still open or it is
 *   not.
 * - **No overlay permission**, refused exactly as `BootReceiver` refuses it: starting anyway would
 *   post a *Screen dimmed* notification over an undimmed screen, and quiet and wrong is worse than
 *   not starting. `shade_running` is left alone on that refusal for `BootReceiver`'s reason — it is
 *   the user's intent, they did not change it, and an app that clears it for them is an app where a
 *   revoked permission silently becomes a lost setting.
 *
 * **The notification permission is deliberately not a fourth refusal**, same as `BootReceiver` and
 * for the same reason: refusing to dim somebody's screen because they refused a notification is the
 * app deciding it knows better. The honest cost is written down rather than guarded — the hatches
 * left are the power menu and the app's own controls at about 1.59 nits (`phase-3.md` R3) — and the
 * window's own end bounds the whole episode, which is more than a hand-started shade gets.
 *
 * **The shade already being up is not a refusal.** The branch runs, `beginShadeAt(BySchedule)`
 * rewrites the deadline to the window's end, and `startShade()` on a live service is a no-op
 * `onStartCommand` that re-enters `START_STICKY`. That is the whole of how a hand-started early
 * shade gets adopted by the schedule.
 *
 * ## Kotlin/Android notes
 *
 * The [goAsync] token and the scope created on the spot are `BootReceiver`'s pattern exactly, and
 * its file has the long version of why: [onReceive] runs on the main thread, the process is
 * disposable the moment it returns, and there is no `await` to reach for. `finally` is what
 * guarantees the token is always spent.
 *
 * **No action check here, unlike `BootReceiver`.** That one is `exported="true"` because the system
 * delivers its broadcasts, so it takes the action it was handed rather than trusting the filter.
 * This receiver is `exported="false"` with no filter at all: the only thing that can reach it is
 * this app's own `PendingIntent`, held by `AlarmManager` on our behalf.
 */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // `applicationContext` rather than the restricted `Context` a manifest receiver is handed —
        // that one refuses `bindService` and `registerReceiver`, and reaching past it here means
        // nothing added later quietly starts to need them.
        val app = context.applicationContext as? MainApplication ?: return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.onScheduledOn()
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * The decision, with the read that informs it — kept out of [ScheduleReceiver.onReceive] so that
 * what it does is one `when` rather than a callback with a body. `BootReceiver.restoreShade`'s
 * shape, deliberately.
 */
private suspend fun MainApplication.onScheduledOn() {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val schedule = preferences.schedule.first()

    // **The window's opening instant is the check and the value at once.** `windowStart` answers
    // null exactly when `contains` is false, and the success branch needs the instant itself to
    // write the marker — so binding it here is one derivation rather than two that could disagree,
    // and Kotlin's smart cast makes it a non-null `Long` inside the `else`.
    val windowStart = schedule.windowStart(now, zone)

    when {
        !schedule.enabled -> Log.i(TAG, "the schedule is off; a stale alarm, nothing to do")

        windowStart == null ->
            Log.i(TAG, "fired at $now, outside the window; too late to open it")

        !canDrawShade() -> Log.i(TAG, "no overlay permission, leaving the stored intent alone")

        else -> {
            preferences.beginShadeAt(ShadeStart.BySchedule, now, zone)
            preferences.setScheduleHonouredAt(windowStart) // this night, done
            startShade()
            Log.i(TAG, "scheduled shade up until ${schedule.windowEnd(now, zone)}")
        }
    }

    // **Outside the `when`, and that placement is load-bearing.** Every branch above, refusals
    // included, must leave the next hop armed: a receiver that re-armed only on the success path
    // would switch the schedule off permanently the first time the user happened to revoke the
    // overlay permission, and it would do it silently.
    armScheduleAlarm(schedule, now)
}
