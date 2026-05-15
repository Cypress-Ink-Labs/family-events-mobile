import XCTest
import SwiftData
@testable import FEData

@MainActor
final class AppModelContainerTests: XCTestCase {
    /// Iron-rule regression: v3 schema (CachedEvent + CachedPlannedEvent) must construct
    /// against a fresh persistent store. M1's empty schema -> M3's two-model schema is the
    /// first non-trivial migration scenario.
    func testMakePersistentSucceedsOnFreshSchema() throws {
        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("fe-app-fresh-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: tempURL) }
        let container = try AppModelContainer.makePersistent(at: tempURL)
        XCTAssertNotNil(container.mainContext)
    }

    /// Iron-rule regression: opening a container against a URL that already has a SwiftData
    /// store from a prior session must succeed (no schema bump required for unchanged shape).
    func testMakePersistentSucceedsOnExistingStore() throws {
        let tempURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("fe-app-existing-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: tempURL) }
        // First open: create the store.
        do {
            _ = try AppModelContainer.makePersistent(at: tempURL)
        }
        // Second open against the same URL: simulates a previous-milestone store.
        let second = try AppModelContainer.makePersistent(at: tempURL)
        XCTAssertNotNil(second.mainContext)
    }
}
