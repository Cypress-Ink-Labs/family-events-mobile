import XCTest
import FECore
@testable import FEData
import FEDataTesting

#if canImport(CoreLocation)
import CoreLocation

final class CoreLocationServiceTests: XCTestCase {
    // Note: CLLocationUpdate has no public initializer in iOS 17 SDK. The Apple API
    // is consumed via `for try await update in CLLocationUpdate.liveUpdates()`. We
    // can't synthesize a CLLocationUpdate in a unit test, so the D8 "service-boundary"
    // tests focus on the seams we CAN drive: the auth provider (all 5 status paths)
    // and the timeout arm. CLLocationUpdate construction is left to a future
    // integration test that runs against a real simulator location daemon.

    func testReturnsNilWhenAuthorizationDenied() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.currentStub = .denied
        let updates = FakeCLLocationUpdatesProvider { AsyncStream { _ in } }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 1)
        let result = await service.currentLocation()
        XCTAssertNil(result)
    }

    func testReturnsNilWhenAuthorizationRestricted() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.currentStub = .restricted
        let updates = FakeCLLocationUpdatesProvider { AsyncStream { _ in } }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 1)
        let result = await service.currentLocation()
        XCTAssertNil(result)
    }

    func testReturnsNilWhenAuthorizationNotDetermined() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.currentStub = .notDetermined
        let updates = FakeCLLocationUpdatesProvider { AsyncStream { _ in } }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 1)
        let result = await service.currentLocation()
        XCTAssertNil(result)
    }

    func testTimeoutPathReturnsNilWhenNoEmit() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.currentStub = .authorized
        // Stream that never emits and never finishes — exercise the timeout arm.
        let updates = FakeCLLocationUpdatesProvider {
            AsyncStream<CLLocationUpdate> { _ in
                // never yield, never finish; the consumer's iteration awaits.
            }
        }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 0.2)
        let start = Date()
        let result = await service.currentLocation()
        let elapsed = Date().timeIntervalSince(start)
        XCTAssertNil(result)
        XCTAssertGreaterThan(elapsed, 0.1)
        XCTAssertLessThan(elapsed, 1.5)
    }

    func testRequestAuthorizationDelegatesToProvider() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.requestStub = .authorized
        let updates = FakeCLLocationUpdatesProvider { AsyncStream { _ in } }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 1)
        let result = await service.requestAuthorization()
        XCTAssertEqual(result, .authorized)
        XCTAssertEqual(auth.requestCallCount, 1)
    }

    func testCurrentAuthorizationDelegatesToProvider() async {
        let auth = FakeLocationAuthorizationProvider()
        auth.currentStub = .denied
        let updates = FakeCLLocationUpdatesProvider { AsyncStream { _ in } }
        let service = CoreLocationService(auth: auth, updates: updates, timeoutSeconds: 1)
        let result = await service.currentAuthorization()
        XCTAssertEqual(result, .denied)
    }
}
#endif
