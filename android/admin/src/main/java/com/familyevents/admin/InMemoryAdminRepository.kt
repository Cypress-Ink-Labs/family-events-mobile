package com.familyevents.admin

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto

class InMemoryAdminRepository : AdminRepository {
    private val cities = mutableListOf<AdminCityDto>()
    private val tags = listOf(
        AdminTagDto(id = "local-family", name = "Family", slug = "family"),
        AdminTagDto(id = "local-outdoors", name = "Outdoors", slug = "outdoors"),
        AdminTagDto(id = "local-free", name = "Free", slug = "free"),
    )

    override suspend fun stats(): AdminStatsDto = AdminStatsDto(2, 0, 2, 0, 0)
    override suspend fun sections(): List<AdminSectionDto> = adminSections()
    override suspend fun updateEvent(eventId: EventId, patchJson: String, tagIds: List<String>, lockEditedFields: Boolean): EventDto =
        EventDto(
            id = eventId, title = "Updated Event", description = null,
            startsAt = java.time.Instant.now(), endsAt = null,
            venueName = null, address = null, imageUrl = null, sourceUrl = null,
            cityId = CityId("chicago"), coordinate = null,
        )
    override suspend fun createEvent(patchJson: String, tagIds: List<String>): EventDto =
        EventDto(
            id = EventId("local-${java.time.Instant.now().toEpochMilli()}"),
            title = "New Event", description = null,
            startsAt = java.time.Instant.now(), endsAt = null,
            venueName = null, address = null, imageUrl = null, sourceUrl = null,
            cityId = CityId("chicago"), coordinate = null,
        )
    override suspend fun unlockEventFields(eventId: EventId): Boolean = true
    override suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean) = Unit
    override suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): AdminInviteCodeResultDto =
        AdminInviteCodeResultDto(
            id = "local-invite-id", code = "LOCAL-CODE",
            maxUses = maxUses ?: 1, expiresAt = null,
            notes = note, createdAt = java.time.Instant.now(),
        )
    override suspend fun approveInviteRequest(requestId: String): AdminInviteApprovalDto =
        AdminInviteApprovalDto(
            requestId = requestId, code = "LOCAL-APPROVED-CODE",
            inviteCodeId = "local-invite-code-id", email = "local@example.com",
            createdAt = java.time.Instant.now(),
        )
    override suspend fun rejectInviteRequest(requestId: String, notes: String?): Boolean = true
    override suspend fun revokeInvite(inviteId: String): Boolean = true
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
    override suspend fun listCities(): List<AdminCityDto> = cities.toList()
    override suspend fun listTags(): List<AdminTagDto> = tags
    override suspend fun listEventTagIds(eventId: EventId): List<String> = emptyList()
    override suspend fun createCity(name: String, state: String?, country: String, slug: String, timezone: String): AdminCityDto {
        val created = AdminCityDto(
            id = CityId("local-${java.time.Instant.now().toEpochMilli()}"),
            name = name, state = state, country = country, slug = slug,
            isActive = true, timezone = timezone, latitude = null, longitude = null,
            createdAt = java.time.Instant.now(),
        )
        cities.add(0, created)
        return created
    }
    override suspend fun updateCity(cityId: CityId, patchJson: String) {
        val index = cities.indexOfFirst { it.id == cityId }
        if (index >= 0) {
            cities[index] = cities[index].copy()
        }
    }
    override suspend fun listRatings(limit: Int): List<AdminRatingDto> = emptyList()
    override suspend fun deleteRating(ratingId: String) = Unit
    override suspend fun listUserAccess(): List<AdminUserAccessDto> = emptyList()
    override suspend fun updateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String?) = Unit
    override suspend fun listSourceRuns(limit: Int): List<AdminSourceRunDto> = emptyList()
    override suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto> = emptyList()
    override suspend fun listEvents(keyword: String?, status: String?, cityId: CityId?, limit: Int, offset: Int): List<AdminEventListItemDto> = emptyList()
    override suspend fun listEventFacets(): AdminEventFacetsDto = AdminEventFacetsDto(emptyMap(), emptyMap())
    override suspend fun bulkUpdateEventStatus(eventIds: List<EventId>, status: String) = Unit
    override suspend fun bulkDeleteEvent(eventIds: List<EventId>) = Unit
    override suspend fun deleteEvent(eventId: EventId) = Unit
    override suspend fun listEventAiTraces(eventId: EventId, limit: Int): List<AdminEventAiTraceDto> = emptyList()
}
