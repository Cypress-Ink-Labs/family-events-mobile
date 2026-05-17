package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EnvConfig
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.core.UserId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface SessionStore {
    suspend fun readUserId(): UserId?
    suspend fun writeUserId(userId: UserId?)
}

class MemorySessionStore : SessionStore {
    private var userId: UserId? = null
    override suspend fun readUserId(): UserId? = userId
    override suspend fun writeUserId(userId: UserId?) {
        this.userId = userId
    }
}

class LocalAuthRepository(private val sessionStore: SessionStore) : AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val sessionState: Flow<SessionState> = state

    override suspend fun restoreSession() {
        state.value = sessionStore.readUserId()?.let(SessionState::SignedIn) ?: SessionState.SignedOut
    }

    override suspend fun signIn(email: String, password: String) {
        val userId = UserId(email.trim().lowercase().ifBlank { "fixture-user" })
        sessionStore.writeUserId(userId)
        state.value = SessionState.SignedIn(userId)
    }

    override suspend fun signUp(email: String, password: String) = signIn(email, password)
    override suspend fun resetPassword(email: String) = Unit

    override suspend fun signOut() {
        sessionStore.writeUserId(null)
        state.value = SessionState.SignedOut
    }
}

class RoomBackedEventRepository(
    private val eventDao: EventDao,
    private val planDao: PlanDao,
) : EventRepository {
    override fun observePlanEvents(userId: UserId, cityId: CityId?): Flow<List<PlanEventRowDto>> =
        combine(
            planDao.observePlan(userId.rawValue),
            eventDao.observeEvents(cityId?.rawValue, limit = 100, offset = 0),
        ) { planRows, eventRows ->
            val eventsById = eventRows.map { it.toDto() }.associateBy { it.id.rawValue }
            if (planRows.isEmpty()) {
                eventRows.take(6).mapIndexed { index, row ->
                    PlanEventRowDto(row.toDto(), section = if (index == 0) "Hero" else "Saturday", rank = index)
                }
            } else {
                planRows.mapNotNull { row ->
                    eventsById[row.eventId]?.let { event -> PlanEventRowDto(event, row.section, row.rank) }
                }
            }
        }.withSeedPlan(cityId)

    override fun observeEventList(query: EventQuery): Flow<List<EventDto>> =
        eventDao.observeEvents(query.cityId?.rawValue, limit = 250, offset = 0)
            .map { rows ->
                rows.map { it.toDto() }
                    .ifEmpty { seedEvents().filterByCity(query.cityId) }
                    .filter { event -> query.search.isNullOrBlank() || event.title.contains(query.search, ignoreCase = true) }
                    .filter { event -> query.tagIds.isEmpty() || event.tags.any { it.id in query.tagIds } }
                    .filter { event -> query.dateKey == null || event.startsAt.toString().startsWith(query.dateKey) }
                    .drop(query.offset)
                    .take(query.limit)
            }

    override fun observeEventDetail(id: EventId): Flow<EventDto?> =
        eventDao.observeEvent(id.rawValue).map { row -> row?.toDto() ?: seedEvents().firstOrNull { it.id == id } }

    override suspend fun refreshPlan(userId: UserId, cityId: CityId?) {
        val events = seedEvents().filterByCity(cityId)
        eventDao.upsert(events.map { it.toEntity() })
        planDao.deleteForUser(userId.rawValue)
        planDao.upsert(events.take(6).mapIndexed { index, event ->
            CachedPlanEventEntity(
                userId = userId.rawValue,
                eventId = event.id.rawValue,
                section = if (index == 0) "Hero" else "Saturday",
                rank = index,
            )
        })
    }

    override suspend fun refreshEventList(query: EventQuery) {
        eventDao.upsert(seedEvents().filterByCity(query.cityId).map { it.toEntity() })
    }

    override suspend fun refreshEventDetail(id: EventId) {
        seedEvents().firstOrNull { it.id == id }?.let { eventDao.upsert(listOf(it.toEntity())) }
    }
}

class RoomBackedFavoriteRepository(private val favoriteDao: FavoriteDao) : FavoriteRepository {
    override fun observeFavorites(userId: UserId): Flow<List<FavoriteDto>> =
        favoriteDao.observeFavorites(userId.rawValue).map { rows -> rows.map { it.toDto() } }

    override fun observeFavoriteIds(userId: UserId): Flow<Set<EventId>> =
        observeFavorites(userId).map { rows -> rows.mapTo(mutableSetOf()) { it.eventId } }

    override suspend fun favorite(userId: UserId, eventId: EventId) {
        favoriteDao.upsert(CachedFavoriteEntity(userId.rawValue, eventId.rawValue, Instant.now().toString()))
    }

