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
/// resolve.
public final class CLLocationManagerAuthorizationProvider: NSObject, LocationAuthorizationProvider, CLLocationManagerDelegate, @unchecked Sendable {
    private let manager: CLLocationManager
    private var pending: CheckedContinuation<LocationAuthorizationStatus, Never>?

    public init(manager: CLLocationManager = CLLocationManager()) {
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
        return await withCheckedContinuation { continuation in
            pending = continuation
            manager.requestWhenInUseAuthorization()
        }
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard let cont = pending else { return }
        pending = nil
        cont.resume(returning: Self.translate(manager.authorizationStatus))
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
