import XCTest
@testable import FECore

final class CacheTTLTests: XCTestCase {
    func test_isFresh_nilLastFetch_isAlwaysStale() {
        XCTAssertFalse(CacheTTL.isFresh(lastFetchedAt: nil, ttl: 60))
    }

    func test_isFresh_withinWindow_isFresh() {
        let now = Date()
        let lastFetch = now.addingTimeInterval(-30)
        XCTAssertTrue(CacheTTL.isFresh(lastFetchedAt: lastFetch, ttl: 60, now: now))
    }

    func test_isFresh_pastWindow_isStale() {
        let now = Date()
        let lastFetch = now.addingTimeInterval(-90)
        XCTAssertFalse(CacheTTL.isFresh(lastFetchedAt: lastFetch, ttl: 60, now: now))
    }

    func test_defaults_matchWebStaleTime() {
        XCTAssertEqual(CacheTTL.default, 60)
        XCTAssertEqual(CacheTTL.plan, 120)
        XCTAssertEqual(CacheTTL.comments, 30)
    }
}
