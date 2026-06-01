import Foundation
#if canImport(CoreLocation)
import CoreLocation
#endif

/// Abstracts the CLLocationManager auth API so CoreLocationService is testable
/// without a real manager. Maps to LocationAuthorizationStatus.
public protocol LocationAuthorizationProvider: Sendable {
    func current() async -> LocationAuthorizationStatus
    func requestWhenInUse() async -> LocationAuthorizationStatus
}

#if canImport(CoreLocation)
/// Real impl over CLLocationManager. The continuation-based requestWhenInUse
/// is unavoidable: CLLocationManager.requestWhenInUseAuthorization() is fire-and-
/// forget; the result arrives via the delegate callback. We bridge once and
/// resolve. MainActor-isolated because CLLocationManager requires main thread
/// and it serializes access to the `pending` continuation.
@MainActor
public final class CLLocationManagerAuthorizationProvider: NSObject, LocationAuthorizationProvider, CLLocationManagerDelegate, Sendable {
    nonisolated(unsafe) private let manager: CLLocationManager
    private var pending: CheckedContinuation<LocationAuthorizationStatus, Never>?

    nonisolated public init(manager: CLLocationManager = CLLocationManager()) {
        self.manager = manager
        super.init()
        self.manager.delegate = self
    }

    public func current() async -> LocationAuthorizationStatus {
        Self.translate(manager.authorizationStatus)
    }

    public func requestWhenInUse() async -> LocationAuthorizationStatus {
        if manager.authorizationStatus != .notDetermined {
            return Self.translate(manager.authorizationStatus)
        }
        if pending != nil {
            return Self.translate(manager.authorizationStatus)
        }
        return await withCheckedContinuation { continuation in
            pending = continuation
            manager.requestWhenInUseAuthorization()
        }
    }

    nonisolated public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            guard let cont = self.pending else { return }
            self.pending = nil
            cont.resume(returning: Self.translate(manager.authorizationStatus))
        }
    }

    private static func translate(_ status: CLAuthorizationStatus) -> LocationAuthorizationStatus {
        switch status {
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .restricted: return .restricted
        case .authorizedAlways, .authorizedWhenInUse: return .authorized
        @unknown default: return .denied
        }
    }
}
#endif
