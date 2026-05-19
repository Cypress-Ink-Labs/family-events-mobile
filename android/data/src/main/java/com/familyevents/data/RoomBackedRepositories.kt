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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

interface SessionStore {
    suspend fun readSession(): PersistedSession?
    suspend fun writeSession(session: PersistedSession?)

    suspend fun readUserId(): UserId? = readSession()?.userId
    suspend fun writeUserId(userId: UserId?) {
        writeSession(userId?.let { PersistedSession(it) })
    }
    suspend fun readAccessToken(): String? = readSession()?.accessToken
}

data class PersistedSession(
    val userId: UserId,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

class MemorySessionStore : SessionStore {
    private var session: PersistedSession? = null
    override suspend fun readSession(): PersistedSession? = session
    override suspend fun writeSession(session: PersistedSession?) {
        this.session = session
    }
}

class LocalAuthRepository(
    private val sessionStore: SessionStore,
    private val api: SupabaseConsumerApi? = null,
) : AuthRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val sessionState: Flow<SessionState> = state

    override suspend fun restoreSession() {
        state.value = sessionStore.readUserId()?.let(SessionState::SignedIn) ?: SessionState.SignedOut
    }

    override suspend fun signIn(email: String, password: String) {
        val session = api?.signIn(email, password)
            ?: PersistedSession(UserId("00000000-0000-0000-0000-000000000000"))
        sessionStore.writeSession(session)
        state.value = SessionState.SignedIn(session.userId)
    }

    override suspend fun signUp(email: String, password: String) {
        val session = api?.signUp(email, password)
            ?: PersistedSession(UserId("00000000-0000-0000-0000-000000000000"))
        sessionStore.writeSession(session)
        state.value = SessionState.SignedIn(session.userId)
    }

    override suspend fun resetPassword(email: String) {
        api?.resetPassword(email)
    }

    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        api?.changePassword(email, currentPassword, newPassword)
    }

    override suspend fun signOut() {
        runCatching { api?.signOut() }
        sessionStore.writeSession(null)
        state.value = SessionState.SignedOut
    }

    override suspend fun invitesRequired(): Boolean = api?.invitesRequired() ?: true

    override suspend fun requestInvite(email: String, message: String?): Boolean =
        api?.requestInvite(email, message) ?: false
}

class RoomBackedEventRepository(
    private val eventDao: EventDao,
    private val planDao: PlanDao,
    private val api: SupabaseConsumerApi? = null,
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
                    .ifEmpty { seedEvents().filterByCity(query.cityId).ifEmpty { seedEvents() } }
                    .filter { event -> query.search.isNullOrBlank() || event.title.contains(query.search, ignoreCase = true) }
                    .filter { event -> query.tagIds.isEmpty() || event.tags.any { it.id in query.tagIds } }
                    .filter { event -> query.dateKey == null || event.startsAt.toString().startsWith(query.dateKey) }
                    .drop(query.offset)
                    .take(query.limit)
            }

    override fun observeEventDetail(id: EventId): Flow<EventDto?> =
        eventDao.observeEvent(id.rawValue).map { row -> row?.toDto() ?: seedEvents().firstOrNull { it.id == id } }

    override suspend fun refreshPlan(userId: UserId, cityId: CityId?, kidAge: Int?) {
        val remotePlan = api?.planEvents(userId, cityId, kidAge)
        val events = remotePlan?.map { it.event }.takeUnless { it.isNullOrEmpty() }
            ?: api?.events(EventQuery(cityId = cityId, limit = 50)).takeUnless { it.isNullOrEmpty() }
            ?: seedEvents().filterByCity(cityId)
        eventDao.upsert(events.map { it.toEntity() })
        planDao.deleteForUser(userId.rawValue)
        planDao.upsert(
            (remotePlan ?: events.take(6).mapIndexed { index, event ->
                PlanEventRowDto(event, section = if (index == 0) "Hero" else "Saturday", rank = index)
            }).map { row ->
                CachedPlanEventEntity(
                    userId = userId.rawValue,
                    eventId = row.event.id.rawValue,
                    section = row.section,
                    rank = row.rank,
                )
            },
        )
    }

    override suspend fun refreshEventList(query: EventQuery) {
        val events = api?.events(query).takeUnless { it.isNullOrEmpty() } ?: seedEvents().filterByCity(query.cityId)
        eventDao.upsert(events.map { it.toEntity() })
    }

    override suspend fun refreshEventDetail(id: EventId) {
        (api?.event(id) ?: seedEvents().firstOrNull { it.id == id })?.let { eventDao.upsert(listOf(it.toEntity())) }
    }
}

