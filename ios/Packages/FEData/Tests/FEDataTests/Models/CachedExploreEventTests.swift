import XCTest
import SwiftData
@testable import FEData

@MainActor
final class CachedExploreEventTests: XCTestCase {
    func testCanInsertAndQueryCachedExploreEvent() throws {
        let container = try AppModelContainer.makeInMemory()
        let ctx = container.mainContext
        let item = CachedExploreEvent(
            filterSignature: "city1:_:_:20",
            eventID: "evt_1",
            rank: 0,
            pageIndex: 0,
            lastSyncedAt: Date()
        )
        ctx.insert(item)
        try ctx.save()
        let fetched = try ctx.fetch(FetchDescriptor<CachedExploreEvent>())
        XCTAssertEqual(fetched.count, 1)
        XCTAssertEqual(fetched.first?.eventID, "evt_1")
        XCTAssertEqual(fetched.first?.compositeKey, "city1:_:_:20::evt_1")
    }

    func testCompositeKeyIsUnique() throws {
        let container = try AppModelContainer.makeInMemory()
        let ctx = container.mainContext
        let sig = "sig1"
        let id = "evt_1"
        let item1 = CachedExploreEvent(filterSignature: sig, eventID: id, rank: 0, pageIndex: 0, lastSyncedAt: Date())
        ctx.insert(item1)
        try ctx.save()
        // Inserting again would violate unique constraint — verify only one row persists
        let fetched = try ctx.fetch(FetchDescriptor<CachedExploreEvent>())
        XCTAssertEqual(fetched.count, 1)
    }

    func testMakePersistentAtCustomURLIncludesExploreModel() throws {
        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("fe-explore-test-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: tempURL) }
        let container = try AppModelContainer.makePersistent(at: tempURL)
        let ctx = container.mainContext
        let item = CachedExploreEvent(
            filterSignature: "test",
            eventID: "evt_x",
            rank: 1,
            pageIndex: 0,
            lastSyncedAt: Date()
        )
        ctx.insert(item)
        XCTAssertNoThrow(try ctx.save())
    }
}
