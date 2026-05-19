import Foundation
import FECore
import FEData

public protocol AdminRepository {
    func stats() async throws -> AdminStatsDTO
    func sections() async throws -> [AdminSectionDTO]
    func updateEvent(eventId: EventID, patchJSON: String, tagIds: [String], lockEditedFields: Bool) async throws -> EventDTO
    func createEvent(patchJSON: String, tagIds: [String]) async throws -> EventDTO
    func unlockEventFields(eventId: EventID) async throws -> Bool
    func moderateComment(commentId: String, approved: Bool, flagged: Bool) async throws
    func upsertInvite(maxUses: Int?, expiresAtISO: String?, note: String?) async throws -> AdminInviteCodeResultDTO
    func approveInviteRequest(requestId: String) async throws -> AdminInviteApprovalDTO
    func rejectInviteRequest(requestId: String, notes: String?) async throws -> Bool
    func revokeInvite(inviteId: String) async throws -> Bool
    func bulkSetAutoApprove(enable: Bool) async throws
    func runSource(sourceId: String?) async throws
    func retryTagQueue(eventId: EventID) async throws -> Bool
    func listCronJobs() async throws -> [AdminCronJobDTO]
    func cronRunHistory(jobName: String?, limit: Int) async throws -> [AdminCronRunDTO]
    func toggleCronJob(jobName: String, active: Bool) async throws
    func setCronSchedule(jobName: String, schedule: String) async throws
    func runDueScrapes() async throws
    func listComments(filter: String) async throws -> [AdminCommentDTO]
    func deleteComment(commentId: String) async throws
    func listSources() async throws -> [AdminSourceDTO]
    func updateSourceActive(sourceId: String, active: Bool) async throws
    func updateSourceAutoApprove(sourceId: String, autoApprove: Bool) async throws
    func listInviteCodes() async throws -> [AdminInviteCodeListDTO]
    func listInviteRequests(status: String) async throws -> [AdminInviteRequestDTO]
    func listCities() async throws -> [AdminCityDTO]
    func listTags() async throws -> [AdminTagDTO]
    func listEventTagIds(eventId: EventID) async throws -> [String]
    func createCity(name: String, state: String?, country: String, slug: String, timezone: String) async throws -> AdminCityDTO
    func updateCity(cityId: CityID, patchJSON: String) async throws
    func listRatings(limit: Int) async throws -> [AdminRatingDTO]
    func deleteRating(ratingId: String) async throws
    func listUserAccess() async throws -> [AdminUserAccessDTO]
    func updateUserAccess(userId: UserID, isEnabled: Bool, disabledReason: String?) async throws
    func listSourceRuns(limit: Int) async throws -> [AdminSourceRunDTO]
    func listTagQueueSummary() async throws -> [AdminTagQueueSummaryRowDTO]
    func listEvents(keyword: String?, status: String?, cityId: CityID?, limit: Int, offset: Int) async throws -> [AdminEventListItemDTO]
    func listEventFacets() async throws -> AdminEventFacetsDTO
    func bulkUpdateEventStatus(eventIds: [EventID], status: String) async throws
    func bulkDeleteEvent(eventIds: [EventID]) async throws
    func deleteEvent(eventId: EventID) async throws
    func listEventAiTraces(eventId: EventID, limit: Int) async throws -> [AdminEventAiTraceDTO]
}

public extension AdminRepository {
    func updateEvent(eventId: EventID, patchJSON: String) async throws -> EventDTO {
        try await updateEvent(eventId: eventId, patchJSON: patchJSON, tagIds: [], lockEditedFields: true)
    }

    func updateEvent(eventId: EventID, patchJSON: String, tagIds: [String]) async throws -> EventDTO {
        try await updateEvent(eventId: eventId, patchJSON: patchJSON, tagIds: tagIds, lockEditedFields: true)
    }

    func createEvent(patchJSON: String) async throws -> EventDTO {
        try await createEvent(patchJSON: patchJSON, tagIds: [])
    }

    func rejectInviteRequest(requestId: String) async throws -> Bool {
        try await rejectInviteRequest(requestId: requestId, notes: nil)
    }

    func cronRunHistory() async throws -> [AdminCronRunDTO] {
        try await cronRunHistory(jobName: nil, limit: 50)
    }

    func cronRunHistory(jobName: String?) async throws -> [AdminCronRunDTO] {
        try await cronRunHistory(jobName: jobName, limit: 50)
    }

    func listComments() async throws -> [AdminCommentDTO] {
        try await listComments(filter: "all")
    }

    func listInviteRequests() async throws -> [AdminInviteRequestDTO] {
        try await listInviteRequests(status: "pending")
    }

    func createCity(name: String, state: String?, slug: String) async throws -> AdminCityDTO {
        try await createCity(name: name, state: state, country: "US", slug: slug, timezone: "America/Chicago")
    }

    func listRatings() async throws -> [AdminRatingDTO] {
        try await listRatings(limit: 100)
    }

    func updateUserAccess(userId: UserID, isEnabled: Bool) async throws {
        try await updateUserAccess(userId: userId, isEnabled: isEnabled, disabledReason: nil)
    }

    func listSourceRuns() async throws -> [AdminSourceRunDTO] {
        try await listSourceRuns(limit: 50)
    }

    func listEvents() async throws -> [AdminEventListItemDTO] {
        try await listEvents(keyword: nil, status: nil, cityId: nil, limit: 50, offset: 0)
    }

    func listEventAiTraces(eventId: EventID) async throws -> [AdminEventAiTraceDTO] {
        try await listEventAiTraces(eventId: eventId, limit: 5)
    }
}

public func adminSections() -> [AdminSectionDTO] {
    [
        AdminSectionDTO(id: "dashboard", title: "Dashboard", description: "Events, sources, review load, and AI confidence."),
        AdminSectionDTO(id: "events", title: "Events", description: "Search, edit, publish, lock, and delete events."),
        AdminSectionDTO(id: "sources", title: "Sources", description: "Scrape sources, runs, errors, and refresh actions."),
        AdminSectionDTO(id: "invites", title: "Invites", description: "Invite requests, codes, approval, and revocation."),
        AdminSectionDTO(id: "comments", title: "Comments", description: "Approve, flag, and moderate event comments."),
        AdminSectionDTO(id: "cities", title: "Cities", description: "Markets, active status, timezone, and source coverage."),
        AdminSectionDTO(id: "ratings", title: "Ratings", description: "Review and remove event ratings."),
        AdminSectionDTO(id: "access", title: "Access", description: "User access, role, and account enablement."),
        AdminSectionDTO(id: "logs", title: "Logs", description: "Source run logs, tag queue, and audit history."),
        AdminSectionDTO(id: "crons", title: "Crons", description: "Scheduled jobs, run history, and manual triggers."),
    ]
}
