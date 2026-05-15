import Foundation
import FECore
import FEData

public final class FakeEventRepository: EventRepository, @unchecked Sendable {
    public var fetchByIDsResult: Result<[EventDTO], Error> = .success([])
    private(set) public var lastIDs: [EventID]?
    private(set) public var lastUserID: UserID?
    public init() {}
    public func fetch(ids: [EventID], for userID: UserID) async throws -> [EventDTO] {
        lastIDs = ids
        lastUserID = userID
        return try fetchByIDsResult.get()
    }
}

extension EventDTO {
    /// Test fixture used by FEData + FEPlan tests.
    public static func fixture(id: String, title: String) -> EventDTO {
        EventDTO(
            id: EventID(id), title: title, description: nil,
            startDatetime: Date(timeIntervalSince1970: 1_700_000_000),
            endDatetime: nil, timezone: "UTC",
            venueName: nil, address: nil, cityID: nil,
            latitude: nil, longitude: nil, ageMin: nil, ageMax: nil,
            price: nil, isFree: true, sourceURL: nil, sourceName: nil,
            sourceID: nil, images: [], status: "published",
            aiConfidence: nil, aiTagProvider: nil, isFeatured: false,
            viewCount: 0,
            createdAt: Date(timeIntervalSince1970: 1_700_000_000),
            updatedAt: Date(timeIntervalSince1970: 1_700_000_000),
            tags: [], avgRating: 0, ratingCount: 0, isFavorited: false
        )
    }
}