    override suspend fun unfavorite(userId: UserId, eventId: EventId) {
        favoriteDao.delete(userId.rawValue, eventId.rawValue)
    }
}

class RoomBackedProfileRepository(private val profileDao: ProfileDao) : ProfileRepository {
    override fun observeProfile(userId: UserId): Flow<ProfileContext?> =
        profileDao.observeProfile(userId.rawValue).map { it?.toDto() }

    override suspend fun currentContext(userId: UserId): ProfileContext =
        ProfileContext(userId, CityId("chicago"), kidAge = 7, notificationsEnabled = false)

    override suspend fun updateContext(userId: UserId, cityId: CityId?, kidAge: Int?) {
        profileDao.upsert(CachedProfileEntity(userId.rawValue, cityId?.rawValue, kidAge, notificationsEnabled = false))
    }

    override suspend fun updateNotificationPreference(userId: UserId, enabled: Boolean) {
        val current = currentContext(userId)
        profileDao.upsert(CachedProfileEntity(userId.rawValue, current.currentCityId?.rawValue, current.kidAge, enabled))
    }

    override suspend fun deleteAccount(userId: UserId) {
        profileDao.upsert(CachedProfileEntity(userId.rawValue, null, null, notificationsEnabled = false))
    }
}

class RoomBackedCityRepository(private val cityDao: CityDao) : CityRepository {
    override fun observeCities(): Flow<List<CityDto>> =
        cityDao.observeCities().map { rows -> rows.map { it.toDto() }.ifEmpty { listOf(CityDto(CityId("chicago"), "Chicago", "IL")) } }

    override suspend fun cityName(id: CityId): String? = cityDao.cityName(id.rawValue) ?: if (id.rawValue == "chicago") "Chicago" else null

    override suspend fun refreshCities() {
        cityDao.upsert(listOf(CachedCityEntity("chicago", "Chicago", "IL")))
    }
}

class RoomBackedWeatherRepository(private val weatherDao: WeatherDao) : WeatherRepository {
    override fun observeForecast(cityId: CityId): Flow<List<WeatherSnapshotDto>> =
        weatherDao.observeForecast(cityId.rawValue).map { rows -> rows.map { it.toDto() }.ifEmpty { seedForecast() } }

    override suspend fun refreshForecast(cityId: CityId) {
        weatherDao.upsert(seedForecast().map { it.toEntity(cityId) })
    }
}

private fun Flow<List<PlanEventRowDto>>.withSeedPlan(cityId: CityId?): Flow<List<PlanEventRowDto>> =
    map { rows ->
        rows.ifEmpty {
            seedEvents().filterByCity(cityId).take(6).mapIndexed { index, event ->
                PlanEventRowDto(event, section = if (index == 0) "Hero" else "Saturday", rank = index)
            }
        }
    }

private fun List<EventDto>.filterByCity(cityId: CityId?): List<EventDto> = filter { cityId == null || it.cityId == cityId }

private fun CachedEventEntity.toDto(): EventDto = EventDto(
    id = EventId(id),
    title = title,
    description = description,
    startsAt = Instant.parse(startsAt),
    endsAt = endsAt?.let(Instant::parse),
    venueName = venueName,
    address = address,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    cityId = CityId(cityId),
    coordinate = latitude?.let { lat -> longitude?.let { lng -> GeoCoordinate(lat, lng) } },
)

private fun EventDto.toEntity(): CachedEventEntity = CachedEventEntity(
    id = id.rawValue,
    title = title,
    description = description,
    startsAt = startsAt.toString(),
    endsAt = endsAt?.toString(),
    venueName = venueName,
    address = address,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    cityId = cityId.rawValue,
    latitude = coordinate?.latitude,
    longitude = coordinate?.longitude,
)

private fun CachedFavoriteEntity.toDto(): FavoriteDto =
    FavoriteDto(EventId(eventId), UserId(userId), Instant.parse(createdAt))

private fun CachedProfileEntity.toDto(): ProfileContext =
    ProfileContext(UserId(userId), currentCityId?.let(::CityId), kidAge, notificationsEnabled)

private fun CachedCityEntity.toDto(): CityDto = CityDto(CityId(id), name, region)

private fun CachedWeatherEntity.toDto(): WeatherSnapshotDto =
    WeatherSnapshotDto(dateKey, summary, temperatureHighF, precipitationChance)

private fun WeatherSnapshotDto.toEntity(cityId: CityId): CachedWeatherEntity =
    CachedWeatherEntity(cityId.rawValue, dateKey, summary, temperatureHighF, precipitationChance)

private fun seedForecast() = listOf(WeatherSnapshotDto("2026-05-16", "Mild, possible afternoon clouds", 72, 0.15))

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
