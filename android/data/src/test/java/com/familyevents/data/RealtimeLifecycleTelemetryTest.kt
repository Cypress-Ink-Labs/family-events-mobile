package com.familyevents.data

import com.familyevents.core.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeLifecycleTelemetryTest {
    @Test
    fun roomBackedFavoritesReportNoLeakedSubscriptionAfterCollectorStops() = runTest {
        val telemetry = RealtimeLifecycleTelemetry(clockMillis = { testScheduler.currentTime })
        val repository = RoomBackedFavoriteRepository(FakeFavoriteDao(), realtimeTelemetry = telemetry)

        val job: Job = launch {
            repository.observeFavorites(UserId("u_1")).collect {}
        }
        advanceUntilIdle()
        assertEquals(1, telemetry.snapshot().activeSubscriptions)

        job.cancelAndJoin()
        advanceUntilIdle()

        val snapshot = telemetry.snapshot()
        assertEquals(0, snapshot.activeSubscriptions)
        assertEquals(1, snapshot.attachCount)
        assertEquals(1, snapshot.detachCount)
        assertFalse(snapshot.hasLeakedSubscriptions)

        telemetry.recordDetach()
        assertEquals(1, telemetry.snapshot().detachCount)
    }

    @Test
    fun reconnectRateStaysBoundedForAuditWindow() {
        var now = 0L
        val telemetry = RealtimeLifecycleTelemetry(clockMillis = { now })

        now = 10_000
        telemetry.recordReconnect()
        now = 20_000
        telemetry.recordReconnect()
        now = 60_000

        val snapshot = telemetry.snapshot()
        assertEquals(2, snapshot.reconnectCount)
        assertTrue(snapshot.reconnectRatePerMinute <= 2.0)
        assertTrue(snapshot.batteryNetworkImpactSummary.contains("reconnects=2"))
    }
}

private class FakeFavoriteDao : FavoriteDao {
    private val rows = MutableStateFlow<List<CachedFavoriteEntity>>(emptyList())

    override fun observeFavorites(userId: String): Flow<List<CachedFavoriteEntity>> = rows

    override suspend fun favorite(userId: String, eventId: String): CachedFavoriteEntity? =
        rows.value.firstOrNull { it.userId == userId && it.eventId == eventId }

    override suspend fun upsert(favorite: CachedFavoriteEntity) {
        rows.value = rows.value.filterNot { it.userId == favorite.userId && it.eventId == favorite.eventId } + favorite
    }

    override suspend fun delete(userId: String, eventId: String) {
        rows.value = rows.value.filterNot { it.userId == userId && it.eventId == eventId }
    }
}
