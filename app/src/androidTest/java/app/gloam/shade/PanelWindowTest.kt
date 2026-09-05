package app.gloam.shade

import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.gloam.MainApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The panel's window, read off a real window manager — the half of `docs/phase-3.md` §6 that no
 * constant can answer.
 *
 * **Two properties, and only a device has either.**
 *
 * - **It is above the shade.** Both windows are `TYPE_APPLICATION_OVERLAY` from the same uid, and
 *   ordering within a type is the window manager's business: the expectation is insertion order,
 *   which is an expectation rather than a documented guarantee. It is the whole case for the panel
 *   — a panel *below* the shade is a panel dimmed by the thing it exists to control — so a ROM
 *   update turning it over should show up as a red leg rather than as a support mail. R1 took this
 *   reading by hand against a bare rectangle; this is the same assertion, kept.
 * - **It is smaller than the display.** A touchable window blocks every touch under it, so its size
 *   is what stands in for the `FLAG_NOT_TOUCHABLE` the shade has and the panel deliberately drops.
 *   `PanelWidthTest` proves the arithmetic; this proves the window was actually built from it.
 *
 * **The cost, stated honestly** — as in `ShadeWindowTest`: `dumpsys window windows` is
 * version-shaped text, and this is the layer that will need attention when a platform changes its
 * output. That is exactly why the flags and the width are *also* asserted on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class PanelWindowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val packageName: String = context.packageName

    private val preferences get() = (context.applicationContext as MainApplication).preferences

    private var overlayWasGranted = false

    @Before
    fun grantWhatThePanelNeeds() {
        overlayWasGranted = Settings.canDrawOverlays(context)
        if (!overlayWasGranted) {
            shell("appops set $packageName SYSTEM_ALERT_WINDOW allow")
        }
        shell("pm grant $packageName android.permission.POST_NOTIFICATIONS")
        assertTrue(
            "The overlay grant did not take, so no window below could be read. Verified with " +
                "canDrawOverlays() rather than by reading the appop back, which reports last-use.",
            Settings.canDrawOverlays(context),
        )
    }

    @After
    fun putTheDeviceBack() {
        // One call takes both windows down: the panel cannot outlive the shade. The write goes with
        // it so a test leg cannot leave the next one — or the phone — believing the user asked for
        // a shade that is not there.
        //
        // `endShade` directly rather than `endShadeAt`, and `honouredAt = null` explicitly: this is
        // a teardown putting the device back, not somebody deciding, so it must leave the schedule's
        // marker exactly as it found it. A leg that spent a night would change what the next leg's
        // reconcile does.
        runBlocking { preferences.endShade(honouredAt = null) }
        context.stopShade()
        awaitNoOverlayWindows()
        if (!overlayWasGranted) {
            shell("appops set $packageName SYSTEM_ALERT_WINDOW default")
        }
    }

    /**
     * Block until neither of our overlay windows is left in the dump.
     *
     * `stopShade()` takes both windows down, but not synchronously: it stops the service, and the
     * window manager removes the windows some time afterwards. Returning before that leaves the
     * next test — this class's second one, or `ShadeWindowTest` — reading a dump that still holds
     * this one's windows, and failing for the previous test's reasons.
     *
     * Best-effort rather than asserted, so a window that outlives its timeout does not fail the run
     * from tear-down. The wait is the point; the guarantees belong to the tests.
     */
    private fun awaitNoOverlayWindows() {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (ourOverlayWindows().isEmpty()) return
            Thread.sleep(POLL_MS)
        }
    }

    @Test
    fun thePanelSitsAboveTheShadeAndDoesNotCoverIt() {
        // The write as well as the start, for the reason `DimScreen` does both: `startShade()` puts
        // a window on screen, and the write is what records that the user asked for it. The panel
        // reads the stored intent to decide whether it has anything to open over, so a start
        // without the write is a shade the summon is right to refuse.
        runBlocking { preferences.beginShade(offAtMillis = null) }
        context.startShade()
        assertNotNull(
            "No shade window appeared within ${TIMEOUT_MS}ms of startShade(); nothing below could " +
                "be read.",
            awaitWindow { it.isShade },
        )

        context.showShadePanel()
        val panel =
            awaitWindow { it.isPanel }
                ?: throw AssertionError(
                    "No wrap-height APPLICATION_OVERLAY window for $packageName appeared within " +
                        "${TIMEOUT_MS}ms of showShadePanel(). The service never added the panel, " +
                        "or dumpsys no longer prints windows the way this test reads them.",
                )

        val windows = ourOverlayWindows()
        val panelIndex = windows.indexOfFirst { it.isPanel }
        val shadeIndex = windows.indexOfFirst { it.isShade }
        assertTrue("The shade is not in the window list beside the panel", shadeIndex >= 0)
        // `dumpsys window windows` prints the stack top-first, so a smaller index is higher up.
        assertTrue(
            "The panel is below the shade, so it is dimmed by the thing it exists to control. " +
                "Windows, top first:\n" + windows.joinToString("\n\n") { it.text },
            panelIndex < shadeIndex,
        )

        val displayWidth =
            context
                .getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds
                .width()
        val requested =
            REQUESTED_WIDTH
                .find(panel.text)
                ?.groupValues
                ?.get(1)
                ?.toInt()
                ?: throw AssertionError(
                    "dumpsys did not print a requested width for the panel; this test reads " +
                        "'Requested w=…'. Window was:\n${panel.text}",
                )
        assertTrue(
            "The panel was added $requested px wide over a $displayWidth px display — a touchable " +
                "window that spans the display blocks every touch under it",
            requested < displayWidth,
        )
        assertEquals(
            "The panel's width did not come from panelWidthPx(), so the swept bound is not the " +
                "one the window manager got",
            panelWidthPx(displayWidth),
            requested,
        )
    }

    /**
     * A summon that arrives with the shade stopped must raise nothing at all.
     *
     * **This is a regression test for a bug the phone found, not a hypothetical.** `showShadePanel()`
     * is a `startForegroundService`, so on a stopped app it does not "ask the service" anything — it
     * *creates* the service. The shade used to go up in `onCreate`, which meant a summon put the
     * shade on screen over a user who had turned it off, took the backlight override with it, and
     * left `shadeIntent` still saying "stopped" — so the controls read *Start dimming* over an
     * already-dark screen. It matters more for the notification than for the button that found it:
     * a `PendingIntent` outlives the process that built it, so a stale tap takes this path.
     *
     * Asserting on the shade rather than on the panel, because the panel not appearing was never
     * the bug.
     */
    @Test
    fun aSummonWithTheShadeStoppedRaisesNothing() {
        // Both halves, because they answer different questions and `DimScreen` does both: the write
        // is the user changing their mind, `stopShade()` is the window coming down. A test that
        // only stopped the service would leave the stored intent saying "running", and the summon
        // would then be right to restore the shade — which is the other branch, not this one.
        //
        // `ByHand` because that is what this line is standing in for. With no schedule stored it
        // resolves to the same write as the teardown's, and saying so keeps the two lines honest
        // about being different events rather than the same one twice.
        runBlocking { preferences.endShadeAt(ShadeEnd.ByHand) }
        context.stopShade()
        assertTrue(
            "A window of ours was still up ${TIMEOUT_MS}ms after stopShade(), so this test could " +
                "not have told a leftover shade from one the summon raised.",
            awaitNoWindows(),
        )

        context.showShadePanel()
        // A settle rather than a wait-for-something: the assertion is about an absence, and an
        // absence is only ever as strong as the time given to contradict it. The buggy build put
        // the shade up inside a second of the summon; this allows five. On a slow enough machine
        // the failure mode is a false pass, which is the harmless direction.
        Thread.sleep(SETTLE_MS)
        assertEquals(
            "Summoning the panel with the shade stopped raised a window. Windows, top first:\n" +
                ourOverlayWindows().joinToString("\n\n") { it.text },
            emptyList<String>(),
            ourOverlayWindows().map { it.text },
        )
    }

    /** Poll until we have no overlay window left, the mirror of [awaitWindow]. */
    private fun awaitNoWindows(): Boolean {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (ourOverlayWindows().isEmpty()) return true
            Thread.sleep(POLL_MS)
        }
        return ourOverlayWindows().isEmpty()
    }

    /** One `Window #N` block of `dumpsys window windows`, with the two questions it can answer. */
    private class OverlayWindow(
        val text: String,
    ) {
        /** `MATCH_PARENT` x `MATCH_PARENT` prints as `fill`, which only the shade asks for. */
        val isShade: Boolean get() = "fillxfill" in text

        /** `WRAP_CONTENT` height over an explicit pixel width, which only the panel asks for. */
        val isPanel: Boolean get() = "xwrap)" in text
    }

    /**
     * Poll until a window matching [predicate] shows up.
     *
     * Polling rather than one sleep, for `ShadeWindowTest`'s reason: adding a window is
     * asynchronous, and the wait it needs is a few hundred milliseconds on a phone and considerably
     * more on a cold emulator leg.
     */
    private fun awaitWindow(predicate: (OverlayWindow) -> Boolean): OverlayWindow? {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            ourOverlayWindows().firstOrNull(predicate)?.let { return it }
            Thread.sleep(POLL_MS)
        }
        return ourOverlayWindows().firstOrNull(predicate)
    }

    /**
     * Every `TYPE_APPLICATION_OVERLAY` window of ours, in the order `dumpsys` printed them.
     *
     * Identified by the package name *and* the type together, because the app's own activities have
     * windows in this dump too and matching one of those would prove nothing.
     */
    private fun ourOverlayWindows(): List<OverlayWindow> =
        shell("dumpsys window windows")
            .split("Window #")
            .filter { packageName in it && "ty=APPLICATION_OVERLAY" in it }
            .map { OverlayWindow(it) }

    private fun shell(command: String): String =
        ParcelFileDescriptor
            .AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes().decodeToString() }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val POLL_MS = 250L
        const val SETTLE_MS = 5_000L
        val REQUESTED_WIDTH = Regex("""Requested w=(\d+)""")
    }
}
