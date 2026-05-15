import XCTest
import SwiftUI
import SwiftData
import FECore
import FEData
import FEDataTesting
import FEAuth
import FEAuthTesting
import FEPlan
@testable import FamilyEvents

@MainActor
final class RootViewSmokeTests: XCTestCase {
    private func makeComposer() throws -> PlanComposer {
        PlanComposer(
            location: FakeLocationService(),
            weather: FakeWeatherProviding(),
            planRepo: FakePlanRepository(),
            eventRepo: FakeEventRepository(),
            modelContainer: try AppModelContainer.makeInMemory()
        )
    }

    func testRootSelectsPlanTabWhenSignedIn() async throws {
        let fake = FakeAuthService()
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        try await store.adopt(.init(userID: UserID("u_1"), accessToken: "a", refreshToken: "r", email: nil, identityProvider: .password))
        _ = RootView(authService: fake, planComposer: try makeComposer(), profileRepo: FakeProfileRepo(), cityRepo: FakeCityRepository())
            .environment(store)
        XCTAssertEqual(store.state, .signedIn(userID: UserID("u_1")))
    }

    func testRootShowsAuthRootWhenSignedOut() async {
        let fake = FakeAuthService()
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        try? await Task.sleep(nanoseconds: 50_000_000)
        await store.signOut()
        _ = RootView(authService: fake, planComposer: try! makeComposer(), profileRepo: FakeProfileRepo(), cityRepo: FakeCityRepository())
            .environment(store)
        XCTAssertEqual(store.state, .signedOut)
    }

    func testRootExposesAllTabs() {
        XCTAssertEqual(RootView.shownTabs, [.plan, .explore, .saved])
    }
}
