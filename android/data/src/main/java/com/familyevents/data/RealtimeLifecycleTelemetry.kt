package com.familyevents.data

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

data class RealtimeLifecycleSnapshot(
    val activeSubscriptions: Int,
    val attachCount: Int,
    val detachCount: Int,
    val reconnectCount: Int,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val hasLeakedSubscriptions: Boolean = activeSubscriptions != 0
    val attachRatePerMinute: Double = ratePerMinute(attachCount)
    val detachRatePerMinute: Double = ratePerMinute(detachCount)
    val reconnectRatePerMinute: Double = ratePerMinute(reconnectCount)
    val batteryNetworkImpactSummary: String =
        "active=$activeSubscriptions, attaches=$attachCount, detaches=$detachCount, reconnects=$reconnectCount, reconnects_per_min=$reconnectRatePerMinute"

    private fun ratePerMinute(count: Int): Double {
        val elapsedMillis = max(updatedAtMillis - startedAtMillis, 1L)
        return count.toDouble() / elapsedMillis.toDouble() * 60_000.0
    }
}

class RealtimeLifecycleTelemetry(
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val startedAtMillis = clockMillis()
    private val updatedAtMillis = AtomicLong(startedAtMillis)
    private val activeSubscriptions = AtomicInteger(0)
    private val attachCount = AtomicInteger(0)
    private val detachCount = AtomicInteger(0)
    private val reconnectCount = AtomicInteger(0)

    fun recordAttach() {
        attachCount.incrementAndGet()
        activeSubscriptions.incrementAndGet()
        updatedAtMillis.set(clockMillis())
    }

    fun recordDetach() {
        var detached = false
        activeSubscriptions.updateAndGet {
            if (it > 0) {
                detached = true
                it - 1
            } else {
                0
            }
        }
        if (detached) {
            detachCount.incrementAndGet()
        }
        updatedAtMillis.set(clockMillis())
    }

    fun recordReconnect() {
        reconnectCount.incrementAndGet()
        updatedAtMillis.set(clockMillis())
    }

    fun snapshot(): RealtimeLifecycleSnapshot {
        val now = clockMillis()
        val updatedAt = max(updatedAtMillis.get(), now)
        return RealtimeLifecycleSnapshot(
            activeSubscriptions = activeSubscriptions.get(),
            attachCount = attachCount.get(),
            detachCount = detachCount.get(),
            reconnectCount = reconnectCount.get(),
            startedAtMillis = startedAtMillis,
            updatedAtMillis = updatedAt,
        )
    }
}
