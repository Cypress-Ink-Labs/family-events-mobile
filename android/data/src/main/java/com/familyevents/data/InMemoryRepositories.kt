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
    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) = Unit
    override suspend fun signOut() {
        state.value = SessionState.SignedOut
    }

    override suspend fun invitesRequired(): Boolean = false
    override suspend fun requestInvite(email: String, message: String?): Boolean = true
}

class InMemoryProfileRepository : ProfileRepository {
    private val profiles = MutableStateFlow<Map<String, ProfileContext>>(emptyMap())

    override fun observeProfile(userId: UserId): Flow<ProfileContext?> = profiles.map { it[userId.rawValue] }

    override suspend fun currentContext(userId: UserId): ProfileContext =
        profiles.value[userId.rawValue] ?: ProfileContext(userId, CityId("chicago"), 7, notificationsEnabled = false)

    override suspend fun profile(userId: UserId): UserProfile =
        currentContext(userId).let { context ->
            UserProfile(
                userId = userId,
                email = userId.rawValue.takeIf { it.contains("@") },
                displayName = null,
                avatarUrl = null,
                currentCityId = context.currentCityId,
                childName = null,
                childAge = context.kidAge,
                notificationsEnabled = context.notificationsEnabled,
            )
        }

    override suspend fun updateProfile(userId: UserId, update: UserProfileUpdate): UserProfile {
        val current = profile(userId)
        val updated = current.copy(
            displayName = update.displayName,
            currentCityId = update.currentCityId,
            childName = update.childName,
            childAge = update.childAge,
        )
        profiles.update { rows -> rows + (userId.rawValue to updated.toContext()) }
        return updated
    }

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

    override suspend fun refreshPlan(userId: UserId, cityId: CityId?, kidAge: Int?) = Unit
    override suspend fun refreshEventList(query: EventQuery) = Unit
    override suspend fun refreshEventDetail(id: EventId) = Unit
    override suspend fun publicEvent(id: EventId): EventDto? = events.value.firstOrNull { it.id == id }

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

class InMemoryRatingRepository : RatingRepository {
    private val ratings = MutableStateFlow<Map<Pair<String, String>, RatingDto>>(emptyMap())

    override suspend fun userRating(userId: UserId, eventId: EventId): RatingDto? =
        ratings.value[userId.rawValue to eventId.rawValue]

    override suspend fun upsertRating(userId: UserId, eventId: EventId, score: Int): RatingDto {
        val rating = RatingDto("rating-${userId.rawValue}-${eventId.rawValue}", userId, eventId, score.coerceIn(1, 5), Instant.now())
        ratings.update { it + ((userId.rawValue to eventId.rawValue) to rating) }
        return rating
    }
}

class InMemoryCommentRepository : CommentRepository {
    private val comments = MutableStateFlow<Map<String, List<CommentDto>>>(emptyMap())

    override suspend fun comments(eventId: EventId): List<CommentDto> =
        comments.value[eventId.rawValue].orEmpty()

