import Foundation
import FECore
import FEData

public final class FakeEventRepository: EventRepository, @unchecked Sendable {
    public var fetchByIDsResult: Result<[EventDTO], Error> = .success([])
    public var fetchListResult: Result<[EventDTO], Error> = .success([])
    public var artificialDelay: Duration?
    private(set) public var lastIDs: [EventID]?
    private(set) public var lastListQuery: EventQuery?
    private(set) public var lastUserID: UserID?
    private(set) public var fetchListCallCount = 0

    public init() {}

    public func fetch(ids: [EventID], for userID: UserID) async throws -> [EventDTO] {
        lastIDs = ids
        lastUserID = userID
        if let delay = artificialDelay { try await Task.sleep(for: delay) }
        return try fetchByIDsResult.get()
    }

    public func fetchList(query: EventQuery, for userID: UserID) async throws -> [EventDTO] {
        fetchListCallCount += 1
        lastListQuery = query
        lastUserID = userID
        if let delay = artificialDelay { try await Task.sleep(for: delay) }
        return try fetchListResult.get()
    }
}

extension EventDTO {
    /// Test fixture used by FEData + FEPlan + FEExplore tests.
    public static func fixture(
        id: String,
        title: String,
        isFree: Bool = true,
        price: Double? = nil,
        venueName: String? = nil,
        description: String? = nil
    ) -> EventDTO {
        EventDTO(
            id: EventID(id), title: title, description: description,
            startDatetime: Date(timeIntervalSince1970: 1_700_000_000),
            endDatetime: nil, timezone: "UTC",
            venueName: venueName, address: nil, cityID: nil,
            latitude: nil, longitude: nil, ageMin: nil, ageMax: nil,
            price: price, isFree: isFree, sourceURL: nil, sourceName: nil,
            sourceID: nil, images: [], status: "published",
            aiConfidence: nil, aiTagProvider: nil, isFeatured: false,
            viewCount: 0,
            createdAt: Date(timeIntervalSince1970: 1_700_000_000),
            updatedAt: Date(timeIntervalSince1970: 1_700_000_000),
            tags: [], avgRating: 0, ratingCount: 0, isFavorited: false
        )
    }
}
