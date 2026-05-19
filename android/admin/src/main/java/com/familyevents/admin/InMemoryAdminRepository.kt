package com.familyevents.admin

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto
import com.familyevents.data.EventTagDto

class InMemoryAdminRepository : AdminRepository {
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
            id = CityId("local-${java.time.Instant.now().toEpochMilli()}"),
            name = name, state = state, country = country, slug = slug,
            isActive = true, timezone = timezone, latitude = null, longitude = null,
            createdAt = java.time.Instant.now(),
        )
    override suspend fun updateCity(cityId: CityId, patchJson: String) = Unit
    override suspend fun listRatings(limit: Int): List<AdminRatingDto> = emptyList()
    override suspend fun deleteRating(ratingId: String) = Unit
    override suspend fun listUserAccess(): List<AdminUserAccessDto> = emptyList()
    override suspend fun updateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String?) = Unit
    override suspend fun listSourceRuns(limit: Int): List<AdminSourceRunDto> = emptyList()
    override suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto> = emptyList()
}
