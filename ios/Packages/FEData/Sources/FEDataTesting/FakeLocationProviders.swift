import Foundation
import FEData
#if canImport(CoreLocation)
import CoreLocation
#endif

public final class FakeLocationAuthorizationProvider: LocationAuthorizationProvider, @unchecked Sendable {
    public var currentStub: LocationAuthorizationStatus = .notDetermined
    public var requestStub: LocationAuthorizationStatus = .notDetermined
    private(set) public var requestCallCount = 0

    public init() {}

    public func current() async -> LocationAuthorizationStatus { currentStub }
    public func requestWhenInUse() async -> LocationAuthorizationStatus {
        requestCallCount += 1
        return requestStub
    }
}

#if canImport(CoreLocation)
public final class FakeCLLocationUpdatesProvider: CLLocationUpdatesProvider, @unchecked Sendable {
    public typealias Update = CLLocationUpdate
    private let makeStream: @Sendable () -> AsyncStream<CLLocationUpdate>

    public init(makeStream: @escaping @Sendable () -> AsyncStream<CLLocationUpdate>) {
        self.makeStream = makeStream
    }

    public func liveUpdates() -> AsyncStream<CLLocationUpdate> {
        makeStream()
    }
}
#endif
