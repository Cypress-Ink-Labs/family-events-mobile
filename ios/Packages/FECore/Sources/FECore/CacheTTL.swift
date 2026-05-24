import Foundation

/// Per-domain cache freshness windows. Aligned with the web client's
/// TanStack Query `staleTime` defaults so iOS and web don't drift visibly
/// for the same signed-in user across devices.
public enum CacheTTL {
    /// Default freshness window for most queries (matches web `staleTime: 60_000`).
    public static let `default`: TimeInterval = 60
    /// Plan / Saturday-plan content updates less often.
    public static let plan: TimeInterval = 120
    /// Comments are cheap; refresh aggressively.
    public static let comments: TimeInterval = 30

    /// Returns `true` when the given timestamp is within the TTL window from
    /// `now`. `nil` last-fetch (i.e. never fetched) is always stale.
    public static func isFresh(
        lastFetchedAt: Date?,
        ttl: TimeInterval,
        now: Date = Date()
    ) -> Bool {
        guard let lastFetchedAt else { return false }
        let age = now.timeIntervalSince(lastFetchedAt)
        // Negative age means the stored timestamp is in the future (e.g. clock
        // drift after a device time change). Treat as stale to force a refetch.
        guard age >= 0 else { return false }
        return age < ttl
    }
}
