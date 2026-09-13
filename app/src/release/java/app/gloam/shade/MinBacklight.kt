package app.gloam.shade

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The release half of the lowest-backlight seam: the shipped [MIN_BACKLIGHT], and nothing that can
 * move it. The debug half is a switch for comparing it against the floor by eye; the reason the
 * split is a source set rather than `BuildConfig.DEBUG` is on `DebugSettings`.
 */
internal val minBacklight: Flow<Float> = flowOf(MIN_BACKLIGHT)
