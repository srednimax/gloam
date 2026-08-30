package app.gloam.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import app.gloam.R

/**
 * The notification channels this app owns.
 *
 * **A channel is a separate mute, and that is the only reason to have more than one.** Ask of every
 * pair: would a user who silences A be annoyed to discover it also silenced B? If yes, they are two
 * channels. If no, one channel with two kinds of text in it is simpler and easier to live with.
 *
 * **The importance is per entry**, and the direction is what makes the choice permanent: Android
 * lets the user lower a channel and never lets the app raise one again.
 *
 * Kotlin note: enum entries carrying constructor arguments make this a small lookup table rather
 * than the bare constants a JS enum gives you — the same shape as `TopLevelDestination`.
 *
 * @param id what Android stores the channel under. **Never change one**: a renamed id is a new
 *   channel, and every user's mute silently reverts to unmuted.
 * @param nameRes the channel's name in the phone's notification settings. `ChannelsTest` holds the
 *   `channel_<id>_name` / `channel_<id>_description` convention these follow.
 * @param importance what the channel is *created* at, and therefore the ceiling the user can lower
 *   from. Never read it back to make a decision — read the live value from the system instead.
 * @param ongoing whether this channel exists to carry a foreground service's status notification
 *   rather than to alert anybody. See [AppChannel.Shade].
 */
enum class AppChannel(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val importance: Int,
    val ongoing: Boolean = false,
) {
    /**
     * The shade's foreground-service notification.
     *
     * **`IMPORTANCE_LOW`, and this is the one case where starting below `DEFAULT` is right.** The
     * usual rule — never create a channel quiet, because the user can lower it and the app can never
     * raise it — is about notifications that arrive to tell somebody something. This one never
     * arrives; it is permanently on screen for as long as the shade is, because Android requires a
     * foreground service to carry a visible notification. At `DEFAULT` it would make a sound and
     * peek a heads-up banner the moment the user dims their screen, which is both useless and
     * exactly the wrong moment for it.
     *
     * It is also the user's way out. Someone who dims to 95% and then cannot find the app still has
     * this notification and its Stop action, so it is deliberately not dismissible.
     */
    Shade(
        "shade",
        R.string.channel_shade_name,
        R.string.channel_shade_description,
        NotificationManager.IMPORTANCE_LOW,
        ongoing = true,
    ),
}

/**
 * Creates every channel, idempotently.
 *
 * `createNotificationChannel` on an id that already exists updates the name and description and
 * leaves the user's importance and their mute alone — which is exactly right, and is why calling
 * this on every launch is safe rather than merely harmless. It is also what makes a channel's name
 * follow an in-app language change.
 */
fun Context.ensureNotificationChannels() {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    for (channel in AppChannel.entries) {
        manager.createNotificationChannel(
            NotificationChannel(channel.id, getString(channel.nameRes), channel.importance).apply {
                description = getString(channel.descriptionRes)
            },
        )
    }
}

/**
 * **Can this channel's notification still appear?** The other half of [notificationsAllowed].
 *
 * Android lets the user lower a channel to `IMPORTANCE_NONE`, which blocks it while leaving the
 * app-wide permission on — so a screen that reads only the permission reports a working escape hatch
 * that the user cannot see. The warning on the dim screen makes a safety claim, and a false negative
 * there is somebody at maximum darkness with no Stop button and nothing saying so.
 *
 * **A channel that does not exist yet reads as `true`, and that is right rather than a gap**: nothing
 * can have been muted before it was created, and [ensureNotificationChannels] creates it at
 * [AppChannel.importance] on the service's first `onCreate`.
 *
 * Read live, never cached — the fix for a muted channel is a settings screen this app hands the user
 * off to, so the answer changes while the app is in the background.
 */
fun Context.channelCanAppear(channel: AppChannel): Boolean {
    val manager = getSystemService(NotificationManager::class.java) ?: return true
    val importance = manager.getNotificationChannel(channel.id)?.importance ?: return true
    return importance != NotificationManager.IMPORTANCE_NONE
}
