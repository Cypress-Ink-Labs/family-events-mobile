import Foundation
import FECore
import FEData

public enum AdminRepositoryStubError: Error {
    case notImplemented
}

public final class SupabaseAdminRepository: AdminRepository {
    public init() {}

    public func stats() async throws -> AdminStatsDTO {
        AdminStatsDTO(
            totalEvents: 0,
            pendingReview: 0,
            published: 0,
            activeSources: 0,
            sourceErrors: 0,
            aiHigh: 0,
            aiMedium: 0,
            aiLow: 0,
            aiUntagged: 0
        )
    }

    public func sections() async throws -> [AdminSectionDTO] {
        adminSections()
    }

    public func updateEvent(eventId: EventID, patchJSON: String, tagIds: [String], lockEditedFields: Bool) async throws -> EventDTO {
        throw AdminRepositoryStubError.notImplemented
    }

    public func createEvent(patchJSON: String, tagIds: [String]) async throws -> EventDTO {
        throw AdminRepositoryStubError.notImplemented
    }

    public func unlockEventFields(eventId: EventID) async throws -> Bool {
        false
    }

    public func moderateComment(commentId: String, approved: Bool, flagged: Bool) async throws {}

    public func upsertInvite(maxUses: Int?, expiresAtISO: String?, note: String?) async throws -> AdminInviteCodeResultDTO {
        throw AdminRepositoryStubError.notImplemented
    }

    public func approveInviteRequest(requestId: String) async throws -> AdminInviteApprovalDTO {
        throw AdminRepositoryStubError.notImplemented
    }

    public func rejectInviteRequest(requestId: String, notes: String?) async throws -> Bool {
        false
    }

    public func revokeInvite(inviteId: String) async throws -> Bool {
        false
    }

    public func bulkSetAutoApprove(enable: Bool) async throws {}

    public func runSource(sourceId: String?) async throws {}

    public func retryTagQueue(eventId: EventID) async throws -> Bool {
        false
    }

    public func listCronJobs() async throws -> [AdminCronJobDTO] {
        []
    }

    public func cronRunHistory(jobName: String?, limit: Int) async throws -> [AdminCronRunDTO] {
        []
    }

    public func toggleCronJob(jobName: String, active: Bool) async throws {}

    public func setCronSchedule(jobName: String, schedule: String) async throws {}

    public func runDueScrapes() async throws {}

    public func listComments(filter: String) async throws -> [AdminCommentDTO] {
        []
    }

    public func deleteComment(commentId: String) async throws {}

    public func listSources() async throws -> [AdminSourceDTO] {
        []
    }

    public func updateSourceActive(sourceId: String, active: Bool) async throws {}

    public func updateSourceAutoApprove(sourceId: String, autoApprove: Bool) async throws {}

    public func listInviteCodes() async throws -> [AdminInviteCodeListDTO] {
        []
    }

    public func listInviteRequests(status: String) async throws -> [AdminInviteRequestDTO] {
        []
    }

    public func listCities() async throws -> [AdminCityDTO] {
        []
    }

    public func listTags() async throws -> [AdminTagDTO] {
        []
    }

    public func listEventTagIds(eventId: EventID) async throws -> [String] {
        []
    }

    public func createCity(name: String, state: String?, country: String, slug: String, timezone: String) async throws -> AdminCityDTO {
        throw AdminRepositoryStubError.notImplemented
    }

    public func updateCity(cityId: CityID, patchJSON: String) async throws {}

    public func listRatings(limit: Int) async throws -> [AdminRatingDTO] {
        []
    }

    public func deleteRating(ratingId: String) async throws {}

    public func listUserAccess() async throws -> [AdminUserAccessDTO] {
        []
    }

    public func updateUserAccess(userId: UserID, isEnabled: Bool, disabledReason: String?) async throws {}

    public func listSourceRuns(limit: Int) async throws -> [AdminSourceRunDTO] {
        []
    }

    public func listTagQueueSummary() async throws -> [AdminTagQueueSummaryRowDTO] {
        []
    }

    public func listEvents(keyword: String?, status: String?, cityId: CityID?, limit: Int, offset: Int) async throws -> [AdminEventListItemDTO] {
        []
    }

    public func listEventFacets() async throws -> AdminEventFacetsDTO {
        AdminEventFacetsDTO(statusCounts: [:], cityCounts: [:])
    }

    public func bulkUpdateEventStatus(eventIds: [EventID], status: String) async throws {}

    public func bulkDeleteEvent(eventIds: [EventID]) async throws {}

    public func deleteEvent(eventId: EventID) async throws {}

    public func listEventAiTraces(eventId: EventID, limit: Int) async throws -> [AdminEventAiTraceDTO] {
        []
    }
}
