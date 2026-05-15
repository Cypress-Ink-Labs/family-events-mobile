import XCTest
import SwiftData
import FECore
@testable import FEData

@MainActor
final class CachedEventTests: XCTestCase {
    func testCanInsertAndQueryCachedEvent() throws {
        let container = try AppModelContainer.makeInMemory()
        let ctx = container.mainContext
        let event = CachedEvent(
            id: "evt_1", title: "Storytime", startDatetime: Date(),
            venueName: "Library", latitude: 30.0, longitude: -97.7,
            isFree: true, imageURLs: ["x.jpg"], lastSyncedAt: Date()
        )
        ctx.insert(event)
        try ctx.save()
        let fetched = try ctx.fetch(FetchDescriptor<CachedEvent>())
        XCTAssertEqual(fetched.count, 1)
        XCTAssertEqual(fetched.first?.title, "Storytime")
    }

    func testCachedPlannedEventLinks() throws {
        let container = try AppModelContainer.makeInMemory()
        let ctx = container.mainContext
        let plan = CachedPlannedEvent(
            eventID: "evt_1", dayOffset: 0, score: 0.8,
            distanceKm: 2.1, lastSyncedAt: Date(), rank: 0
        )
        ctx.insert(plan)
        try ctx.save()
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 1)
    }

    func testMakePersistentAtCustomURLSucceeds() throws {
        // Iron-rule regression: makePersistent must accept a fresh store URL.
        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("fe-test-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: tempURL) }
        let container = try AppModelContainer.makePersistent(at: tempURL)
        XCTAssertNotNil(container.mainContext)
    }

    func testUpsertReplacesExistingRow() throws {
        let container = try AppModelContainer.makeInMemory()
        let ctx = container.mainContext
        let dto = EventDTO(id: EventID("evt_1"), title: "Storytime", description: nil,
                           startDatetime: Date(), endDatetime: nil, timezone: "UTC",
                           venueName: "Library", address: nil, cityID: nil,
                           latitude: nil, longitude: nil, ageMin: nil, ageMax: nil,
                           price: nil, isFree: true, sourceURL: nil, sourceName: nil,
                           sourceID: nil, images: [], status: "published",
                           aiConfidence: nil, aiTagProvider: nil, isFeatured: false,
                           viewCount: 0, createdAt: Date(), updatedAt: Date(),
                           tags: [], avgRating: 0, ratingCount: 0, isFavorited: false)
        CachedEvent.upsert(dto, in: ctx, at: Date())
        CachedEvent.upsert(dto, in: ctx, at: Date()) // second upsert must not duplicate
        try ctx.save()
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 1)
    }
}
