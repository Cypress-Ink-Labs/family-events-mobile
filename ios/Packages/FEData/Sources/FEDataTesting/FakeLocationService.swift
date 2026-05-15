import Foundation
import FECore
import FEData

public final class FakeLocationService: LocationService, @unchecked Sendable {
    public var authorizationStub: LocationAuthorizationStatus = .notDetermined
    public var locationStub: GeoCoordinate?
    private(set) public var requestAuthorizationCallCount = 0
    private(set) public var currentLocationCallCount = 0

    public init() {}

    public func currentAuthorization() async -> LocationAuthorizationStatus { authorizationStub }
    public func requestAuthorization() async -> LocationAuthorizationStatus {
        requestAuthorizationCallCount += 1
        return authorizationStub
    }
    public func currentLocation() async -> GeoCoordinate? {
        currentLocationCallCount += 1
        return locationStub
    }
}
