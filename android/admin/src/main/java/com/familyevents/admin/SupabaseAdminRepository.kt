package com.familyevents.admin

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto

class SupabaseAdminRepository(private val api: SupabaseAdminApi) : AdminRepository {
    override suspend fun stats(): AdminStatsDto = api.adminStats()

    override suspend fun sections(): List<AdminSectionDto> = adminSections()

    override suspend fun updateEvent(eventId: EventId, patchJson: String, tagIds: List<String>, lockEditedFields: Boolean): EventDto =
        api.adminUpdateEvent(eventId, patchJson, tagIds, lockEditedFields)

    override suspend fun createEvent(patchJson: String, tagIds: List<String>): EventDto =
        api.adminCreateEvent(patchJson, tagIds)

    override suspend fun unlockEventFields(eventId: EventId): Boolean =
        api.adminUnlockEventFields(eventId)

    override suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean) {
        api.adminModerateComment(commentId, approved, flagged)
    }

    override suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): AdminInviteCodeResultDto =
        api.adminCreateInviteCode(maxUses ?: 1, expiresAtIso, note)

    override suspend fun approveInviteRequest(requestId: String): AdminInviteApprovalDto =
        api.adminApproveInviteRequest(requestId)

    override suspend fun rejectInviteRequest(requestId: String, notes: String?): Boolean =
        api.adminRejectInviteRequest(requestId, notes)

    override suspend fun revokeInvite(inviteId: String): Boolean =
        api.adminRevokeInvite(inviteId)

    override suspend fun bulkSetAutoApprove(enable: Boolean) {
        api.adminBulkSetAutoApprove(enable)
    }

    override suspend fun runSource(sourceId: String?) {
        api.adminRunSource(sourceId)
    }

    override suspend fun retryTagQueue(eventId: EventId): Boolean =
        api.adminRetryTagQueue(eventId)

    override suspend fun listCronJobs(): List<AdminCronJobDto> =
        api.adminListCronJobs()

    override suspend fun cronRunHistory(jobName: String?, limit: Int): List<AdminCronRunDto> =
        api.adminCronRunHistory(jobName, limit)

    override suspend fun toggleCronJob(jobName: String, active: Boolean) {
        api.adminToggleCronJob(jobName, active)
    }

    override suspend fun setCronSchedule(jobName: String, schedule: String) {
        api.adminSetCronSchedule(jobName, schedule)
    }

    override suspend fun runDueScrapes() {
        api.adminRunDueScrapes()
    }

    override suspend fun listComments(filter: String): List<AdminCommentDto> =
        api.adminListComments(filter)

    override suspend fun deleteComment(commentId: String) {
        api.adminDeleteComment(commentId)
    }

    override suspend fun listSources(): List<AdminSourceDto> =
        api.adminListSources()

    override suspend fun updateSourceActive(sourceId: String, active: Boolean) {
        api.adminUpdateSourceActive(sourceId, active)
    }

    override suspend fun updateSourceAutoApprove(sourceId: String, autoApprove: Boolean) {
        api.adminUpdateSourceAutoApprove(sourceId, autoApprove)
    }

    override suspend fun listInviteCodes(): List<AdminInviteCodeListDto> =
        api.adminListInviteCodes()

    override suspend fun listInviteRequests(status: String): List<AdminInviteRequestDto> =
        api.adminListInviteRequests(status)

    override suspend fun listCities(): List<AdminCityDto> =
        api.adminListCities()

    override suspend fun createCity(name: String, state: String?, country: String, slug: String, timezone: String): AdminCityDto =
        api.adminCreateCity(name, state, country, slug, timezone)

    override suspend fun updateCity(cityId: CityId, patchJson: String) {
        api.adminUpdateCity(cityId, patchJson)
    }

    override suspend fun listRatings(limit: Int): List<AdminRatingDto> =
        api.adminListRatings(limit)

    override suspend fun deleteRating(ratingId: String) {
        api.adminDeleteRating(ratingId)
    }

    override suspend fun listUserAccess(): List<AdminUserAccessDto> =
        api.adminListUserAccess()

    override suspend fun updateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String?) {
        api.adminUpdateUserAccess(userId, isEnabled, disabledReason)
    }

    override suspend fun listSourceRuns(limit: Int): List<AdminSourceRunDto> =
        api.adminListSourceRuns(limit)

    override suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto> =
        api.adminListTagQueueSummary()

    override suspend fun listEvents(keyword: String?, status: String?, cityId: CityId?, limit: Int, offset: Int): List<AdminEventListItemDto> =
        api.adminListEvents(keyword, status, cityId, limit, offset)

    override suspend fun listEventFacets(): AdminEventFacetsDto =
        api.adminListEventFacets()

    override suspend fun bulkUpdateEventStatus(eventIds: List<EventId>, status: String) {
        api.adminBulkUpdateEventStatus(eventIds, status)
    }

    override suspend fun bulkDeleteEvent(eventIds: List<EventId>) {
        api.adminBulkDeleteEvent(eventIds)
    }

    override suspend fun deleteEvent(eventId: EventId) {
        api.adminDeleteEvent(eventId)
    }

    override suspend fun listEventAiTraces(eventId: EventId, limit: Int): List<AdminEventAiTraceDto> =
        api.adminListEventAiTraces(eventId, limit)
}
