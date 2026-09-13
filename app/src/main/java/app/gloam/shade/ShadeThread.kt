package app.gloam.shade

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * The thread the shade's window lives on. It is not the main thread, and that choice fixes a visible
 * flash rather than tuning speed.
 *
 * ## The flash it removes
 *
 * **Android platform, not our choice: a rotation takes the backlight override away until the shade
 * has redrawn.** An overlay's `screenBrightness` counts only while its latest frame is committed, and
 * a rotation sends every window back to "draw pending" (AOSP's `RootWindowContainer.
 * handleNotObscuredLocked`). On Android 16 the window manager sends that redraw request to the app as
 * a *client transaction*, which runs on the process's **main thread** (`WindowState.reportResized`).
 * So the request waits behind whatever else the main thread is doing when the display turns.
 *
 * **Here, that was our own screens.** Measured on the phone on 2026-09-13: with Chrome in front, the
 * shade redrew 67-78 ms after a rotation. With Gloam's own screen in front it took 350-450 ms, because
 * the main thread was recreating that screen and laying out Compose for the new orientation. For those
 * ~400 ms the backlight climbed towards the user's own level. That was the tester's "rotating breaks
 * the dim for about a second".
 *
 * A window's views belong to the thread that *added* the window, not to the main thread. A shade
 * added from here therefore draws on this thread's own frame clock, and a busy Compose frame no longer
 * holds it back.
 *
 * **What no thread can fix:** during the rotation animation Android hides every overlay window,
 * whatever its type, and fades it back in afterwards (`AsyncRotationController`). A short flash on
 * each turn stays for every overlay dimmer.
 *
 * ## Kotlin/Android notes
 *
 * JavaScript gives a page one thread, so "which thread owns this object" has no analogue there. On
 * Android every `View` is bound to the thread whose `Looper` added its window. Touching it from
 * another thread throws, or races silently. [run] is the single way in.
 *
 * `run` **blocks** its caller until the block has finished. That is like `await`, except the calling
 * thread really stops instead of yielding. It is deliberate: `ShadeService` keeps all of its state on
 * the main thread as before, the window calls happen in the order they always did, and the wait is one
 * binder call long, because nothing else ever runs on this thread. Blocking main is harmless for the
 * same reason. What must not be blocked is *this* thread, and nothing here waits on main.
 */
internal class ShadeThread {
    /**
     * `THREAD_PRIORITY_DISPLAY` is the priority Android gives threads that draw. At the default
     * background priority, this thread could lose the CPU to exactly the Compose work it exists to
     * get away from.
     */
    private val thread = HandlerThread("gloam-shade", Process.THREAD_PRIORITY_DISPLAY).apply { start() }

    private val handler = Handler(thread.looper)

    /** Run [block] on the shade thread and hand back its result, waiting for it. */
    fun <T> run(block: () -> T): T {
        // Already here: running inline is the only answer that cannot deadlock.
        if (Looper.myLooper() == thread.looper) return block()
        val task = FutureTask(block)
        // A failed post would otherwise wait forever on a task nothing will run, which is an ANR.
        // Only a call after [quit] can get here, and that is a bug worth crashing on.
        check(handler.post(task)) { "The shade thread has already quit." }
        return try {
            task.get()
        } catch (e: ExecutionException) {
            // Rethrow the block's own exception, not the wrapper, so a stack trace names the real
            // failure. Every window call inside is already `runCatching`, so this is a bug if it runs.
            throw e.cause ?: e
        }
    }

    /**
     * Stop the thread once everything already posted to it has run.
     *
     * `quitSafely` rather than `quit`: removing a window finishes with a message the window's own
     * `ViewRootImpl` posts to this thread. Plain `quit` would drop that message and leave the removal
     * half done.
     */
    fun quit() {
        thread.quitSafely()
    }
}
