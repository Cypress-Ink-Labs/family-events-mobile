package com.familyevents.admin

import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventDto

interface AdminRepository {
    suspend fun stats(): AdminStatsDto
    suspend fun sections(): List<AdminSectionDto>
    suspend fun updateEvent(eventId: EventId, patchJson: String, tagIds: List<String> = emptyList(), lockEditedFields: Boolean = true): EventDto
    suspend fun createEvent(patchJson: String, tagIds: List<String> = emptyList()): EventDto
    suspend fun unlockEventFields(eventId: EventId): Boolean
    suspend fun moderateComment(commentId: String, approved: Boolean, flagged: Boolean)
    suspend fun upsertInvite(maxUses: Int?, expiresAtIso: String?, note: String?): AdminInviteCodeResultDto
    suspend fun approveInviteRequest(requestId: String): AdminInviteApprovalDto
    suspend fun rejectInviteRequest(requestId: String, notes: String? = null): Boolean
    suspend fun revokeInvite(inviteId: String): Boolean
    suspend fun bulkSetAutoApprove(enable: Boolean)
    suspend fun runSource(sourceId: String?)
    suspend fun retryTagQueue(eventId: EventId): Boolean
    suspend fun listCronJobs(): List<AdminCronJobDto>
    suspend fun cronRunHistory(jobName: String? = null, limit: Int = 50): List<AdminCronRunDto>
    suspend fun toggleCronJob(jobName: String, active: Boolean)
    suspend fun setCronSchedule(jobName: String, schedule: String)
    suspend fun runDueScrapes()
    suspend fun listComments(filter: String = "all"): List<AdminCommentDto>
    suspend fun deleteComment(commentId: String)
    suspend fun listSources(): List<AdminSourceDto>
    suspend fun updateSourceActive(sourceId: String, active: Boolean)
    suspend fun updateSourceAutoApprove(sourceId: String, autoApprove: Boolean)
    suspend fun listInviteCodes(): List<AdminInviteCodeListDto>
    suspend fun listInviteRequests(status: String = "pending"): List<AdminInviteRequestDto>
    suspend fun listCities(): List<AdminCityDto>
    suspend fun createCity(name: String, state: String?, country: String = "US", slug: String, timezone: String = "America/Chicago"): AdminCityDto
    suspend fun updateCity(cityId: CityId, patchJson: String)
    suspend fun listRatings(limit: Int = 100): List<AdminRatingDto>
    suspend fun deleteRating(ratingId: String)
    suspend fun listUserAccess(): List<AdminUserAccessDto>
    suspend fun updateUserAccess(userId: UserId, isEnabled: Boolean, disabledReason: String? = null)
    suspend fun listSourceRuns(limit: Int = 50): List<AdminSourceRunDto>
    suspend fun listTagQueueSummary(): List<AdminTagQueueSummaryRowDto>
    suspend fun listEvents(keyword: String? = null, status: String? = null, cityId: CityId? = null, limit: Int = 50, offset: Int = 0): List<AdminEventListItemDto>
    suspend fun listEventFacets(): AdminEventFacetsDto
    suspend fun bulkUpdateEventStatus(eventIds: List<EventId>, status: String)
    suspend fun bulkDeleteEvent(eventIds: List<EventId>)
    suspend fun deleteEvent(eventId: EventId)
    suspend fun listEventAiTraces(eventId: EventId, limit: Int = 5): List<AdminEventAiTraceDto>
}

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
