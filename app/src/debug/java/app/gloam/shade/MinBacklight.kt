package app.gloam.shade

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The debug half of the lowest-backlight seam: a switch between the shipped [MIN_BACKLIGHT] and the
 * **floor**, so Phase 2b's biggest lever can be judged on a real page rather than on paper.
 *
 * **In memory, never in DataStore, and that is the safety property rather than laziness.** Below
 * `MIN_BACKLIGHT` the escape hatches get darker too — the notification shade and quick settings read
 * back our override unchanged (`MIN_BACKLIGHT`'s R5) — so an experimental value must not outlive the
 * process somebody set up to look at it. A kill, a reinstall or a reboot puts the shipped value back.
 *
 * Kotlin note: a `MutableStateFlow` is a value with subscribers, closest to RxJS's `BehaviorSubject`:
 * it always holds a current value, and a new collector receives that value immediately.
 */
object DebugMinBacklight {
    /** The development panel's floor, `mScreenBrightnessRangeMinimum`: 2.0 nits by R1's fit. */
    const val FLOOR = 6.83661E-4f

    val value = MutableStateFlow(MIN_BACKLIGHT)
}

internal val minBacklight: Flow<Float> = DebugMinBacklight.value
