package app.gloam.shade

import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shade's window, read off a real window manager.
 *
 * **The first test in `androidTest/`, and it exists because the matrix that runs it had nothing to
 * run.** Three instrumented CI legs were passing by having zero tests — a green tick with nothing
 * behind it — and this is the one property worth putting there: a JVM test can assert what
 * [SHADE_WINDOW_FLAGS] *contains*, but only a device can say whether the window that reached the
 * window manager was built from it.
 *
 * **What it asserts**: the shade arrives as `TYPE_APPLICATION_OVERLAY` carrying `NOT_TOUCHABLE` and
 * `NOT_FOCUSABLE`. Lose either and the phone appears frozen with the escape hatch underneath the
 * thing freezing it, which is the worst outcome this app has.
 *
 * **The cost, stated honestly**: `dumpsys window windows` is version-shaped text and this is the
 * layer that will need attention when a platform changes its output. That is exactly why the flag
 * set is *also* asserted on the JVM, where nothing can drift.
 *
 * **Never read the appop back to check the grant.** `appops get` reports last-use rather than the
 * granted mode on the ROM in this loop — a trap already recorded once in `CLAUDE.md` — so the grant
 * is verified with [Settings.canDrawOverlays], which asks the question we actually mean.
 */
@RunWith(AndroidJUnit4::class)
class ShadeWindowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val packageName: String = context.packageName

    /**
     * Whether this device already had the overlay grant before the test touched it.
     *
     * The tear-down uses it to leave the device as it was found. On CI that is a formality; on the
     * physical phone — where `CLAUDE.md`'s split-APK workaround runs this same instrumentation — it
     * is the difference between a test run and a test run that silently revokes the permission the
     * app cannot start without.
     */
    private var overlayWasGranted = false

    @Before
    fun grantWhatTheShadeNeeds() {
        overlayWasGranted = Settings.canDrawOverlays(context)
        if (!overlayWasGranted) {
            shell("appops set $packageName SYSTEM_ALERT_WINDOW allow")
        }
        // Not for the window — a foreground service starts and adds its view without it — but the
        // service posts an ongoing notification, and a denied POST_NOTIFICATIONS leaves the run
        // logging a failed post on every leg. `pm grant` through the shell we have open already,
        // rather than a GrantPermissionRule, because that rule lives in `androidx.test:rules` and
        // this is the whole of what the dependency would buy.
        //
        // **This one is not put back, and that is deliberate.** `am instrument` runs the test *in
        // the target app's process*, and `pm revoke` restarts that process — the tear-down would
        // kill the run reading its own result. The appop above has no such effect, which is why
        // only it is restored.
        shell("pm grant $packageName android.permission.POST_NOTIFICATIONS")

        assertTrue(
            "The overlay grant did not take, so nothing below could be read. Verified with " +
                "canDrawOverlays() rather than by reading the appop back, which reports last-use.",
            Settings.canDrawOverlays(context),
        )
    }

    @After
    fun putTheDeviceBack() {
        context.stopShade()
        awaitNoOverlayWindows()
        if (!overlayWasGranted) {
            shell("appops set $packageName SYSTEM_ALERT_WINDOW default")
        }
    }

    /**
     * Block until no `TYPE_APPLICATION_OVERLAY` window of ours is left in the dump.
     *
     * `stopShade()` is not synchronous with the screen: it stops the service, and the window
     * manager removes the window some time afterwards. Returning from tear-down before that has
     * happened leaves the *next* test polling a dump that still contains this one's windows — which
     * is a test failing for the previous test's reasons, the hardest kind to read.
     *
     * Best-effort rather than asserted: a tear-down that fails the run for a window that outlived
     * its timeout would replace a rare flake with a rarer, more confusing one. The wait is the
     * point; the guarantee belongs to the assertions in the tests themselves.
     */
    private fun awaitNoOverlayWindows() {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val ours =
                shell("dumpsys window windows")
                    .split("Window #")
                    .any { packageName in it && "ty=APPLICATION_OVERLAY" in it }
            if (!ours) return
            Thread.sleep(POLL_MS)
        }
    }

    @Test
    fun theShadeWindowCannotTrapTheUser() {
        context.startShade()

        val window =
            awaitShadeWindow()
                ?: throw AssertionError(
                    "No full-screen ($SHADE_LAYOUT) APPLICATION_OVERLAY window for $packageName " +
                        "appeared within ${TIMEOUT_MS}ms of startShade(). The service never added " +
                        "its view, the shade stopped asking for MATCH_PARENT, or dumpsys no longer " +
                        "prints windows the way this test reads them.",
                )

        assertTrue(
            "The shade's window is missing NOT_TOUCHABLE — every touch on this screen would be " +
                "swallowed by a window the user cannot see. Window was:\n$window",
            "NOT_TOUCHABLE" in window,
        )
        assertTrue(
            "The shade's window is missing NOT_FOCUSABLE — key events would go to the shade " +
                "instead of the app underneath it. Window was:\n$window",
            "NOT_FOCUSABLE" in window,
        )
    }

    /**
     * Poll `dumpsys` until the shade's window shows up, and hand back the block describing it.
     *
     * Polling rather than one sleep: adding the window is asynchronous — `startForegroundService`
     * hands off to the service's `onCreate` — and the wait it needs is a few hundred milliseconds on
     * a phone and considerably more on a cold emulator leg. A fixed sleep long enough for the
     * emulator would be paid on every run; a short one would be flaky on exactly the machine CI
     * uses.
     */
    private fun awaitShadeWindow(): String? {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            shadeWindowBlock()?.let { return it }
            Thread.sleep(POLL_MS)
        }
        return shadeWindowBlock()
    }

    /**
     * The `dumpsys window windows` block describing the **shade**, or null.
     *
     * The output is a sequence of `Window #N Window{…}` blocks, so splitting on that header is the
     * whole of the parsing. Ours is identified by three things together — the package name,
     * `ty=APPLICATION_OVERLAY`, and the shade's own full-screen layout. The first two are because
     * the app's activities have windows in this dump too and asserting flags against one of those
     * would pass while proving nothing.
     *
     * **The third is not belt-and-braces, it is the whole correctness of this test now.** Since
     * Phase 3b the panel is *also* `TYPE_APPLICATION_OVERLAY` from this same uid, and it
     * deliberately does not carry `NOT_TOUCHABLE` — that is ADR-0011, not a defect. So "the first
     * overlay window we own", which is what this used to take, can be the panel, and the assertion
     * below then reports the panel's missing flag as the shade's. That failure has actually
     * happened on a CI leg: `PanelWindowTest` sorts first, its teardown does not wait for the
     * window manager to finish removing what it put up, and this test matched the outgoing panel.
     *
     * `fillxfill` is `MATCH_PARENT` x `MATCH_PARENT`, which only the shade asks for — the same
     * discrimination `PanelWindowTest.OverlayWindow` already makes, and the reason a
     * `MATCH_PARENT` in the panel's `LayoutParams` is a bug rather than a simplification.
     */
    private fun shadeWindowBlock(): String? =
        shell("dumpsys window windows")
            .split("Window #")
            .firstOrNull { packageName in it && "ty=APPLICATION_OVERLAY" in it && SHADE_LAYOUT in it }

    /**
     * Run a shell command as the shell user and return everything it printed.
     *
     * Android note with no JS analogue: an instrumented test runs in its own process with the app's
     * permissions, which are not enough to grant an appop or read `dumpsys`. `UiAutomation` is the
     * hatch — commands issued through it run as uid 2000 (`shell`), the same uid `adb` gives you —
     * and it hands back a pipe rather than a string, which is why this reads a file descriptor.
     */
    private fun shell(command: String): String =
        ParcelFileDescriptor
            .AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { it.readBytes().decodeToString() }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val POLL_MS = 250L

        /** `MATCH_PARENT` x `MATCH_PARENT` as `dumpsys` prints it. Only the shade asks for it. */
        const val SHADE_LAYOUT = "fillxfill"
    }
}
