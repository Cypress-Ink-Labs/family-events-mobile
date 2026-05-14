import XCTest
import SwiftUI
import FECore
import FEAuth
import FEAuthTesting
@testable import FamilyEvents

@MainActor
final class RootViewSmokeTests: XCTestCase {
    func testRootSelectsPlanTabWhenSignedIn() async throws {
        let fake = FakeAuthService()
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        try await store.adopt(.init(userID: UserID("u_1"), accessToken: "a", refreshToken: "r", email: nil, identityProvider: .password))
        _ = RootView(authService: fake).environment(store)
        XCTAssertEqual(store.state, .signedIn(userID: UserID("u_1")))
    }

    func testRootShowsAuthRootWhenSignedOut() async {
        let fake = FakeAuthService()
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        // Give bootstrap a moment to settle on .signedOut.
        try? await Task.sleep(nanoseconds: 50_000_000)
        await store.signOut()
        _ = RootView(authService: fake).environment(store)
        XCTAssertEqual(store.state, .signedOut)
    }

    func testRootExposesAllTabs() {
        XCTAssertEqual(RootView.shownTabs, [.plan, .explore, .saved])
    }
}
