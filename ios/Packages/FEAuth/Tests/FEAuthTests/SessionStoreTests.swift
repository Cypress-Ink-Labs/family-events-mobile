import XCTest
@testable import FEAuth

@MainActor
final class SessionStoreTests: XCTestCase {
    func testInitialStateIsSignedOut() {
        let store = SessionStore()
        if case .signedOut = store.state {} else {
            XCTFail("expected initial state .signedOut, got \(store.state)")
        }
    }

    func testTransitionToSignedIn() {
        let store = SessionStore()
        store.markSignedIn(userID: "user_42")
        guard case .signedIn(let id) = store.state else {
            XCTFail("expected .signedIn, got \(store.state)")
            return
        }
        XCTAssertEqual(id, "user_42")
    }

    func testSignOutResets() {
        let store = SessionStore()
        store.markSignedIn(userID: "user_1")
        store.signOut()
        if case .signedOut = store.state {} else {
            XCTFail("expected .signedOut, got \(store.state)")
        }
    }
}
