package app.gloam.work

import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The channel enum and `strings.xml` are the same claim written twice, and this is what stops them
 * drifting — the same shape as `AppLanguageTest`, for the same reason.
 *
 * Checked **in both directions**, and neither failure is loud on its own. A channel with no strings
 * behind it cannot happen (the `R` reference would not compile), but a *stale* `channel_…` string
 * left behind after a channel is renamed or removed is a settings row's worth of copy describing
 * something that no longer exists — and channel ids are permanent, so this is the drift that
 * actually happens. Lint says nothing about either.
 *
 * Reads the resource off disk rather than through `R`: a JVM unit test has no Android framework
 * under it, and the file's *contents* are what is in question. Gradle runs unit tests with the
 * module directory as the working directory.
 */
class ChannelsTest {
    private val declared: Set<String> =
        Regex("""<string\s+name="(channel_[^"]+)"""")
            .findAll(File("src/main/res/values/strings.xml").readText())
            .map { it.groupValues[1] }
            .toSet()

    /** The naming convention the enum's `R.string` fields follow, stated once. */
    private val expected: Set<String> =
        AppChannel.entries
            .flatMap { channel -> listOf("channel_${channel.id}_name", "channel_${channel.id}_description") }
            .toSet()

    @Test
    fun `every channel has a name and a description, and nothing else declares one`() {
        assertEquals(expected, declared)
    }

    @Test
    fun `there is exactly one channel, and it is the shade's`() {
        // One per thing that posts, and no more. A channel is the user's only per-kind control, so
        // adding one is a decision about what they are allowed to mute separately — this test is
        // what makes that a deliberate act rather than a passing convenience.
        assertEquals(setOf("shade"), AppChannel.entries.map { it.id }.toSet())
    }

    @Test
    fun `at most one channel is high-importance`() {
        // The level is spent once, on purpose. If everything is HIGH then nothing is. The other
        // direction is what makes it worth pinning: a channel created at the wrong importance can
        // never be raised again, only lowered by the user, so it is a decision with no second try.
        assertTrue(
            AppChannel.entries.count { it.importance == NotificationManager.IMPORTANCE_HIGH } <= 1,
        )
    }

    @Test
    fun `only an ongoing channel may be created below IMPORTANCE_DEFAULT`() {
        // Creating an *alerting* channel quiet makes the mute decision on the user's behalf, in the
        // one direction that cannot be undone — so the rule stands for anything that arrives.
        //
        // A foreground service's notification never arrives: it is on screen for as long as the
        // service runs, because Android requires it to be. At DEFAULT it would buzz and peek a
        // banner at the moment the user asked for a darker screen. `ongoing` is what separates the
        // two cases, so the exemption has to be claimed explicitly rather than by picking a number.
        for (channel in AppChannel.entries) {
            if (!channel.ongoing) {
                assertTrue(
                    "${channel.id} alerts, so it may not start below DEFAULT",
                    channel.importance >= NotificationManager.IMPORTANCE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun `channel ids are stable, lowercase and free of the app's package`() {
        // A renamed id is a *new* channel: the user's mute silently goes back to unmuted and there
        // is no migration for it. Nothing enforces that but review — this at least pins the shape.
        assertTrue(AppChannel.entries.all { it.id.matches(Regex("[a-z][a-z_]*")) })
    }
}