class RoomBackedFavoriteRepository(
    private val favoriteDao: FavoriteDao,
    private val api: SupabaseConsumerApi? = null,
    val realtimeTelemetry: RealtimeLifecycleTelemetry = RealtimeLifecycleTelemetry(),
) : FavoriteRepository {
    override fun observeFavorites(userId: UserId): Flow<List<FavoriteDto>> =
        favoriteDao.observeFavorites(userId.rawValue)
            .onStart { realtimeTelemetry.recordAttach() }
            .onCompletion { realtimeTelemetry.recordDetach() }
            .map { rows -> rows.map { it.toDto() } }

    override fun observeFavoriteIds(userId: UserId): Flow<Set<EventId>> =
        observeFavorites(userId).map { rows -> rows.mapTo(mutableSetOf()) { it.eventId } }

    override suspend fun favorite(userId: UserId, eventId: EventId) {
        val row = CachedFavoriteEntity(userId.rawValue, eventId.rawValue, Instant.now().toString())
        favoriteDao.upsert(row)
        try {
            api?.favorite(userId, eventId)
        } catch (error: Throwable) {
            favoriteDao.delete(userId.rawValue, eventId.rawValue)
            throw error
        }
    }

    override suspend fun unfavorite(userId: UserId, eventId: EventId) {
        val previous = favoriteDao.favorite(userId.rawValue, eventId.rawValue)
        favoriteDao.delete(userId.rawValue, eventId.rawValue)
        try {
            api?.unfavorite(userId, eventId)
        } catch (error: Throwable) {
            previous?.let { favoriteDao.upsert(it) }
            throw error
        }
    }
}

class RoomBackedProfileRepository(
    private val profileDao: ProfileDao,
    private val api: SupabaseConsumerApi? = null,
) : ProfileRepository {
    override fun observeProfile(userId: UserId): Flow<ProfileContext?> =
        profileDao.observeProfile(userId.rawValue).map { it?.toProfile()?.toContext() }

    override suspend fun currentContext(userId: UserId): ProfileContext =
        profile(userId).toContext()

    override suspend fun profile(userId: UserId): UserProfile = try {
        api?.profile(userId)?.also { profileDao.upsert(it.toEntity()) }
            ?: profileDao.profile(userId.rawValue)?.toProfile()
            ?: defaultProfile(userId)
    } catch (error: Throwable) {
        profileDao.profile(userId.rawValue)?.toProfile() ?: defaultProfile(userId)
    }

    override suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile {
        val local = profile(userId).copy(
            displayName = update.displayName,
            currentCityId = update.currentCityId,
            childName = update.childName,
            childAge = update.childAge,
        )
        val remote = api?.updateProfile(userId, update)
        if (remote != null) {
            profileDao.upsert(remote.toEntity())
            return remote
        }
        profileDao.upsert(local.toEntity())
        return local
    }

    override suspend fun updateContext(userId: UserId, cityId: CityId?, kidAge: Int?) {
        val current = profile(userId)
        updateProfile(
            userId,
            UserProfileUpdate(
                displayName = current.displayName,
                currentCityId = cityId,
                childName = current.childName,
                childAge = kidAge,
            ),
        )
    }

    override suspend fun updateNotificationPreference(userId: UserId, enabled: Boolean) {
        val current = profile(userId)
        val updated = current.copy(notificationsEnabled = enabled)
        profileDao.upsert(updated.toEntity())
    }

    override suspend fun deleteAccount(userId: UserId) {
        api?.deleteAccount()
        profileDao.upsert(defaultProfile(userId).copy(currentCityId = null, childAge = null).toEntity())
    }
}

class RoomBackedCityRepository(
    private val cityDao: CityDao,
    private val api: SupabaseConsumerApi? = null,
) : CityRepository {
    override fun observeCities(): Flow<List<CityDto>> =
        cityDao.observeCities().map { rows -> rows.map { it.toDto() }.ifEmpty { listOf(CityDto(CityId("chicago"), "Chicago", "IL")) } }

    override suspend fun cityName(id: CityId): String? = cityDao.cityName(id.rawValue) ?: if (id.rawValue == "chicago") "Chicago" else null

    override suspend fun refreshCities() {
        cityDao.upsert(api?.cities()?.takeIf { it.isNotEmpty() }?.map { it.toEntity() } ?: listOf(CachedCityEntity("chicago", "Chicago", "IL")))
    }
}

class RoomBackedWeatherRepository(private val weatherDao: WeatherDao) : WeatherRepository {
    override fun observeForecast(cityId: CityId): Flow<List<WeatherSnapshotDto>> =
        weatherDao.observeForecast(cityId.rawValue).map { rows -> rows.map { it.toDto() }.ifEmpty { seedForecast() } }

