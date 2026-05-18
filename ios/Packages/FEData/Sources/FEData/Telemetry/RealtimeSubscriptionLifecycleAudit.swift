import Foundation

public struct RealtimeSubscriptionAuditSnapshot: Equatable, Sendable {
    public let activeSubscriptions: Int
    public let attachCount: Int
    public let detachCount: Int
    public let reconnectCount: Int
    public let startedAt: Date
    public let updatedAt: Date

    public var hasLeakedSubscriptions: Bool {
        activeSubscriptions != 0
    }

    public var attachRatePerMinute: Double {
        ratePerMinute(attachCount)
    }

    public var detachRatePerMinute: Double {
        ratePerMinute(detachCount)
    }

    public var reconnectRatePerMinute: Double {
        ratePerMinute(reconnectCount)
    }

    public var batteryNetworkImpactSummary: String {
        let reconnects = String(format: "%.2f", reconnectRatePerMinute)
        return "active=\(activeSubscriptions), attaches=\(attachCount), detaches=\(detachCount), reconnects=\(reconnectCount), reconnects_per_min=\(reconnects)"
    }

    private func ratePerMinute(_ count: Int) -> Double {
        let elapsed = max(updatedAt.timeIntervalSince(startedAt), 1)
        return Double(count) / elapsed * 60
    }
}

public actor RealtimeSubscriptionLifecycleAudit {
    private let startedAt: Date
    private var updatedAt: Date
    private var activeSubscriptions = 0
    private var attachCount = 0
    private var detachCount = 0
    private var reconnectCount = 0

    public init(now: Date = Date()) {
        self.startedAt = now
        self.updatedAt = now
    }

    public func recordAttach(now: Date = Date()) {
        attachCount += 1
        activeSubscriptions += 1
        updatedAt = now
    }

    public func recordDetach(now: Date = Date()) {
        guard activeSubscriptions > 0 else {
            updatedAt = now
            return
        }
        detachCount += 1
        activeSubscriptions -= 1
        updatedAt = now
    }

    public func recordReconnect(now: Date = Date()) {
        reconnectCount += 1
        updatedAt = now
    }

    public func snapshot(now: Date = Date()) -> RealtimeSubscriptionAuditSnapshot {
        RealtimeSubscriptionAuditSnapshot(
            activeSubscriptions: activeSubscriptions,
            attachCount: attachCount,
            detachCount: detachCount,
            reconnectCount: reconnectCount,
            startedAt: startedAt,
            updatedAt: max(updatedAt, now)
        )
    }
}
