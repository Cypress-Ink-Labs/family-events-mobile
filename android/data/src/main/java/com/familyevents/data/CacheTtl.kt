package com.familyevents.data

import java.util.concurrent.ConcurrentHashMap

class CacheTtlTracker {
    private val timestamps = ConcurrentHashMap<String, Long>()

    fun isStale(key: String, ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        val last = timestamps[key] ?: return true
        return System.currentTimeMillis() - last > ttlMs
    }

    fun markRefreshed(key: String) {
        timestamps[key] = System.currentTimeMillis()
    }

    fun clear() {
        timestamps.clear()
    }

    companion object {
        const val DEFAULT_TTL_MS = 60_000L
        const val PLAN_TTL_MS = 120_000L
        const val COMMENTS_POLL_MS = 15_000L
    }
}
