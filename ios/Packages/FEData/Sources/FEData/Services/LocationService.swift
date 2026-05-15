import Foundation
import FECore

public enum LocationAuthorizationStatus: Sendable, Equatable {
    case notDetermined
    case denied
    case restricted
    case authorized
}

public protocol LocationService: Sendable {
    func currentAuthorization() async -> LocationAuthorizationStatus
    /// Prompts the user if `notDetermined`. Returns the resulting status.
    func requestAuthorization() async -> LocationAuthorizationStatus
    /// One-shot. Returns nil if denied/restricted, or if no fix is available within ~10s.
    func currentLocation() async -> GeoCoordinate?
}
