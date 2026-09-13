package app.gloam.shade

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The debug half of the lowest-backlight seam: a switch between the shipped [MIN_BACKLIGHT] and
 * [PREVIOUS], the value it replaced.
 *
 * **Kept for the by-eye reading that is still owed.** Since 2026-09-13 [MIN_BACKLIGHT] is the panel's
 * floor, and below `0.01f` the escape hatches get darker too: the notification shade and quick
 * settings read back our override unchanged (`MIN_BACKLIGHT`'s R5). If *Stop* turns out to be
 * unfindable at 2.0 nits, this is the way back up to 6.64 on the development phone without a rebuild,
 * and the way to see what the change bought.
 *
 * **In memory, never in DataStore.** It starts at the shipped value, and a switch up lasts until the
 * process dies.
 *
 * Kotlin note: a `MutableStateFlow` is a value with subscribers, closest to RxJS's `BehaviorSubject`:
 * it always holds a current value, and a new collector receives that value immediately.
 */
object DebugMinBacklight {
    /** What [MIN_BACKLIGHT] was until 2026-09-13: 6.64 nits on the development panel, set by R2. */
    const val PREVIOUS = 0.01f

    val value = MutableStateFlow(MIN_BACKLIGHT)
}

internal val minBacklight: Flow<Float> = DebugMinBacklight.value
