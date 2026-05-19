package com.familyevents.admin

import com.familyevents.core.AppError
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto

class SupabaseAdminRepository(private val api: SupabaseAdminApi? = null) : AdminRepository {
    override suspend fun stats(): AdminStatsDto =
        api?.adminStats() ?: AdminStatsDto(0, 0, 0, 0, 0)

    override suspend fun sections(): List<AdminSectionDto> = adminSections()

    override suspend fun updateEvent(eventId: EventId, patchJson: String, tagIds: List<String>, lockEditedFields: Boolean): EventDto =
        api?.adminUpdateEvent(eventId, patchJson, tagIds, lockEditedFields)
            ?: throw AppError.Remote("Event management is unavailable.")

    override suspend fun createEvent(patchJson: String, tagIds: List<String>): EventDto =
        api?.adminCreateEvent(patchJson, tagIds)
            ?: throw AppError.Remote("Event creation is unavailable.")

    override suspend fun unlockEventFields(eventId: EventId): Boolean =
        api?.adminUnlockEventFields(eventId) ?: false

    override suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean) {
        api?.adminModerateComment(commentId, approved, flagged)
    }

    override suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): AdminInviteCodeResultDto =
        api?.adminCreateInviteCode(maxUses ?: 1, expiresAtIso, note)
            ?: throw AppError.Remote("Invite code creation is unavailable.")

    override suspend fun approveInviteRequest(requestId: String): AdminInviteApprovalDto =
        api?.adminApproveInviteRequest(requestId)
            ?: throw AppError.Remote("Invite approval is unavailable.")

    override suspend fun rejectInviteRequest(requestId: String, notes: String?): Boolean =
        api?.adminRejectInviteRequest(requestId, notes) ?: false

    override suspend fun revokeInvite(inviteId: String): Boolean =
        api?.adminRevokeInvite(inviteId) ?: false

    override suspend fun bulkSetAutoApprove(enable: Boolean) {
        api?.adminBulkSetAutoApprove(enable)
    }

    override suspend fun runSource(sourceId: String?) {
        api?.adminRunSource(sourceId)
    }

    override suspend fun retryTagQueue(eventId: EventId): Boolean =
        api?.adminRetryTagQueue(eventId) ?: false

    override suspend fun listCronJobs(): List<AdminCronJobDto> =
        api?.adminListCronJobs() ?: emptyList()

    override suspend fun cronRunHistory(jobName: String?, limit: Int): List<AdminCronRunDto> =
        api?.adminCronRunHistory(jobName, limit) ?: emptyList()

    override suspend fun toggleCronJob(jobName: String, active: Boolean) {
        api?.adminToggleCronJob(jobName, active)
    }

    override suspend fun setCronSchedule(jobName: String, schedule: String) {
        api?.adminSetCronSchedule(jobName, schedule)
    }

    override suspend fun runDueScrapes() {
        api?.adminRunDueScrapes()
    }

    override suspend fun listComments(filter: String): List<AdminCommentDto> =
        api?.adminListComments(filter) ?: emptyList()

    override suspend fun deleteComment(commentId: String) {
        api?.adminDeleteComment(commentId)
    }

    override suspend fun listSources(): List<AdminSourceDto> =
        api?.adminListSources() ?: emptyList()

    override suspend fun updateSourceActive(sourceId: String, active: Boolean) {
        api?.adminUpdateSourceActive(sourceId, active)
    }

    override suspend fun updateSourceAutoApprove(sourceId: String, autoApprove: Boolean) {
        api?.adminUpdateSourceAutoApprove(sourceId, autoApprove)
    }

    override suspend fun listInviteCodes(): List<AdminInviteCodeListDto> =
        api?.adminListInviteCodes() ?: emptyList()

    override suspend fun listInviteRequests(status: String): List<AdminInviteRequestDto> =
        api?.adminListInviteRequests(status) ?: emptyList()

    override suspend fun listCities(): List<AdminCityDto> =
        api?.adminListCities() ?: emptyList()

    override suspend fun createCity(name: String, state: String?, country: String, slug: String, timezone: String): AdminCityDto =
        api?.adminCreateCity(name, state, country, slug, timezone)
            ?: throw AppError.Remote("City creation unavailable.")

    override suspend fun updateCity(cityId: CityId, patchJson: String) {
        api?.adminUpdateCity(cityId, patchJson)
    }

    override suspend fun listRatings(limit: Int): List<AdminRatingDto> =
        api?.adminListRatings(limit) ?: emptyList()

    override suspend fun deleteRating(ratingId: String) {
        api?.adminDeleteRating(ratingId)
    }

    override suspend fun listUserAccess(): List<AdminUserAccessDto> =
        api?.adminListUserAccess() ?: emptyList()

    override suspend fun updateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String?) {
        api?.adminUpdateUserAccess(userId, isEnabled, disabledReason)
    }

    override suspend fun listSourceRuns(limit: Int): List<AdminSourceRunDto> =
        api?.adminListSourceRuns(limit) ?: emptyList()

    override suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto> =
        api?.adminListTagQueueSummary() ?: emptyList()

    override suspend fun listEvents(keyword: String?, status: String?, cityId: CityId?, limit: Int, offset: Int): List<AdminEventListItemDto> =
        api?.adminListEvents(keyword, status, cityId, limit, offset) ?: emptyList()

    override suspend fun listEventFacets(): AdminEventFacetsDto =
        api?.adminListEventFacets() ?: AdminEventFacetsDto(emptyMap(), emptyMap())

    override suspend fun bulkUpdateEventStatus(eventIds: List<EventId>, status: String) {
        api?.adminBulkUpdateEventStatus(eventIds, status)
    }

    override suspend fun bulkDeleteEvent(eventIds: List<EventId>) {
        api?.adminBulkDeleteEvent(eventIds)
    }

    override suspend fun deleteEvent(eventId: EventId) {
        api?.adminDeleteEvent(eventId)
    }

    override suspend fun listEventAiTraces(eventId: EventId, limit: Int): List<AdminEventAiTraceDto> =
        api?.adminListEventAiTraces(eventId, limit) ?: emptyList()
}
