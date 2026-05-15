import Foundation
#if canImport(CoreLocation)
import CoreLocation
#endif

#if canImport(CoreLocation)
/// Abstracts Apple's iOS 17+ `CLLocationUpdate.liveUpdates()` AsyncSequence so
/// `CoreLocationService` can be unit-tested without the real CoreLocation daemon.
/// Returns `AsyncStream<CLLocationUpdate>` (non-throwing, Sendable) — the concrete
/// impl swallows Apple's iterator errors and maps them to stream termination.
public protocol CLLocationUpdatesProvider: Sendable {
    func liveUpdates() -> AsyncStream<CLLocationUpdate>
}

/// Real impl. Wraps `CLLocationUpdate.liveUpdates()`. Iterator errors map to
/// stream termination (caller sees "no emit").
public final class RealCLLocationUpdatesProvider: CLLocationUpdatesProvider, Sendable {
    public init() {}
    public func liveUpdates() -> AsyncStream<CLLocationUpdate> {
        AsyncStream<CLLocationUpdate> { continuation in
            let task = Task {
                do {
                    for try await update in CLLocationUpdate.liveUpdates() {
                        continuation.yield(update)
                    }
                } catch {
                    // Swallow — Failure==Never on our seam.
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
#endif
