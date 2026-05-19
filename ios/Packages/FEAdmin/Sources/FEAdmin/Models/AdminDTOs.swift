import Foundation
import FECore

public struct AdminStatsDTO: Equatable, Sendable, Codable {
    public let totalEvents: Int
    public let pendingReview: Int
    public let published: Int
    public let activeSources: Int
    public let sourceErrors: Int
    public let aiHigh: Int
    public let aiMedium: Int
    public let aiLow: Int
    public let aiUntagged: Int
}

public struct AdminSectionDTO: Equatable, Sendable, Codable {
    public let id: String
    public let title: String
    public let description: String
}

public struct AdminInviteCodeResultDTO: Equatable, Sendable, Codable {
    public let id: String
    public let code: String
    public let maxUses: Int
    public let expiresAt: Date?
    public let notes: String?
    public let createdAt: Date
}

public struct AdminInviteApprovalDTO: Equatable, Sendable, Codable {
    public let requestId: String
    public let code: String
    public let inviteCodeId: String
    public let email: String
    public let createdAt: Date
}

public struct AdminCronJobDTO: Equatable, Sendable, Codable {
    public let jobId: Int64
    public let jobName: String
    public let schedule: String
    public let command: String
    public let active: Bool
    public let lastRunStart: Date?
    public let lastRunEnd: Date?
    public let lastRunStatus: String?
    public let lastRunMessage: String?
}

public struct AdminCommentDTO: Equatable, Sendable, Codable {
    public let id: String
    public let userId: UserID
    public let eventId: EventID
    public let body: String
    public let isApproved: Bool
    public let isFlagged: Bool
    public let createdAt: Date
    public let authorDisplayName: String?
    public let eventTitle: String?
}

public struct AdminCronRunDTO: Equatable, Sendable, Codable {
    public let runId: Int64
    public let jobName: String
    public let status: String
    public let returnMessage: String?
    public let startTime: Date
    public let endTime: Date?
    public let durationMs: Double?
}

public struct AdminSourceDTO: Equatable, Sendable, Codable {
    public let id: String
    public let name: String
    public let cityId: CityID?
    public let url: String?
    public let isActive: Bool
    public let autoApprove: Bool
    public let lastStatus: String?
    public let lastScrapedAt: Date?
}

public struct AdminInviteCodeListDTO: Equatable, Sendable, Codable {
    public let id: String
    public let maxUses: Int
    public let usedCount: Int
    public let expiresAt: Date?
    public let notes: String?
    public let createdAt: Date
    public let revokedAt: Date?
}

public struct AdminInviteRequestDTO: Equatable, Sendable, Codable {
    public let id: String
    public let email: String
    public let message: String?
    public let status: String
    public let createdAt: Date
    public let reviewedAt: Date?
    public let adminNotes: String?
}

public struct AdminCityDTO: Equatable, Sendable, Codable {
    public let id: CityID
    public let name: String
    public let state: String?
    public let country: String
    public let slug: String
    public let isActive: Bool
    public let timezone: String
    public let latitude: Double?
    public let longitude: Double?
    public let createdAt: Date
}

public struct AdminRatingDTO: Equatable, Sendable, Codable {
    public let id: String
    public let userId: UserID
    public let eventId: EventID
    public let score: Int
    public let createdAt: Date
    public let authorDisplayName: String?
    public let eventTitle: String?
}

public struct AdminUserAccessDTO: Equatable, Sendable, Codable {
    public let userId: UserID
    public let isEnabled: Bool
    public let accessExpiresAt: Date?
    public let enabledAt: Date?
    public let disabledAt: Date?
    public let disabledReason: String?
    public let displayName: String?
    public let email: String?
    public let role: String
}

public struct AdminSourceRunDTO: Equatable, Sendable, Codable {
    public let id: String
    public let sourceId: String?
    public let sourceName: String?
    public let startedAt: Date
    public let completedAt: Date?
    public let status: String
    public let eventsFound: Int
    public let eventsImported: Int
    public let eventsSkipped: Int
    public let errorLog: String?
}

public struct AdminTagQueueSummaryRowDTO: Equatable, Sendable, Codable {
    public let status: String
    public let rowCount: Int
    public let oldestEnqueuedAt: Date?
    public let newestEnqueuedAt: Date?
    public let lastDeadLetterAt: Date?
    public let avgAttempts: Double?
}

public struct AdminTagDTO: Equatable, Sendable, Codable {
    public let id: String
    public let name: String
    public let slug: String?
}

public struct AdminEventListItemDTO: Equatable, Sendable, Codable {
    public let id: EventID
    public let title: String
    public let description: String?
    public let startsAt: Date
    public let endsAt: Date?
    public let venueName: String?
    public let cityId: CityID?
    public let cityName: String?
    public let status: String
    public let aiConfidence: Double?
    public let price: Double?
    public let isFree: Bool
    public let ageMin: Int?
    public let ageMax: Int?
    public let imageUrl: String?
    public let sourceName: String?
    public let createdAt: Date
    public let updatedAt: Date
}

public struct AdminEventFacetsDTO: Equatable, Sendable, Codable {
    public let statusCounts: [String: Int]
    public let cityCounts: [String: Int]
}

public struct AdminEventAiTraceDTO: Equatable, Sendable, Codable {
    public let id: String
    public let eventId: EventID
    public let provider: String?
    public let model: String?
    public let createdAt: Date
    public let inputSummary: String?
    public let outputSummary: String?
    public let confidence: Double?
}
