package app.gloam.shade

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import app.gloam.MainActivity
import app.gloam.MainApplication
import app.gloam.R
import app.gloam.data.AppPreferences
import app.gloam.work.AppChannel
import app.gloam.work.ensureNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/** The notification's Stop action, delivered straight to the service so it needs no Activity. */
private const val ACTION_STOP = "app.gloam.shade.STOP"

/**
 * Summon the panel, delivered to the service for the same reason [ACTION_STOP] is — and for one
 * more: the panel is a window this service owns, so there is no Activity in the route at all. A tap
 * that would otherwise be a task switch to a screen under the shade becomes a card drawn above it.
 */
private const val ACTION_SHOW_PANEL = "app.gloam.shade.SHOW_PANEL"

private const val NOTIFICATION_ID = 1

/** Logcat's own name for this class, so `adb logcat -s ShadeService:*` finds the auto-off line. */
private const val TAG = "ShadeService"

/**
 * How long the deadline loop is ever allowed to sleep in one go.
 *
 * **A platform fact rather than a style.** `delay` on the main dispatcher is a `Handler.postDelayed`,
 * scheduled against `SystemClock.uptimeMillis` — a clock that **stops advancing in deep sleep**. One
 * two-hour `delay` on a phone that slept for ninety minutes of it fires ninety minutes late. Waking
 * to re-read the wall clock at most a minute at a time bounds the error to a minute *of CPU-awake
 * time*, which is the most any mechanism without an alarm can promise — and it costs nothing while
 * the phone is asleep, because `postDelayed` sets no alarm and holds no wakelock. The message is
 * simply overdue when the device next wakes for some other reason, and the loop notices immediately.
 */
private const val DEADLINE_RECHECK_MS = 60_000L

/**
 * The flags the shade's window is added with, named rather than written inline so that a test can
 * hold them to account.
 *
 * **Two of these four are the property `CLAUDE.md` calls load-bearing.** `FLAG_NOT_TOUCHABLE` makes
 * every touch pass through to whatever is underneath and `FLAG_NOT_FOCUSABLE` keeps key events with
 * the app the user is actually in; a window over every other app that lost either one is a phone
 * that appears frozen, with the way out behind the thing freezing it. The other two are layout —
 * they put the window over the system bars instead of inside the content area — and nothing about
 * safety rests on them.
 *
 * **`const` is what makes the JVM half of the test possible.** A `const val` is inlined into its
 * callers at compile time, so `ShadeWindowFlagsTest` reads a number rather than calling into
 * `android.jar`, where an unmocked method throws. `ShadeWindowTest` covers the half a constant
 * cannot: that the window really arrives at the window manager carrying them.
 */
