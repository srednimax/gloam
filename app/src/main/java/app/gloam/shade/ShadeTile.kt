package app.gloam.shade

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.gloam.ControlsActivity
import app.gloam.MainApplication
import app.gloam.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * **The second escape hatch: a Quick Settings tile that takes the shade down.**
 *
 * `docs/phase-2.md` §2 defines an escape hatch as a surface that stops the shade and can be reached
 * without seeing Gloam's own UI, and lists exactly three that survive at maximum dim — the ongoing
 * notification's **Stop** action, this tile, and the power menu. All three sit above
 * `TYPE_APPLICATION_OVERLAY`, which is why the shade cannot cover them.
 *
 * **Why a second one when the notification already works.** The argument is independence, not
 * strength. The notification can be taken away by two different switches — `POST_NOTIFICATIONS`
 * revoked, or, far more quietly, the shade's *notification channel* lowered to `IMPORTANCE_NONE`,
 * which leaves the permission reading as granted while nothing appears on screen. [escapeHatchLive]
 * is the predicate over both. A tile is subject to neither: it needs no permission and no channel,
 * so the two hatches fail for disjoint reasons. That is the whole case for having both.
 *
 * **It is not a gate term.** `EscapeHatch.kt` was written expecting `escapeHatchLive() || tileAdded()`
 * to gate ultra dark. Phase 2b priced ultra-dark-by-alpha out — past [MAX_SHADE_ALPHA] there is 17%
 * of light to win and both of `shadeValuesFor`'s invariants to lose — so there is no new mode to
 * gate, and the disjunction is not built. That is the happier outcome, because **there is no live
 * read of "is my tile added"**. The platform offers `requestAddTileService` and the `onTileAdded` /
 * `onTileRemoved` callbacks, so the only way to answer it is to *remember*, and a remembered
 * `tile_added` key is live state in a store Auto Backup carries whole to the next phone — where the
 * tile is not added and the key says it is. `shade_began_at` is what that costs (CLAUDE.md).
 *
 * **One caller would still want the disjunction, and it keeps the conservative half.**
 * `ControlsActivity.forwardIfUnusable()` refuses the compact window when [escapeHatchLive] is false,
 * and with a tile added that refusal is now stricter than the danger warrants — the tile *is* a way
 * out. It stays as written: unreadable state cannot be an argument to a safety predicate, and the
 * failure it errs into is one extra hop to the full app.
 *
 * ## What it must never do
 *
 * **Stopping always works.** Every branch that can leave the shade up is a branch that can trap
 * somebody, so [onClick] has no precondition on the stop path — no permission read, no service
 * liveness check, no unlock. It writes the intent and calls [stopShade], and `stopService` on a
 * service that a ROM already killed is a no-op rather than a throw.
 *
 * Kotlin note: `TileService` is a `Service`, so it has a `Context` and may start the shade itself.
 * That is not the rule CLAUDE.md states for a `ViewModel` — the rule there is that a `ViewModel`
 * outlives no `Context`; a Service *is* one.
 */
class ShadeTile : TileService() {
    /**
     * Alive only between [onStartListening] and [onStopListening].
     *
     * Kotlin note: a `CoroutineScope` here is the structured-concurrency answer to "unsubscribe on
     * teardown" — cancelling the scope cancels the collection, the way clearing an interval handle
     * would in JS, except it is impossible to forget one of several.
     */
    private var listening: CoroutineScope? = null

    private val preferences: AppPreferences
        get() = (application as MainApplication).preferences

    /**
     * The platform calls this when the tile becomes visible, and only between here and
     * [onStopListening] may [getQsTile] be touched at all — outside that window the reference is
     * stale and `updateTile` does nothing.
     *
     * **It reads the stored intent, never the running service** (CLAUDE.md). Xiaomi kills the
     * service without the user changing their mind, and a tile drawn from process liveness would go
     * dark on its own and invite a second tap that starts a second episode.
     */
    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        listening = scope
        preferences.shadeIntent
            .onEach { intent -> render(intent.running) }
            .launchIn(scope)
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    private fun render(running: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    /**
     * Toggle the shade.
     *
     * The two directions are deliberately asymmetric, because their failure modes are:
     *
     * - **Down** is unconditional. See the class KDoc — a stop that can decline is not a hatch.
     * - **Up** needs `SYSTEM_ALERT_WINDOW`, which the user can revoke at any time, so without it
     *   this hands off to [ControlsActivity] rather than failing silently. A tile that does nothing
     *   when tapped is indistinguishable from a broken one.
     *
     * `ShadeStart.ByHand`: this *is* a hand start — somebody pulled the shade down and tapped — so
     * auto-off applies exactly as it does to the button on the dim screen. `Deadlines.kt` explains
     * why a scheduled start is the case that must not take it.
     *
     * The scope is created here rather than reusing [listening], because a click can be the last
     * thing that happens before the platform stops listening, and cancelling mid-write would leave
     * the intent and the service disagreeing.
     */
    override fun onClick() {
        super.onClick()
        val app = application as MainApplication
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val running = preferences.shadeIntent.first().running
            if (running) {
                preferences.endShadeAt(ShadeEnd.ByHand)
                app.stopShade()
            } else if (app.canDrawShade()) {
                preferences.beginShadeAt(ShadeStart.ByHand)
                app.startShade()
            } else {
                openTheApp()
            }
        }
    }

    /**
     * **Two spellings of the same call, and the split is the platform rather than a preference.**
     *
     * `startActivityAndCollapse(Intent)` is how this worked through API 33. API 34 deprecated it and
     * made it *throw* `UnsupportedOperationException` — not a warning, a crash — replacing it with a
     * `PendingIntent` overload that did not exist before. `minSdk` is 33 and `targetSdk` is 36, so
     * this app is the one window where both spellings are reachable and neither alone is enough.
     *
     * **`ControlsActivity`, not `MainActivity`, and the choice is `docs/phase-3.md` §3's rather than
     * this file's.** That section reserved the tile for 2b and left one integration behind so 2b
     * would not have to rediscover it: the tile's *open app* route goes to the compact host, because
     * a tile can be tapped with no shade running and the panel cannot answer that. It costs nothing
     * on the path this function is actually used from — `ControlsActivity.forwardIfUnusable()` sends
     * anyone without `canDrawShade()` straight on to `MainActivity`, through a window that never
     * draws — so today the two spellings land in the same place. It is written the documented way so
     * that a later long-press route inherits it rather than contradicting it.
     *
     * **Lint calls the `Intent` branch an error and it is wrong here, which is why the suppression is
     * on this function rather than in `build.gradle.kts`.** Its message — *"will throw … in apps
     * targeting UpsideDownCake and higher"* — reads `targetSdk` alone, and by that reading this app
     * throws everywhere. The throw is a `CompatChanges` gate in the **platform's** copy of
     * `TileService`, and Android 13 has no such gate to run: an API-33 device executes the old body
     * whatever the app targets, and the `PendingIntent` overload it would be told to use does not
     * exist there to call. So the branch is on `SDK_INT` — the device — and the suppression is
     * scoped to the one call lint cannot see the guard around.
     *
     * ⚠ **Reasoned from the platform sources, not measured.** The dev phone is well past 33 and
     * `phase-5.md`'s R6 has the API-33 emulator blocked by a segfault, so nothing here has run the
     * `else` branch. It is the branch that does nothing worse than fail to open a settings screen.
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openTheApp() {
        val intent =
            Intent(this, ControlsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
