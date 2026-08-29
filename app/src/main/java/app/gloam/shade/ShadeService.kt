package app.gloam.shade

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import app.gloam.MainActivity
import app.gloam.MainApplication
import app.gloam.R
import app.gloam.work.AppChannel
import app.gloam.work.ensureNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** The notification's Stop action, delivered straight to the service so it needs no Activity. */
private const val ACTION_STOP = "app.gloam.shade.STOP"

private const val NOTIFICATION_ID = 1

/**
 * The foreground service that owns the shade — one window, added above everything else.
 *
 * ## Why a service at all
 *
 * The shade has to outlive the app's own screen: the entire point is to dim *other* apps, so the
 * moment the user leaves Gloam the process must still be alive and still own a window. An Activity
 * cannot do that. A **foreground** service is the only shape Android offers that survives the user
 * navigating away, and the price it charges is a permanently visible notification — which turns out
 * to be the feature, not the tax. Someone who dims to 95% and loses the app still has the Stop
 * action, and Android will not let that notification be swiped away.
 *
 * ## What the window actually is
 *
 * A plain black `View` at a chosen alpha, in a window of type `TYPE_APPLICATION_OVERLAY`, with the
 * flags that make it **incapable of being interacted with**: `FLAG_NOT_TOUCHABLE` means every touch
 * passes through to whatever is underneath, and `FLAG_NOT_FOCUSABLE` means it never takes keyboard
 * focus. Without both, the shade would swallow the user's taps and the phone would appear frozen —
 * the single worst failure this app could ship, because the way out of it is also underneath the
 * shade.
 *
 * `FLAG_LAYOUT_IN_SCREEN` and `FLAG_LAYOUT_NO_LIMITS` together with `MATCH_PARENT` are what carry it
 * over the status and navigation bars. Without them the shade stops at the app area and the two
 * system bars stay at full brightness, which reads as a bug rather than as a design.
 *
 * **It does not touch the backlight.** Lowering the system brightness to its floor first is a real
 * improvement and a separate decision (CONTEXT.md keeps `backlight` and `dim level` apart on
 * purpose); this service subtracts light only by drawing over it.
 *
 * ## Kotlin/Android notes
 *
 * A `Service` is not a thread — every callback here runs on the main thread, which is why the
 * preference `Flow` is collected on a scope this class owns and cancels rather than blocking in
 * `onStartCommand`. `START_STICKY` asks Android to recreate the service if it is killed for memory;
 * on Xiaomi that promise is worth less than the stored `shadeRunning` preference, which is why the
 * intent to be dimming lives in DataStore rather than in this object.
 */
class ShadeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var shadeView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()

        windowManager = getSystemService(WindowManager::class.java)
        addShadeWindow()

        // The one source of truth for how dark it is. Collected rather than passed in the Intent so
        // that dragging the slider updates the live window without restarting anything.
        val preferences = (application as MainApplication).preferences
        preferences.dimLevel
            .distinctUntilChanged()
            .onEach { level -> applyDimLevel(level) }
            .launchIn(scope)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            // Record that the user turned it off *before* stopping. Without this the stored intent
            // still says "running", and the next launch — or a system restart of a START_STICKY
            // service — would helpfully put the shade back over a user who just dismissed it.
            scope.launch {
                (application as MainApplication).preferences.setShadeRunning(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeShadeWindow()
        super.onDestroy()
    }

    private fun addShadeWindow() {
        if (shadeView != null) return
        // Guarded because the permission can be revoked while the service is alive, and `addView`
        // throws rather than returning a failure. A shade that fails to appear is a bug; a crash
        // over it is a worse one.
        if (!canDrawShade()) return

        val view = View(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching { windowManager?.addView(view, params) }
            .onSuccess { shadeView = view }
    }

    private fun removeShadeWindow() {
        val view = shadeView ?: return
        runCatching { windowManager?.removeView(view) }
        shadeView = null
    }

    /**
     * 0 is invisible, 100 is as dark as this app is willing to go.
     *
     * **Capped below fully opaque, deliberately.** At alpha 1.0 the screen is a black rectangle and
     * every way out of it — the notification shade, the app, the Stop action — is behind it. The cap
     * is the difference between a very dark screen and a phone the user believes is broken.
     */
    private fun applyDimLevel(level: Int) {
        shadeView?.alpha = level.coerceIn(0, 100) / 100f * MAX_SHADE_ALPHA
    }

    private fun buildNotification(): Notification {
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stop =
            PendingIntent.getService(
                this,
                1,
                Intent(this, ShadeService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, AppChannel.Shade.id)
            .setContentTitle(getString(R.string.shade_notification_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .addAction(0, getString(R.string.shade_notification_stop), stop)
            // Not dismissible: this notification is the way back out of a very dark screen.
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        /**
         * The darkest the shade is allowed to be.
         *
         * 0.95 rather than 1.0 — see [applyDimLevel]. It is a constant rather than a preference on
         * purpose: it is a safety floor, not a taste.
         */
        const val MAX_SHADE_ALPHA = 0.95f
    }
}

/**
 * Start the shade.
 *
 * `startForegroundService` unconditionally: `minSdk` is 26, so the pre-Oreo `startService` path is
 * unreachable here. It carries a hard contract — the service **must** call `startForeground` within
 * a few seconds or Android kills the process with an ANR — which `onStartCommand` does on its first
 * statement for exactly that reason.
 */
fun Context.startShade() {
    startForegroundService(Intent(this, ShadeService::class.java))
}

/** Stop the shade and take the window down. */
fun Context.stopShade() {
    stopService(Intent(this, ShadeService::class.java))
}