    override suspend fun addComment(userId: UserId, eventId: EventId, body: String): CommentDto {
        val now = Instant.now()
        val comment = CommentDto(
            id = "comment-${now.toEpochMilli()}",
            userId = userId,
            eventId = eventId,
            body = body,
            isApproved = true,
            isFlagged = false,
            createdAt = now,
            updatedAt = now,
            authorDisplayName = null,
            authorAvatarUrl = null,
        )
        comments.update { rows -> rows + (eventId.rawValue to (listOf(comment) + rows[eventId.rawValue].orEmpty())) }
        return comment
    }
}

class InMemoryAdminRepository : AdminRepository {
    override suspend fun stats(): AdminStatsDto = AdminStatsDto(2, 0, 2, 0, 0)
    override suspend fun sections(): List<AdminSectionDto> = adminSections()
    override suspend fun updateEvent(eventId: EventId, patchJson: String, tagIds: List<String>, lockEditedFields: Boolean): EventDto =
        EventDto(
            id = eventId, title = "Updated Event", description = null,
            startsAt = java.time.Instant.now(), endsAt = null,
            venueName = null, address = null, imageUrl = null, sourceUrl = null,
            cityId = com.familyevents.core.CityId("chicago"), coordinate = null,
        )
    override suspend fun createEvent(patchJson: String, tagIds: List<String>): EventDto =
        EventDto(
            id = com.familyevents.core.EventId("local-${java.time.Instant.now().toEpochMilli()}"),
            title = "New Event", description = null,
            startsAt = java.time.Instant.now(), endsAt = null,
            venueName = null, address = null, imageUrl = null, sourceUrl = null,
            cityId = com.familyevents.core.CityId("chicago"), coordinate = null,
        )
    override suspend fun unlockEventFields(eventId: EventId): Boolean = true
    override suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean) = Unit
    override suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): AdminInviteCodeResultDto =
        AdminInviteCodeResultDto(
            id = "local-invite-id",
            code = "LOCAL-CODE",
            maxUses = maxUses ?: 1,
            expiresAt = null,
            notes = note,
            createdAt = java.time.Instant.now(),
        )
    override suspend fun approveInviteRequest(requestId: String): AdminInviteApprovalDto =
        AdminInviteApprovalDto(
            requestId = requestId,
            code = "LOCAL-APPROVED-CODE",
            inviteCodeId = "local-invite-code-id",
            email = "local@example.com",
            createdAt = java.time.Instant.now(),
        )
    override suspend fun rejectInviteRequest(requestId: String, notes: String?): Boolean = true
    override suspend fun revokeInvite(inviteId: String) = Unit
    override suspend fun bulkSetAutoApprove(enable: Boolean) = Unit
    override suspend fun runSource(sourceId: String?) = Unit
    override suspend fun retryTagQueue(eventId: EventId): Boolean = true
    override suspend fun listCronJobs(): List<AdminCronJobDto> = emptyList()
    override suspend fun cronRunHistory(jobName: String?, limit: Int): List<AdminCronRunDto> = emptyList()
    override suspend fun toggleCronJob(jobName: String, active: Boolean) = Unit
    override suspend fun setCronSchedule(jobName: String, schedule: String) = Unit
    override suspend fun runDueScrapes() = Unit
    override suspend fun listComments(filter: String): List<AdminCommentDto> = emptyList()
    override suspend fun deleteComment(commentId: String) = Unit
    override suspend fun listSources(): List<AdminSourceDto> = emptyList()
    override suspend fun updateSourceActive(sourceId: String, active: Boolean) = Unit
    override suspend fun updateSourceAutoApprove(sourceId: String, autoApprove: Boolean) = Unit
    override suspend fun listInviteCodes(): List<AdminInviteCodeListDto> = emptyList()
    override suspend fun listInviteRequests(status: String): List<AdminInviteRequestDto> = emptyList()
    override suspend fun listCities(): List<AdminCityDto> = emptyList()
    override suspend fun createCity(name: String, state: String?, country: String, slug: String, timezone: String): AdminCityDto =
        AdminCityDto(
            id = com.familyevents.core.CityId("local-${java.time.Instant.now().toEpochMilli()}"),
            name = name, state = state, country = country, slug = slug,
            isActive = true, timezone = timezone, latitude = null, longitude = null,
            createdAt = java.time.Instant.now(),
        )
    override suspend fun updateCity(cityId: com.familyevents.core.CityId, patchJson: String) = Unit
    override suspend fun listRatings(limit: Int): List<AdminRatingDto> = emptyList()
    override suspend fun deleteRating(ratingId: String) = Unit
    override suspend fun listUserAccess(): List<AdminUserAccessDto> = emptyList()
    override suspend fun updateUserAccess(userId: com.familyevents.core.UserId, isEnabled: Boolean, disabledReason: String?) = Unit
    override suspend fun listSourceRuns(limit: Int): List<AdminSourceRunDto> = emptyList()
    override suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto> = emptyList()
}

class RepositoryGraph(
    val authRepository: AuthRepository = InMemoryAuthRepository(),
    val eventRepository: EventRepository = InMemoryEventRepository(),
    val favoriteRepository: FavoriteRepository = InMemoryFavoriteRepository(eventRepository),
    val profileRepository: ProfileRepository = InMemoryProfileRepository(),
    val cityRepository: CityRepository = InMemoryCityRepository(),
    val weatherRepository: WeatherRepository = InMemoryWeatherRepository(),
    val ratingRepository: RatingRepository = InMemoryRatingRepository(),
    val commentRepository: CommentRepository = InMemoryCommentRepository(),
    val adminRepository: AdminRepository = InMemoryAdminRepository(),
) {
    companion object {
        fun roomBacked(
            database: FamilyEventsDatabase,
            config: com.familyevents.core.EnvConfig,
            sessionStore: SessionStore = MemorySessionStore(),
        ): RepositoryGraph {
            SupabaseClientFactory.create(config)
            val api = KtorSupabaseConsumerApi(config, sessionStore)
            val eventRepository = RoomBackedEventRepository(database.eventDao(), database.planDao(), api)
            return RepositoryGraph(
                authRepository = LocalAuthRepository(sessionStore, api),
                eventRepository = eventRepository,
                favoriteRepository = RoomBackedFavoriteRepository(database.favoriteDao(), api),
                profileRepository = RoomBackedProfileRepository(database.profileDao(), api),
                cityRepository = RoomBackedCityRepository(database.cityDao(), api),
                weatherRepository = RoomBackedWeatherRepository(database.weatherDao()),
                ratingRepository = SupabaseRatingRepository(api),
                commentRepository = SupabaseCommentRepository(api),
                adminRepository = SupabaseAdminRepository(api),
            )
        }
    }
}
