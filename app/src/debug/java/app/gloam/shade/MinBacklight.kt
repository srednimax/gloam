package app.gloam.shade

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The debug half of the lowest-backlight seam: a switch between the shipped [MIN_BACKLIGHT] and the
 * **floor**, so Phase 2b's biggest lever can be judged on a real page rather than on paper.
 *
 * **Debug builds start at the floor, since 2026-09-13.** The floor is what is being judged, night after
 * night, and a switch that went back to shipped whenever HyperOS killed the process was quietly handing
 * back readings of the wrong thing. Below `MIN_BACKLIGHT` the escape hatches get darker too: the
 * notification shade and quick settings read back our override unchanged (`MIN_BACKLIGHT`'s R5). That
 * cost is accepted on the development phone, where the person carrying it chose it. The release half
 * is still the constant, so nobody else pays it before 2b decides.
 *
 * **Still in memory, never in DataStore.** The switch back to shipped lasts until the process dies,
 * and then the floor returns. Nothing here survives into a build that is not this one.
 *
 * Kotlin note: a `MutableStateFlow` is a value with subscribers, closest to RxJS's `BehaviorSubject`:
 * it always holds a current value, and a new collector receives that value immediately.
 */
object DebugMinBacklight {
    /** The development panel's floor, `mScreenBrightnessRangeMinimum`: 2.0 nits by R1's fit. */
    const val FLOOR = 6.83661E-4f

    val value = MutableStateFlow(FLOOR)
}

internal val minBacklight: Flow<Float> = DebugMinBacklight.value
