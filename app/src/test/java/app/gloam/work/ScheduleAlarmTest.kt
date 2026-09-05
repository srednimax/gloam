package app.gloam.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The alarm chain, swept rather than argued about.
 *
 * **The property that matters is negative**: no hop overshoots the on-instant. A chain that
 * overshoots does not merely arrive late, it arrives *after* the window it was aiming at - and the
 * schedule's receiver would then decline the very night it was armed for, silently, on a screen
 * still saying the schedule is on. That is the failure this arithmetic exists to make impossible,
 * and it is the one a handful of examples would not catch.
 *
 * The second property is that the chain **terminates cheaply**. An arrangement that never overshoots
 * is trivially satisfiable by hopping one millisecond at a time; what makes this one shippable is
 * that a whole day closes in a handful of wakeups.
 */
class ScheduleAlarmTest {
    private val minute = 60_000L
    private val hour = 60 * minute

    /** Every gap worth sweeping, from a day out down to a millisecond. */
    private val gaps =
        buildList {
            var gap = 25 * hour
            while (gap > 0) {
                add(gap)
                gap -= minute
            }
            addAll(listOf(1L, 999L, 60_000L, 5 * minute, 5 * minute + 1, 140 * minute, 140 * minute - 1))
        }

    @Test
    fun `no hop overshoots the instant it is aiming at`() {
        for (gap in gaps) {
            val hop = hopFor(gap)
            if (gap <= 5 * minute) continue // the final hop aims *at* the target; see below
            assertTrue(
                "gap=$gap hop=$hop arrives at ${latestArrival(hop)}, past the window's opening",
                latestArrival(hop) <= gap,
            )
        }
    }

    @Test
    fun `every hop is positive and shorter than the gap it is closing`() {
        for (gap in gaps) {
            val hop = hopFor(gap)
            assertTrue("gap=$gap produced a non-positive hop $hop", hop > 0)
            assertTrue("gap=$gap produced a hop $hop past the gap", hop <= gap)
        }
    }

    /**
     * **The last hop is the only one allowed to arrive late**, and this bounds by how much. Five
     * minutes of futurity buys a window of three minutes forty-five, which is the whole of the
     * imprecision the feature ships with and the number the short-window warning is written from.
     */
    @Test
    fun `the final hop aims at the target and overshoots by at most three quarters of it`() {
        for (gap in listOf(1L, 1000L, minute, 4 * minute, 5 * minute)) {
            assertEquals("the final hop aims at the target", gap, hopFor(gap))
            assertTrue("overshoot is bounded", latestArrival(gap) - gap <= 3 * minute + 45_000)
        }
    }

    /**
     * The chain closes a day in a handful of wakeups, taking the **worst case at every step** - the
     * alarm arriving at the earliest end of its window, which is the arrival that leaves the most
     * still to close.
     */
    @Test
    fun `a whole day closes in five hops`() {
        for (start in listOf(24 * hour, 25 * hour, 14 * hour, 2 * hour, 61 * minute)) {
            var gap = start
            var hops = 0
            while (gap > 5 * minute) {
                gap -= hopFor(gap) // the earliest arrival: everything else leaves less to close
                hops++
                assertTrue("$start did not converge: stuck at $gap", hops < 20)
            }
            assertTrue("$start took $hops hops", hops <= 5)
        }
    }

    @Test
    fun `the two regimes meet where the cap starts binding`() {
        assertEquals("above the boundary, leave an hour of room", 140 * minute - hour, hopFor(140 * minute))
        assertEquals("below it, four sevenths", (140 * minute - 1) * 4 / 7, hopFor(140 * minute - 1))
        // Both sides of the boundary have to satisfy the same safety property, which is the point of
        // choosing the boundary where they agree rather than where they look tidy.
        assertTrue(latestArrival(hopFor(140 * minute)) <= 140 * minute)
        assertTrue(latestArrival(hopFor(140 * minute - 1)) <= 140 * minute - 1)
    }

    @Test
    fun `a gap that has already passed is answered rather than thrown at`() {
        assertEquals(0L, hopFor(0L))
        assertEquals(-1L, hopFor(-1L))
    }
}
