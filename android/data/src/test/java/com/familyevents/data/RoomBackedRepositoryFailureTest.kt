package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomBackedRepositoryFailureTest {
    @Test
    fun refreshCitiesFallsBackToDefaultCityWhenRemoteFails() = runTest {
        val cityDao = FakeCityDao()
        val repository = RoomBackedCityRepository(cityDao, FailingConsumerApi)

        repository.refreshCities()

        assertEquals(listOf(CachedCityEntity("chicago", "Chicago", "IL")), cityDao.rows.value)
    }

    @Test
    fun refreshEventListFallsBackToSeedEventsWhenRemoteFails() = runTest {
        val eventDao = FakeEventDao()
        val repository = RoomBackedEventRepository(eventDao, FakePlanDao(), FailingConsumerApi)

        repository.refreshEventList(EventQuery(cityId = CityId("chicago")))

        assertTrue(eventDao.rows.value.any { it.id == "demo-library-storytime" })
    }

    @Test
    fun refreshPlanFallsBackToSeedPlanWhenRemoteFails() = runTest {
        val eventDao = FakeEventDao()
        val planDao = FakePlanDao()
        val repository = RoomBackedEventRepository(eventDao, planDao, FailingConsumerApi)

        repository.refreshPlan(UserId("user"), CityId("chicago"), kidAge = 7, lat = null, lng = null)

        assertTrue(eventDao.rows.value.any { it.id == "demo-library-storytime" })
        assertTrue(planDao.rows.value.any { it.eventId == "demo-library-storytime" })
    }

    @Test
    fun refreshEventDetailFallsBackToSeedEventWhenRemoteFails() = runTest {
        val eventDao = FakeEventDao()
        val repository = RoomBackedEventRepository(eventDao, FakePlanDao(), FailingConsumerApi)

        repository.refreshEventDetail(EventId("demo-library-storytime"))

        assertTrue(eventDao.rows.value.any { it.id == "demo-library-storytime" })
    }
}

private object FailingConsumerApi : SupabaseConsumerApi {
    override suspend fun signIn(email: String, password: String): PersistedSession = fail()
    override suspend fun signUp(email: String, password: String): PersistedSession = fail()
    override suspend fun signInWithIdToken(provider: String, idToken: String, nonce: String?): PersistedSession = fail()
    override suspend fun resetPassword(email: String) = fail<Unit>()
    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) = fail<Unit>()
    override suspend fun signOut() = fail<Unit>()
    override suspend fun cities(): List<CityDto> = fail()
    override suspend fun events(query: EventQuery): List<EventDto> = fail()
    override suspend fun event(id: EventId): EventDto? = fail()
    override suspend fun planEvents(userId: UserId, cityId: CityId?, kidAge: Int?, lat: Double?, lng: Double?): List<PlanEventRowDto> = fail()
    override suspend fun profile(userId: UserId): UserProfile? = fail()
    override suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile = fail()
    override suspend fun favorite(userId: UserId, eventId: EventId) = fail<Unit>()
    override suspend fun unfavorite(userId: UserId, eventId: EventId) = fail<Unit>()
    override suspend fun deleteAccount() = fail<Unit>()

    private fun <T> fail(): T = throw RuntimeException("remote unavailable")
}

private class FakeEventDao : EventDao {
    val rows = MutableStateFlow<List<CachedEventEntity>>(emptyList())

    override fun observeEvents(cityId: String?, limit: Int, offset: Int): Flow<List<CachedEventEntity>> =
        rows.map { events ->
            events
                .filter { cityId == null || it.cityId == cityId }
                .drop(offset)
                .take(limit)
        }

    override fun observeEvent(id: String): Flow<CachedEventEntity?> =
        rows.map { events -> events.firstOrNull { it.id == id } }

    override suspend fun upsert(events: List<CachedEventEntity>) {
        rows.value = (rows.value.associateBy { it.id } + events.associateBy { it.id }).values.toList()
    }
}

private class FakePlanDao : PlanDao {
    val rows = MutableStateFlow<List<CachedPlanEventEntity>>(emptyList())

    override fun observePlan(userId: String): Flow<List<CachedPlanEventEntity>> =
        rows.map { plans -> plans.filter { it.userId == userId }.sortedBy { it.rank } }

    override suspend fun deleteForUser(userId: String) {
        rows.value = rows.value.filterNot { it.userId == userId }
    }

    override suspend fun upsert(rows: List<CachedPlanEventEntity>) {
        val incomingKeys = rows.map { it.userId to it.eventId }.toSet()
        this.rows.value = this.rows.value.filterNot { it.userId to it.eventId in incomingKeys } + rows
    }
}

private class FakeCityDao : CityDao {
    val rows = MutableStateFlow<List<CachedCityEntity>>(emptyList())

    override fun observeCities(): Flow<List<CachedCityEntity>> = rows

    override suspend fun cityName(id: String): String? = rows.value.firstOrNull { it.id == id }?.name

    override suspend fun upsert(cities: List<CachedCityEntity>) {
        rows.value = (rows.value.associateBy { it.id } + cities.associateBy { it.id }).values.toList()
    }
}
