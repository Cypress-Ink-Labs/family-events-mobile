package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.core.UserId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryAuthRepository : AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val sessionState: Flow<SessionState> = state

    override suspend fun restoreSession() {
        state.value = SessionState.SignedOut
    }

    override suspend fun signIn(email: String, password: String) {
        state.value = SessionState.SignedIn(UserId(email.trim().ifBlank { "fixture-user" }))
    }

    override suspend fun signUp(email: String, password: String) = signIn(email, password)
    override suspend fun resetPassword(email: String) = Unit
    override suspend fun signOut() {
        state.value = SessionState.SignedOut
    }
}

class InMemoryProfileRepository : ProfileRepository {
    private val profiles = MutableStateFlow<Map<String, ProfileContext>>(emptyMap())

    override fun observeProfile(userId: UserId): Flow<ProfileContext?> = profiles.map { it[userId.rawValue] }

    override suspend fun currentContext(userId: UserId): ProfileContext =
        profiles.value[userId.rawValue] ?: ProfileContext(userId, CityId("chicago"), 7, notificationsEnabled = false)

    override suspend fun updateContext(userId: UserId, cityId: CityId?, kidAge: Int?) {
        profiles.update { current ->
            current + (userId.rawValue to currentContext(userId).copy(currentCityId = cityId, kidAge = kidAge))
        }
    }

    override suspend fun updateNotificationPreference(userId: UserId, enabled: Boolean) {
        profiles.update { current ->
            current + (userId.rawValue to currentContext(userId).copy(notificationsEnabled = enabled))
        }
    }

    override suspend fun deleteAccount(userId: UserId) {
        profiles.update { it - userId.rawValue }
    }
}

class InMemoryEventRepository : EventRepository {
    private val events = MutableStateFlow(seedEvents())

    override fun observePlanEvents(userId: UserId, cityId: CityId?): Flow<List<PlanEventRowDto>> =
        events.map { rows ->
            rows.filter { cityId == null || it.cityId == cityId }
                .take(6)
                .mapIndexed { index, event -> PlanEventRowDto(event, section = if (index == 0) "Hero" else "Saturday", rank = index) }
        }

    override fun observeEventList(query: EventQuery): Flow<List<EventDto>> =
        events.map { rows ->
            rows.filter { query.cityId == null || it.cityId == query.cityId }
                .filter { query.search.isNullOrBlank() || it.title.contains(query.search, ignoreCase = true) }
                .drop(query.offset)
                .take(query.limit)
        }

    override fun observeEventDetail(id: EventId): Flow<EventDto?> = events.map { rows -> rows.firstOrNull { it.id == id } }

    override suspend fun refreshPlan(userId: UserId, cityId: CityId?) = Unit
    override suspend fun refreshEventList(query: EventQuery) = Unit
    override suspend fun refreshEventDetail(id: EventId) = Unit

    private fun seedEvents() = listOf(
        EventDto(
            id = EventId("demo-library-storytime"),
            title = "Neighborhood Storytime",
            description = "Songs, picture books, and a small craft for younger kids.",
            startsAt = Instant.parse("2026-05-16T15:00:00Z"),
            endsAt = Instant.parse("2026-05-16T16:00:00Z"),
            venueName = "Family Library",
            address = "100 Main St",
            imageUrl = null,
            sourceUrl = "https://familyevents.app/share/demo-library-storytime",
            cityId = CityId("chicago"),
            coordinate = GeoCoordinate(41.8818, -87.6231),
            tags = listOf(EventTagDto("free", "Free"), EventTagDto("indoor", "Indoor")),
        ),
        EventDto(
            id = EventId("demo-park-music"),
            title = "Park Music Hour",
            description = "A low-key outdoor concert with room for strollers.",
            startsAt = Instant.parse("2026-05-16T19:00:00Z"),
            endsAt = Instant.parse("2026-05-16T20:00:00Z"),
            venueName = "Civic Park",
            address = "200 Park Ave",
            imageUrl = null,
            sourceUrl = "https://familyevents.app/share/demo-park-music",
            cityId = CityId("chicago"),
            coordinate = GeoCoordinate(41.882, -87.62),
            tags = listOf(EventTagDto("music", "Music"), EventTagDto("outdoor", "Outdoor")),
        ),
    )
}

class InMemoryFavoriteRepository(private val events: EventRepository) : FavoriteRepository {
    private val favorites = MutableStateFlow<Set<EventId>>(emptySet())

    override fun observeFavorites(userId: UserId): Flow<List<FavoriteDto>> =
        favorites.map { ids -> ids.map { FavoriteDto(it, userId, Instant.now()) } }

    override fun observeFavoriteIds(userId: UserId): Flow<Set<EventId>> = favorites

    override suspend fun favorite(userId: UserId, eventId: EventId) {
        favorites.update { it + eventId }
    }

    override suspend fun unfavorite(userId: UserId, eventId: EventId) {
        favorites.update { it - eventId }
    }
}

class InMemoryCityRepository : CityRepository {
    private val cities = MutableStateFlow(listOf(CityDto(CityId("chicago"), "Chicago", "IL")))
    override fun observeCities(): Flow<List<CityDto>> = cities
    override suspend fun cityName(id: CityId): String? = cities.value.firstOrNull { it.id == id }?.name
    override suspend fun refreshCities() = Unit
}

class InMemoryWeatherRepository : WeatherRepository {
    private val forecast = MutableStateFlow(listOf(WeatherSnapshotDto("2026-05-16", "Mild, possible afternoon clouds", 72, 0.15)))
    override fun observeForecast(cityId: CityId): Flow<List<WeatherSnapshotDto>> = forecast
    override suspend fun refreshForecast(cityId: CityId) = Unit
}

class RepositoryGraph {
    val authRepository: AuthRepository = InMemoryAuthRepository()
    val eventRepository: EventRepository = InMemoryEventRepository()
    val favoriteRepository: FavoriteRepository = InMemoryFavoriteRepository(eventRepository)
    val profileRepository: ProfileRepository = InMemoryProfileRepository()
    val cityRepository: CityRepository = InMemoryCityRepository()
    val weatherRepository: WeatherRepository = InMemoryWeatherRepository()
}
