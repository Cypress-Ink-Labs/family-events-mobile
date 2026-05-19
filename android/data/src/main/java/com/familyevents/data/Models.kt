package com.familyevents.data

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.core.UserId
import java.time.Instant

data class EventTagDto(val id: String, val label: String)

data class EventDto(
    val id: EventId,
    val title: String,
    val description: String?,
    val startsAt: Instant,
    val endsAt: Instant?,
    val venueName: String?,
    val address: String?,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val price: Double? = null,
    val isFree: Boolean = false,
    val imageUrl: String?,
    val sourceUrl: String?,
    val cityId: CityId,
    val coordinate: GeoCoordinate?,
    val tags: List<EventTagDto> = emptyList(),
    val avgRating: Double = 0.0,
    val ratingCount: Int = 0,
    val isFavorited: Boolean = false,
    val isInCalendar: Boolean = false,
)

data class FavoriteDto(val eventId: EventId, val userId: UserId, val createdAt: Instant)

data class WeatherSnapshotDto(
    val dateKey: String,
    val summary: String,
    val temperatureHighF: Int?,
    val precipitationChance: Double?,
)

data class PlanEventRowDto(
    val event: EventDto,
    val section: String,
    val rank: Int,
)

data class EventQuery(
    val cityId: CityId?,
    val search: String? = null,
    val tagIds: List<String> = emptyList(),
    val dateKey: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

data class ProfileContext(
    val userId: UserId,
    val currentCityId: CityId?,
    val kidAge: Int?,
    val notificationsEnabled: Boolean,
    val role: String = "user",
) {
    val isAdmin: Boolean get() = role == "admin"
}

data class RatingDto(
    val id: String,
    val userId: UserId,
    val eventId: EventId,
    val score: Int,
    val createdAt: Instant,
)

data class CommentDto(
    val id: String,
    val userId: UserId,
    val eventId: EventId,
    val body: String,
    val isApproved: Boolean,
    val isFlagged: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val authorDisplayName: String?,
    val authorAvatarUrl: String?,
)

data class UserProfile(
    val userId: UserId,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val currentCityId: CityId?,
    val childName: String?,
    val childAge: Int?,
    val notificationsEnabled: Boolean,
    val role: String = "user",
) {
    fun toContext(): ProfileContext = ProfileContext(userId, currentCityId, childAge, notificationsEnabled, role)
}

data class UserProfileUpdate(
    val displayName: String?,
    val currentCityId: CityId?,
    val childName: String?,
    val childAge: Int?,
)

data class CityDto(val id: CityId, val name: String, val region: String?)

data class AdminStatsDto(
    val totalEvents: Int,
    val pendingReview: Int,
    val published: Int,
    val activeSources: Int,
    val sourceErrors: Int,
    val aiHigh: Int = 0,
    val aiMedium: Int = 0,
    val aiLow: Int = 0,
    val aiUntagged: Int = 0,
)

data class AdminSectionDto(
    val id: String,
    val title: String,
    val description: String,
)

data class AdminInviteCodeResultDto(
    val id: String,
    val code: String,
    val maxUses: Int,
    val expiresAt: Instant?,
    val notes: String?,
    val createdAt: Instant,
)

data class AdminInviteApprovalDto(
    val requestId: String,
    val code: String,
    val inviteCodeId: String,
    val email: String,
    val createdAt: Instant,
)

data class AdminCronJobDto(
    val jobid: Long,
    val jobname: String,
    val schedule: String,
    val command: String,
    val active: Boolean,
    val lastRunStart: Instant?,
    val lastRunEnd: Instant?,
    val lastRunStatus: String?,
    val lastRunMessage: String?,
)

data class AdminCommentDto(
    val id: String,
    val userId: UserId,
    val eventId: EventId,
    val body: String,
    val isApproved: Boolean,
    val isFlagged: Boolean,
    val createdAt: Instant,
    val authorDisplayName: String?,
    val eventTitle: String?,
)

data class AdminCronRunDto(
    val runid: Long,
    val jobname: String,
    val status: String,
    val returnMessage: String?,
    val startTime: Instant,
    val endTime: Instant?,
    val durationMs: Double?,
)

data class AdminSourceDto(
    val id: String,
    val name: String,
    val cityId: CityId?,
    val url: String?,
    val isActive: Boolean,
    val autoApprove: Boolean,
    val lastStatus: String?,
    val lastScrapedAt: Instant?,
)

data class AdminInviteCodeListDto(
    val id: String,
    val maxUses: Int,
    val usedCount: Int,
    val expiresAt: Instant?,
    val notes: String?,
    val createdAt: Instant,
)

data class AdminInviteRequestDto(
    val id: String,
    val email: String,
    val message: String?,
    val status: String,
    val createdAt: Instant,
    val reviewedAt: Instant?,
    val adminNotes: String?,
)

data class AdminCityDto(
    val id: CityId,
    val name: String,
    val state: String?,
    val country: String,
    val slug: String,
    val isActive: Boolean,
    val timezone: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Instant,
)

data class AdminRatingDto(
    val id: String,
    val userId: UserId,
    val eventId: EventId,
    val score: Int,
    val createdAt: Instant,
    val authorDisplayName: String?,
    val eventTitle: String?,
)

data class AdminUserAccessDto(
    val userId: UserId,
    val isEnabled: Boolean,
    val accessExpiresAt: Instant?,
    val enabledAt: Instant?,
    val disabledAt: Instant?,
    val disabledReason: String?,
    val displayName: String?,
    val email: String?,
    val role: String,
)

data class AdminSourceRunDto(
    val id: String,
    val sourceId: String?,
    val sourceName: String?,
    val startedAt: Instant,
    val completedAt: Instant?,
    val status: String,
    val eventsFound: Int,
    val eventsImported: Int,
    val eventsSkipped: Int,
    val errorLog: String?,
)

data class AdminTagQueueSummaryRowDto(
    val status: String,
    val rowCount: Int,
    val oldestEnqueuedAt: Instant?,
    val newestEnqueuedAt: Instant?,
    val lastDeadLetterAt: Instant?,
    val avgAttempts: Double?,
)
