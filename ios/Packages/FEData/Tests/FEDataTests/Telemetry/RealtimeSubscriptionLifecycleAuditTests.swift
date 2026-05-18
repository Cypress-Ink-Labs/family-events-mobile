import XCTest
@testable import FEData

final class RealtimeSubscriptionLifecycleAuditTests: XCTestCase {
    func testSnapshotReportsNoLeakAfterBalancedAttachDetach() async {
        let start = Date(timeIntervalSince1970: 0)
        let audit = RealtimeSubscriptionLifecycleAudit(now: start)

        await audit.recordAttach(now: start.addingTimeInterval(1))
        await audit.recordDetach(now: start.addingTimeInterval(2))
        await audit.recordDetach(now: start.addingTimeInterval(3))

        let snapshot = await audit.snapshot(now: start.addingTimeInterval(60))
        XCTAssertEqual(snapshot.activeSubscriptions, 0)
        XCTAssertEqual(snapshot.attachCount, 1)
        XCTAssertEqual(snapshot.detachCount, 1)
        XCTAssertFalse(snapshot.hasLeakedSubscriptions)
    }

    func testSnapshotReportsBoundedReconnectRate() async {
        let start = Date(timeIntervalSince1970: 0)
        let audit = RealtimeSubscriptionLifecycleAudit(now: start)

        await audit.recordReconnect(now: start.addingTimeInterval(10))
        await audit.recordReconnect(now: start.addingTimeInterval(20))

        let snapshot = await audit.snapshot(now: start.addingTimeInterval(60))
        XCTAssertEqual(snapshot.reconnectCount, 2)
        XCTAssertLessThanOrEqual(snapshot.reconnectRatePerMinute, 2)
        XCTAssertTrue(snapshot.batteryNetworkImpactSummary.contains("reconnects=2"))
    }
}
