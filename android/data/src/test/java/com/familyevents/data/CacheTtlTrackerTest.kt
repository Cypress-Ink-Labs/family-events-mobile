package com.familyevents.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheTtlTrackerTest {
    @Test
    fun staleBeforeFirstRefresh() {
        val tracker = CacheTtlTracker()
        assertTrue(tracker.isStale("events"))
    }

    @Test
    fun notStaleImmediatelyAfterRefresh() {
        val tracker = CacheTtlTracker()
        tracker.markRefreshed("events")
        assertFalse(tracker.isStale("events", ttlMs = 60_000))
    }

    @Test
    fun staleAfterTtlExceeded() {
        val tracker = CacheTtlTracker()
        tracker.markRefreshed("events")
        Thread.sleep(2)
        assertTrue(tracker.isStale("events", ttlMs = 1))
    }

    @Test
    fun notStaleWithMaxTtl() {
        val tracker = CacheTtlTracker()
        tracker.markRefreshed("events")
        assertFalse(tracker.isStale("events", ttlMs = Long.MAX_VALUE))
    }

    @Test
    fun clearResetsAllKeys() {
        val tracker = CacheTtlTracker()
        tracker.markRefreshed("a")
        tracker.markRefreshed("b")
        tracker.clear()
        assertTrue(tracker.isStale("a"))
        assertTrue(tracker.isStale("b"))
    }

    @Test
    fun independentKeysTrackedSeparately() {
        val tracker = CacheTtlTracker()
        tracker.markRefreshed("events")
        assertTrue(tracker.isStale("comments"))
        assertFalse(tracker.isStale("events", ttlMs = 60_000))
    }
}