    override suspend fun refreshForecast(cityId: CityId) {
        weatherDao.upsert(seedForecast().map { it.toEntity(cityId) })
    }
}

class SupabaseRatingRepository(private val api: SupabaseConsumerApi? = null) : RatingRepository {
    override suspend fun userRating(userId: UserId, eventId: EventId): RatingDto? =
        api?.userRating(userId, eventId)

    override suspend fun upsertRating(userId: UserId, eventId: EventId, score: Int): RatingDto =
        api?.upsertRating(userId, eventId, score.coerceIn(1, 5))
            ?: RatingDto("local-${eventId.rawValue}", userId, eventId, score.coerceIn(1, 5), Instant.now())
}

class SupabaseCommentRepository(private val api: SupabaseConsumerApi? = null) : CommentRepository {
    override suspend fun comments(eventId: EventId): List<CommentDto> =
        api?.comments(eventId) ?: emptyList()

    override suspend fun addComment(userId: UserId, eventId: EventId, body: String): CommentDto =
        api?.addComment(userId, eventId, body)
            ?: CommentDto(
                id = "local-${Instant.now().toEpochMilli()}",
                userId = userId,
                eventId = eventId,
                body = body,
                isApproved = true,
                isFlagged = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                authorDisplayName = null,
                authorAvatarUrl = null,
            )
}

class SupabaseAdminRepository(private val api: SupabaseConsumerApi? = null) : AdminRepository {
    override suspend fun stats(): AdminStatsDto =
        api?.adminStats() ?: AdminStatsDto(0, 0, 0, 0, 0)

    override suspend fun sections(): List<AdminSectionDto> = adminSections()

    override suspend fun updateEvent(eventId: EventId, patchJson: String) {
        api?.adminUpdateEvent(eventId, patchJson)
    }

    override suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean) {
        api?.adminModerateComment(commentId, approved, flagged)
    }

    override suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): String =
        api?.adminCreateInvite(maxUses, expiresAtIso, note) ?: ""

    override suspend fun revokeInvite(inviteId: String) {
        api?.adminRevokeInvite(inviteId)
    }

    override suspend fun runSource(sourceId: String?) {
        api?.adminRunSource(sourceId)
    }

    override suspend fun runCron(jobName: String?) {
        api?.adminRunCron(jobName)
    }
}

private fun Flow<List<PlanEventRowDto>>.withSeedPlan(cityId: CityId?): Flow<List<PlanEventRowDto>> =
    map { rows ->
        rows.ifEmpty {
            seedEvents().filterByCity(cityId).ifEmpty { seedEvents() }.take(6).mapIndexed { index, event ->
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

private fun CachedProfileEntity.toProfile(): UserProfile =
    UserProfile(
        userId = UserId(userId),
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        currentCityId = currentCityId?.let(::CityId),
        childName = childName,
        childAge = kidAge,
        notificationsEnabled = notificationsEnabled,
        role = role,
    )

private fun UserProfile.toEntity(): CachedProfileEntity =
    CachedProfileEntity(
        userId = userId.rawValue,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        currentCityId = currentCityId?.rawValue,
        childName = childName,
        kidAge = childAge,
        notificationsEnabled = notificationsEnabled,
        role = role,
    )

private fun defaultProfile(userId: UserId): UserProfile =
    UserProfile(
        userId = userId,
        email = userId.rawValue.takeIf { it.contains("@") },
        displayName = null,
        avatarUrl = null,
        currentCityId = CityId("chicago"),
        childName = null,
        childAge = 7,
        notificationsEnabled = false,
        role = "user",
    )

fun adminSections(): List<AdminSectionDto> = listOf(
    AdminSectionDto("dashboard", "Dashboard", "Events, sources, review load, and AI confidence."),
    AdminSectionDto("sources", "Sources", "Scrape sources, runs, errors, and refresh actions."),
    AdminSectionDto("events", "Events", "Search, edit, publish, lock, and delete events."),
    AdminSectionDto("cities", "Cities", "Markets, active status, timezone, and source coverage."),
    AdminSectionDto("comments", "Comments", "Approve, flag, and moderate event comments."),
    AdminSectionDto("ratings", "Ratings", "Review and remove event ratings."),
    AdminSectionDto("access", "Access", "User access, role, and account enablement."),
    AdminSectionDto("invites", "Invites", "Invite requests, codes, approval, and revocation."),
    AdminSectionDto("logs", "Logs", "Source run logs, tag queue, and audit history."),
    AdminSectionDto("crons", "Crons", "Scheduled jobs, run history, and manual triggers."),
)

private fun CachedCityEntity.toDto(): CityDto = CityDto(CityId(id), name, region)

private fun CityDto.toEntity(): CachedCityEntity = CachedCityEntity(id.rawValue, name, region)

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
