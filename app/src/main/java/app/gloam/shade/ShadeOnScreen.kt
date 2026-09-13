package app.gloam.shade

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the shade's window is on screen in this process right now — **`CONTEXT.md`'s *running*,
 * the live state**, as opposed to `shadeRunning` in DataStore, which is what the user asked for.
 *
 * ## Why the button needs both
 *
 * HyperOS *Clear all* kills Gloam whenever another app is in front, and without autostart nothing
 * brings the service back. The stored intent still says running, so a button drawn from it alone
 * reopened on *Stop dimming* over a screen with no shade on it, and getting the shade back took Stop
 * and then Start (`docs/phase-2.md`, R8a's other half). The button shows *Stop* only while this is
 * true as well; otherwise it offers *Start*, which is one tap and says what is on the screen.
 *
 * **The intent is not touched, and that is `CLAUDE.md`'s rule rather than caution.** A kill is not
 * the user changing their mind, so nothing here writes *stopped* on their behalf: the boot receiver,
 * `START_STICKY` under autostart and the deadline all still read what they asked for. This is a
 * second fact shown beside the first, never a source for it. Restoring the shade on open instead was
 * built and turned down: with auto-off at *Never* it would dim a screen the next afternoon because
 * the app was opened.
 *
 * ## Why a process-wide value is exact here
 *
 * The service and every screen that reads this share one process, and a killed process takes the
 * window with it — so a value that lives in memory and starts at `false` is wrong in neither
 * direction. No `ActivityManager` query is involved; `getRunningServices` is deprecated for exactly
 * this use and would answer about the service rather than the window.
 *
 * Kotlin note: a `StateFlow` is a `BehaviorSubject` — it always has a current value, and a new
 * collector gets that value first — which is what lets a screen composed after the window went up
 * still see it.
 */
val shadeOnScreen: StateFlow<Boolean>
    get() = onScreen.asStateFlow()

private val onScreen = MutableStateFlow(false)

/** The service's half: set exactly where the window is added and removed, and nowhere else. */
internal fun reportShadeOnScreen(up: Boolean) {
    onScreen.value = up
}