const val SHADE_WINDOW_FLAGS =
    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

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
 * A `FrameLayout` with two children — black at the dim level, amber at the warmth — in a window of
 * type `TYPE_APPLICATION_OVERLAY`, with the flags that make it **incapable of being interacted
 * with**: `FLAG_NOT_TOUCHABLE` means every touch passes through to whatever is underneath, and
 * `FLAG_NOT_FOCUSABLE` means it never takes keyboard focus. Without both, the shade would swallow
 * the user's taps and the phone would appear frozen — the single worst failure this app could ship,
 * because the way out of it is also underneath the shade.
 *
 * **Two children, still one window.** Every safety flag above, the window type, the cutout mode and
 * the foreground notification are attributes of the *window*, so a second layer costs nothing and
 * changes none of them — which is why the warmth layer lands here rather than in a second window
 * later. What it does change is where the safety cap lives: black at `MAX_SHADE_ALPHA` under amber
 * at `MAX_WARMTH_ALPHA` leaves half of what the black child alone was ever allowed to leave, with
 * neither child past its own limit, so the bound belongs to the composite. [shadeValuesFor] is
 * where that is enforced, and it is proven on the JVM rather than by looking at a screen.
 *
 * `FLAG_LAYOUT_IN_SCREEN` and `FLAG_LAYOUT_NO_LIMITS` together with `MATCH_PARENT` are what carry it
 * over the status and navigation bars. Without them the shade stops at the app area and the two
 * system bars stay at full brightness, which reads as a bug rather than as a design.
 *
 * ## The backlight the window carries
 *
 * The same window also carries a **brightness override** — `LayoutParams.screenBrightness` — which
 * is why the backlight half needs no permission at all: it is an attribute of a window this app
 * already owns, not a write to `Settings.System` (ADR-0010 rejected `WRITE_SETTINGS` for exactly
 * that reason). Android applies it while the window is on screen and hands the panel back the
 * instant it is removed, **including when the ROM kills the process**, which is the property that
 * makes it safe to reach for: there is no state left behind to strand a user with.
 *
 * The price is stated in the UI rather than hidden: while the override is live the user's own
 * brightness slider moves and does nothing, and whatever they set lands the moment the shade comes
 * off. `dim_backlight_hint` says so, always visible, because the symptom recurs and a dismissed
 * dialog is not there when it does.
 *
 * ## The deadline it takes itself down on
 *
 * Auto-off is a job on this service's own scope and needs no permission at all, which is the whole
 * difference between taking something down and putting something up: taking it down only has to be
 * right the next time the user looks at the screen. [awaitDeadline] is the loop, and the deadline it
 * watches is an absolute instant in DataStore rather than a countdown in this object — so a ROM kill
 * and a `START_STICKY` restart resume the right deadline instead of silently restarting the clock.
 *
 * **Nothing here covers the kill that does *not* restart.** The window dies with the process, the
 * stored intent still says running, and no code of ours is alive to notice the deadline pass. That
 * case belongs to the screen's resume reconcile, not to this class.
 *
 * ## Kotlin/Android notes
 *
 * A `Service` is not a thread — every callback here runs on the main thread, which is why the
 * preference `Flow` is collected on a scope this class owns and cancels rather than blocking in
 * `onStartCommand`. `START_STICKY` asks Android to recreate the service if it is killed for memory;
 * on Xiaomi that promise is worth less than the stored intent, which is why what the user asked for
 * lives in DataStore rather than in this object.
 */
class ShadeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Read through the `Application` rather than held, because a `Service` outlives nothing and
     * owns nothing: `application` is valid from `onCreate` on, and a `get()` accessor means there is
     * no field to be stale.
     */
    private val preferences: AppPreferences
        get() = (application as MainApplication).preferences

    private var windowManager: WindowManager? = null

    /**
     * The last values the preference collector produced, kept so a window added *after* it started
     * can still be painted.
     *
     * The collector is `distinctUntilChanged`, which is what stops an unrelated preference write
     * re-laying-out the window — and also means it will not re-emit for the benefit of a latecomer.
     * [addShadeWindow] no longer runs in `onCreate`, so it is now exactly such a latecomer: without
     * this it would add a window whose two children sit at the alpha 0 they are born with, and the
     * shade would be up and perfectly transparent.
     */
    private var lastSettings: DimSettings? = null

    /** The window's root: the `FrameLayout` that `WindowManager` holds, not either layer. */
    private var shadeView: View? = null

    /** The black child, carrying the **dim level**. */
    private var dimLayer: View? = null

    /** The amber child, drawn above the black one, carrying the **warmth**. */
    private var warmthLayer: View? = null

    /**
     * The very `LayoutParams` the window was added with, kept rather than rebuilt.
     *
     * A window attribute is not a view property: `view.alpha = x` takes effect on its own, but
     * `params.screenBrightness = x` does nothing until [WindowManager.updateViewLayout] is handed
     * these params back. Building a fresh instance each time would mean re-stating every safety flag
     * correctly on every slider drag — one omission and the shade starts swallowing touches.
     */
    private var shadeParams: WindowManager.LayoutParams? = null

    /**
     * The backlight Gloam took over from, **captured once and held**.
     *
     * Not re-read while the override is live, and that is correctness rather than thrift: the stored
     * setting still *moves* underneath us, because the user's own slider writes to it blind, so a
     * re-read mid-override would pick up a value they set without being able to see it. Under
     * adaptive brightness it is worse — the framework keeps re-choosing as the room changes, so a
     * re-read would pick up a value chosen for a room rather than for the user.
     *
     * Null means either *no override is applied* or *this device did not give us a number we trust*.
     * Those two want the same behaviour — the shade-only ramp — so they are the same value here, and
     * re-reading on the next emission is right in both: nothing is live to be disturbed.
     */
    private var backlightTop: Float? = null

    /** The panel's root view while it is up, and the thing [removePanelWindow] takes down. */
    private var panelView: View? = null

    /**
     * The lifecycle and saved-state owners the panel's composition is hanging from.
     *
     * **One per summon, and that is not tidiness.** `DESTROYED` is terminal — `LifecycleRegistry`
     * throws when asked to move up out of it, and a `SavedStateRegistryController` restores once —
     * so a `show()` / `hide()` pair on one long-lived host works exactly once. The panel has three
     * ways to close and one to reopen, which makes the second summon the ordinary case.
     */
    private var panelHost: PanelHost? = null

    /** What the panel draws, updated while it is up by [trackPanel]. */
    private var panelState: MutableStateFlow<PanelState>? = null

    /** The state collectors and the idle timer, cancelled together when the panel comes down. */
    private var panelJob: Job? = null

    /**
     * The panel window's own `LayoutParams`, kept for the same reason [shadeParams] is: a re-measure
     * edits them in place and hands them back to `updateViewLayout`.
     */
    private var panelParams: WindowManager.LayoutParams? = null

    /**
     * Every touch the panel receives, whether or not a control consumed it.
     *
     * **Touches rather than values written**, which is the trigger the panel actually needs: its one
     * reason to exist is the dim level moving over real content, and judging that means setting a
     * value and then *looking*, which writes nothing. A user who drags once and studies the result
     * would lose the panel mid-judgement on a value-written timer. The recovery property survives
     * the wider trigger untouched — a panel drawn off-screen by a layout bug receives no touches
     * either, so it still dies on schedule.
     *
     * `DROP_OLDEST` over a buffer of one: this is a re-arm signal, so the only interesting question
     * is whether a touch happened recently, and `tryEmit` from the main thread must never suspend.
     */
    private val panelTouches =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()

        windowManager = getSystemService(WindowManager::class.java)

        // **The shade is not raised here.** `onCreate` runs for every reason the service starts,
        // and one of those reasons is a summon (`ACTION_SHOW_PANEL`) arriving at a service that is
        // not running — from the debug button, or from a `PendingIntent` that outlived the process
        // it was built in. Raising the shade here meant the summon turned the shade *on* over a
        // user who had turned it off, and left `shadeIntent` saying "stopped" while the screen was
        // dimmed: a button reading *Start dimming* over an already-dark screen. Measured on the
        // phone, not reasoned about. `onStartCommand` decides now, because it is the half of
        // starting that knows *why*.

        // The one source of truth for how dark it is. Collected rather than passed in the Intent so
        // that dragging the slider updates the live window without restarting anything.
        //
        // Kotlin note: `combine` over three `Flow`s is `Promise.all` over streams that never finish
        // — it emits a fresh tuple whenever *any* input changes, so this gets one settled
        // `DimSettings` rather than three callbacks it would have to reconcile, racing each other
        // into `updateViewLayout`. `distinctUntilChanged` after it is what stops an unrelated
        // preference write re-applying the window layout.
        combine(
            preferences.dimLevel,
            preferences.warmth,
            preferences.lowerBacklight,
        ) { level, warmth, lower ->
            DimSettings(dimLevel = level, warmth = warmth, lowerBacklight = lower)
        }.distinctUntilChanged()
            .onEach { settings ->
                lastSettings = settings
                applyShadeValues(settings)
            }.launchIn(scope)

        // Auto-off. `collectLatest` is `switchMap` rather than `forEach`: a new deadline cancels the
        // wait still running for the old one, which is what re-arms the loop when the user taps a
        // different chip while the shade is up, and when the boot receiver restores a deadline. The
        // loop cannot capture the deadline once, because it moves for reasons other than starting.
        scope.launch {
            preferences.shadeIntent
                .map { it.offAtMillis }
                .distinctUntilChanged()
                .collectLatest { offAt -> awaitDeadline(offAt) }
        }
    }

    /**
     * Wait out one deadline and take the shade down, or return immediately if there is none.
     *
     * **`NonCancellable` is required here rather than defensive, and without it the failure is
     * intermittent.** [AppPreferences.endShade] writes the very value this block is collecting: the
     * write commits, the flow emits, `collectLatest` cancels the block that is still running — and
     * the cancellation lands *between* the write returning and [stopSelf]. The shade comes down,
     * the intent goes false, and the service stays alive holding an ongoing notification over a
     * screen it is no longer dimming. Putting `stopSelf()` first does not help either: `onDestroy`
     * cancels the scope, which cancels the write from the other side.
     *
     * Kotlin note: cancellation is only ever observed at a suspension point, so once the
     * `NonCancellable` block returns, the plain `stopSelf()` after it always runs. This is the one
     * place worth stepping outside structured concurrency, and JS has no analogue — a `Promise`
     * cannot be cancelled out from under you, so there is nothing to opt out of.
     */
    private suspend fun awaitDeadline(offAtMillis: Long?) {
        if (offAtMillis == null) return
        while (coroutineContext.isActive) {
            val remaining = offAtMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                // The one line R7 reads the lateness off. A notification vanishing is visible on the
                // phone and is not timestamped, and logging is not developer *surface*, so it lives
                // here rather than behind the debug seam.
                Log.i(TAG, "auto-off fired ${-remaining}ms after the deadline")
                withContext(NonCancellable) { preferences.endShade() }
                stopSelf()
                return
            }
            delay(min(remaining, DEADLINE_RECHECK_MS))
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopFromWithin()
            return START_NOT_STICKY
        }
        // Unconditionally, and before the panel branch below: `startForegroundService` carries a
        // hard contract — call `startForeground` within a few seconds or Android kills the process
        // — and a summon delivered to a service the ROM had killed and restarted arrives here too.
        // Calling it again on an already-foreground service costs nothing.
        startForeground(NOTIFICATION_ID, buildNotification())
        // A summon raises the panel and nothing else; every other reason to be started — the app's
        // own Start, the boot receiver, and Android's `START_STICKY` restart with a null intent —
        // raises the shade. The `else` is the load-bearing half: it is what keeps a summon that
        // arrives at a dead service from putting the shade up as a side effect of creating one.
        if (intent?.action == ACTION_SHOW_PANEL) showPanel() else addShadeWindow()
        return START_STICKY
    }

    /**
     * The shade comes down, by the notification's Stop action or by the panel's own button.
     *
     * Records that the user turned it off *before* stopping. Without this the stored intent still
     * says "running", and the next launch — or a system restart of a `START_STICKY` service — would
     * helpfully put the shade back over a user who just dismissed it.
     */
    private fun stopFromWithin() {
        scope.launch {
            preferences.endShade()
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        // Before the shade, because the panel is a child of the shade's lifetime rather than a peer
        // of it: there is no state in which the panel is up and the shade is not. This is the third
        // of the panel's three ways out, and the only one the user does not ask for.
        removePanelWindow()
        removeShadeWindow()
        super.onDestroy()
    }

    private fun addShadeWindow() {
        if (shadeView != null) return
        // Guarded because the permission can be revoked while the service is alive, and `addView`
        // throws rather than returning a failure. A shade that fails to appear is a bug; a crash
        // over it is a worse one.
        if (!canDrawShade()) return

        // **Amber above black**, and the order is not arbitrary. It makes no difference to how much
        // of the content survives — `(1 - a) x (1 - w)` either way — but it decides the amber's own
        // strength: underneath, the tint would be attenuated by exactly the black layer that makes
        // it worth having, so warmth would fade out where a dark-adapted eye most notices it.
        //
        // Both start transparent. The window is added here, before the first preference emission
        // arrives, and a child at Android's default alpha of 1 would paint one opaque black frame
        // over whatever the user was looking at.
        val dim =
            View(this).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                alpha = 0f
            }
        val warmth =
            View(this).apply {
                setBackgroundColor(SHADE_AMBER)
                alpha = 0f
            }
        val view =
            FrameLayout(this).apply {
                addView(dim, fillParams())
                addView(warmth, fillParams())
            }
        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    SHADE_WINDOW_FLAGS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    // **Measured on a device, not assumed.** With the flags alone the shade stopped
                    // below the status bar on HyperOS: everything dimmed except a bright strip
                    // across the top, which reads as a bug rather than as a design. The system's own
                    // overlays carry this attribute and ours did not.
                    //
                    // ALWAYS needs API 30 and `minSdk` is 33 (ADR-0008), so this is unconditional.
                    // It used to branch three ways; two of those branches could never run on a
                    // device this app ships to, and one of them had never executed anywhere.
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }

        runCatching { windowManager?.addView(view, params) }
            .onSuccess {
                shadeView = view
                dimLayer = dim
                warmthLayer = warmth
                shadeParams = params
                // The children are born transparent and the collector that would darken them is
                // `distinctUntilChanged`, so on every path where it has already emitted, this is
                // the only thing that paints them. See [lastSettings].
                lastSettings?.let { applyShadeValues(it) }
            }
    }

    /**
     * Both children fill the window.
     *
     * Stated rather than left to `FrameLayout`'s default, and a fresh instance per child rather than
     * one shared: a `ViewGroup` keeps the reference it is handed, so two children sharing one
     * `LayoutParams` are two children one edit away from moving together.
     */
    private fun fillParams() =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

    private fun removeShadeWindow() {
        val view = shadeView ?: return
        runCatching { windowManager?.removeView(view) }
        shadeView = null
        dimLayer = null
        warmthLayer = null
        shadeParams = null
        backlightTop = null
    }

    /**
     * Summon the panel, or do nothing.
     *
     * **`shadeView == null` is `docs/phase-3.md` §6's rule 2, enforced rather than assumed**: the
     * panel cannot outlive the shade, and it may not precede it either — a window that cannot exist
     * without a shade can never be the surface that starts one. On the notification route that
     * precondition holds by construction, because the notification only exists while this service
     * does; the check is what keeps a stale `PendingIntent` or a future caller from finding out
     * otherwise.
     *
     * The read is `suspend`, so the window is added from a coroutine rather than from here. That is
     * the whole reason [panelStateNow] exists: a `Flow` collected inside the composition would
     * arrive after the first frame, and the first frame is the one with the user's dim level on it.
     */
    private fun showPanel() {
        if (panelView != null) return
        scope.launch {
            val state = panelStateNow()
            // **The stored intent decides, not the window field.** The guard used to read
            // `shadeView == null`, meaning "only over a live shade" — but on the path that matters
            // it could never fail, because the summon that created the service had just filled that
            // field in `onCreate`. What the user asked for is in DataStore; a summon arriving after
            // they stopped the shade has nothing to open over, and the service it started has no
            // reason to stay alive.
            if (!state.running) {
                Log.i(TAG, "summoned with the shade stopped; nothing to open over")
                stopSelf()
                return@launch
            }
            // A no-op whenever the shade is already up, which is the ordinary case. It does the
            // work only when the ROM killed the service while the stored intent still said running
            // — the summon then restores the shade first and opens the panel over it, rather than
            // opening a panel with nothing underneath.
            addShadeWindow()
            addPanelWindow(state)
        }
    }

    /**
     * Everything the panel draws, read once, before there is a window to draw it in.
     *
     * Six `first()` calls rather than one combined read, and it is not six disk reads: DataStore
     * serves `store.data` from its in-memory cache once anything has collected it, and this service
     * has been collecting since `onCreate`. There is also nothing to tear here — unlike
     * [AppPreferences.shadeIntent], no two of these are written in one transaction, so a reader that
     * takes them apart cannot see a half-written anything.
     */
    private suspend fun panelStateNow(): PanelState =
        PanelState(
            dimLevel = preferences.dimLevel.first(),
            warmth = preferences.warmth.first(),
            lowerBacklight = preferences.lowerBacklight.first(),
            running = preferences.shadeIntent.first().running,
            themeMode = preferences.themeMode.first(),
            materialYou = preferences.materialYou.first(),
        )

    /**
     * Add the panel's window above the shade, with the controls in it.
     *
     * ## The size is the safety bound
     *
     * A touchable window blocks every touch under it, so where the shade is safe because of a flag,
     * the panel is safe because of its `LayoutParams` — a weaker kind of guarantee, because a flag
     * is a constant and a size is a computation. [panelWidthPx] is that computation and
     * `PanelWidthTest` sweeps it. The height is `WRAP_CONTENT` on purpose: a bounded height clips a
     * control, and an unreachable close button is the trap this whole design is shaped to avoid.
     *
     * ## Why it is bottom-anchored and offset
     *
     * `Gravity.BOTTOM` puts the controls under the thumb rather than over the middle of whatever the
     * user is reading, and the offset above that edge is the flat [PANEL_BOTTOM_MARGIN_DP]. The
     * navigation bar is deliberately *not* added to it: the panel carries no `FLAG_LAYOUT_NO_LIMITS`
     * — which the shade does — so the window manager lays it out inside a display frame that already
     * stops above the bar, whichever navigation mode the phone is in. R6 read that off the phone,
     * after a first attempt added the inset by hand and floated the panel five times too high.
     *
     * ## No brightness of its own
     *
     * `screenBrightness` is left at `BRIGHTNESS_OVERRIDE_NONE`, which means *not asking* rather than
     * *asking for nothing* — and R1 measured that the shade underneath keeps its override when this
     * window declines. One dim level, one ramp, in a fixed order (ADR-0010): a second window deriving
     * a second brightness would be a second ramp wearing a window's clothes.
     */
    private fun addPanelWindow(initial: PanelState) {
        if (panelView != null) return
        val manager = windowManager ?: return
        // Guarded like `addShadeWindow`: the permission can be revoked while the service is alive,
        // and `addView` throws rather than returning a failure.
        if (!canDrawShade()) return

        val state = MutableStateFlow(initial)
        val host = PanelHost(this) { panelTouches.tryEmit(Unit) }
        // Read once per summon rather than collected: whether this device honours a window
        // brightness override at all is a property of the device, not a preference.
        val backlightAvailable = readBacklightTop(this) != null

        host.setContent {
            PanelContent(
                state = state,
                backlightAvailable = backlightAvailable,
                onDimLevel = { level -> scope.launch { preferences.setDimLevel(level) } },
                onWarmth = { warmth -> scope.launch { preferences.setWarmth(warmth) } },
                onLowerBacklight = { on -> scope.launch { preferences.setLowerBacklight(on) } },
                onToggleRunning = ::togglePanelRunning,
                onClose = ::removePanelWindow,
            )
        }

        val params =
            WindowManager
                .LayoutParams(
                    currentPanelWidth(manager),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    PANEL_WINDOW_FLAGS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.BOTTOM
                    // **Measured, not assumed** (R6). Without `FLAG_LAYOUT_NO_LIMITS` — which the
                    // shade has and the panel deliberately does not — the window is laid out inside
                    // the display frame the system already keeps clear of the navigation bar, so
                    // `y` is an offset from the *top* of that bar rather than from the bottom of the
                    // display. Adding the navigation-bar inset here counted it twice and floated the
                    // panel 59 dp up instead of 12.
                    y = (PANEL_BOTTOM_MARGIN_DP * resources.displayMetrics.density).toInt()
                }

        runCatching { manager.addView(host.view, params) }
            .onSuccess {
                panelView = host.view
                panelHost = host
                panelState = state
                panelParams = params
                // **`RESUMED` is not a formality.** Below `STARTED` the `Recomposer` stops applying
                // recompositions, and the window draws its first frame correctly and then never
                // changes again — a slider that will not move under a finger, with nothing in
                // logcat to say why.
                host.show()
                panelJob = scope.launch { trackPanel(state) }
            }.onFailure { host.destroy() }
    }

    /** The panel's width for the display as it is *now*, which is the only input [panelWidthPx] has. */
    private fun currentPanelWidth(manager: WindowManager): Int =
        panelWidthPx(manager.currentWindowMetrics.bounds.width())

    /**
     * Re-measure the panel when the display changes shape.
     *
     * **Android platform, not our choice:** a `WindowManager` window laid out from explicit pixels
     * keeps those pixels across a rotation. Nothing re-runs [panelWidthPx], so without this the
     * panel wears the width of whichever orientation it was summoned in. R8 read the bad case off
     * the phone: summoned in landscape the width cap gives 1200 px, and rotating to portrait leaves
     * 1200 px on a 1220 px display — a touchable window with 10 px of screen either side of it,
     * where the whole point of the inset is that touches still reach the app beside the panel.
     *
     * Only the width. The height is `WRAP_CONTENT`, so Compose re-measures it for free, and `y` is
     * a density-scaled constant that a rotation does not change.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = panelView ?: return
        val params = panelParams ?: return
        val manager = windowManager ?: return
        val width = currentPanelWidth(manager)
        if (params.width == width) return
        params.width = width
        runCatching { manager.updateViewLayout(view, params) }
    }

    /**
     * Take the panel down, from any of its three routes: the close control, the idle timeout, or
     * `onDestroy` when the shade goes.
     *
     * **It must not take the shade with it.** The panel closing is not the user saying they are done
     * reading; the two lifetimes point one way only.
     */
    private fun removePanelWindow() {
        panelJob?.cancel()
        panelJob = null
        val view = panelView ?: return
        runCatching { windowManager?.removeView(view) }
        panelHost?.destroy()
        panelView = null
        panelHost = null
        panelState = null
        panelParams = null
    }

    /**
     * Keep [PanelState] current, and take the panel down when it has been left alone.
     *
     * One collector per preference rather than one `combine` over six, because each writes its own
     * field with `copy()` and none of them needs to know about the others — and `coroutineScope`
     * makes the whole set one cancellable unit, which is what [removePanelWindow] cancels.
     *
     * **The timeout is `collectLatest`, which is `switchMap` and not `forEach`**: a new touch cancels
     * the wait still running for the previous one, which is what re-arms it. `onStart` emits once so
     * the clock is running from the moment the panel appears rather than from the first touch — a
     * panel drawn where nobody can touch it is exactly the case the timeout exists for.
     *
     * Calling [removePanelWindow] from inside this block cancels the job the block is running in.
     * That is safe here and would not be in general: cancellation is only observed at a suspension
     * point, and there is none after the call, so the removal completes before the coroutine dies.
     * `ShadeService`'s deadline loop is the same hazard with the opposite answer — there the write
     * suspends, which is what `NonCancellable` is for in [awaitDeadline].
     */
    private suspend fun trackPanel(state: MutableStateFlow<PanelState>) =
        coroutineScope {
            preferences.dimLevel.onEach { v -> state.update { it.copy(dimLevel = v) } }.launchIn(this)
            preferences.warmth.onEach { v -> state.update { it.copy(warmth = v) } }.launchIn(this)
            preferences.lowerBacklight.onEach { v -> state.update { it.copy(lowerBacklight = v) } }.launchIn(this)
            preferences.shadeIntent.onEach { v -> state.update { it.copy(running = v.running) } }.launchIn(this)
            preferences.themeMode.onEach { v -> state.update { it.copy(themeMode = v) } }.launchIn(this)
            preferences.materialYou.onEach { v -> state.update { it.copy(materialYou = v) } }.launchIn(this)

            launch {
                panelTouches.onStart { emit(Unit) }.collectLatest {
                    delay(PANEL_IDLE_TIMEOUT_MS)
                    Log.i(TAG, "panel idle for ${PANEL_IDLE_TIMEOUT_MS}ms, taking it down")
                    removePanelWindow()
                }
            }
        }

    /**
     * The panel's start/stop button.
     *
     * **In practice this only ever stops**, because the panel cannot be up without the shade — but
     * the button says what [PanelState.running] tells it to say, and a button that lies is worse
     * than a branch that rarely runs. The stop half is deliberately the same call the notification's
     * Stop action makes, so the two cannot drift; the start half is `DimViewModel`'s pair, because
     * a deadline is never written without the flag beside it.
     */
    private fun togglePanelRunning() {
        if (panelState?.value?.running == false) {
            scope.launch {
                val choice = preferences.autoOff.first()
                preferences.beginShade(deadlineFor(System.currentTimeMillis(), choice))
            }
        } else {
            stopFromWithin()
        }
    }

    /**
     * One dim level in, a backlight and an alpha out — the arithmetic itself is [shadeValuesFor],
     * which has no Android under it and is proven by `ShadeRampTest` rather than by looking at a
     * screen. What is left here is the part that genuinely needs a device: deciding *when* the
     * override is captured and when it is let go.
     */
    private fun applyShadeValues(settings: DimSettings) {
        // The override goes from released to applied exactly when the level crosses 0 or the toggle
        // comes on, and that is the moment to read: any later and we would be reading a brightness
        // the user could no longer see to change. A user who wants their own slider back drags Gloam
        // to 0, which releases the override — and makes the next capture a fresh one.
        if (settings.lowerBacklight && settings.dimLevel > 0) {
            if (backlightTop == null) backlightTop = readBacklightTop(this)
        } else {
            backlightTop = null
        }

        val values = shadeValuesFor(settings, backlightTop)
        // A view property, unlike the backlight below: `alpha` takes effect on its own, where
        // `params.screenBrightness` does nothing until the window layout is handed back.
        dimLayer?.alpha = values.shadeAlpha
        warmthLayer?.alpha = values.warmthAlpha
        applyBacklight(values.backlight)
    }

    /**
     * Push the window's brightness override, or release it.
     *
     * `null` becomes `BRIGHTNESS_OVERRIDE_NONE` here and nowhere else — the one place a nullable
     * Kotlin value collapses to Java's sentinel, the way a discriminated union collapses to a wire
     * format at the boundary.
     */
    private fun applyBacklight(value: Float?) {
        val view = shadeView ?: return
        val params = shadeParams ?: return
        val requested = value ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (params.screenBrightness == requested) return
        params.screenBrightness = requested
        // Guarded for the same reason `addView` is: the window can be gone underneath us.
        runCatching { windowManager?.updateViewLayout(view, params) }
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

/**
 * Summon the panel over a shade that is already up.
 *
 * `startForegroundService` rather than `startService` for the same contract [startShade] carries:
 * the target may not be running — a ROM kill between the caller reading the notification and tapping
 * it is exactly the case — and a background `startService` on a dead service throws. The service
 * calls `startForeground` on its first statement either way.
 */
fun Context.showShadePanel() {
    startForegroundService(Intent(this, ShadeService::class.java).setAction(ACTION_SHOW_PANEL))
}
