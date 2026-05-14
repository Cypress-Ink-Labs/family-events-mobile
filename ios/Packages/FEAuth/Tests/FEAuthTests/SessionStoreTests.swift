import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

@MainActor
final class SessionStoreTests: XCTestCase {
    func testInitialStateIsSignedOut() async throws {
        let store = SessionStore(authService: FakeAuthService(), storage: InMemoryKeychainStorage())
        // Let bootstrap complete with empty storage → .signedOut
        try await Task.sleep(nanoseconds: 50_000_000)
        if case .signedOut = store.state {} else {
            XCTFail("expected initial state .signedOut, got \(store.state)")
        }
    }

    func testTransitionToSignedIn() async throws {
        let store = SessionStore(authService: FakeAuthService(), storage: InMemoryKeychainStorage())
        let session = AuthSession(
            userID: UserID("user_42"),
            accessToken: "access",
            refreshToken: "refresh",
            email: nil,
            identityProvider: .password
        )
        try await store.adopt(session)
        guard case .signedIn(let id) = store.state else {
            XCTFail("expected .signedIn, got \(store.state)")
            return
        }
        XCTAssertEqual(id, UserID("user_42"))
    }

    func testSignOutResets() async throws {
        let store = SessionStore(authService: FakeAuthService(), storage: InMemoryKeychainStorage())
        let session = AuthSession(
            userID: UserID("user_1"),
            accessToken: "access",
            refreshToken: "refresh",
            email: nil,
            identityProvider: .password
        )
        try await store.adopt(session)
        await store.signOut()
        if case .signedOut = store.state {} else {
            XCTFail("expected .signedOut, got \(store.state)")
        }
    }

    func testInitialStateIsHydratingWhenStorageHasTokens() async throws {
        let storage = InMemoryKeychainStorage()
        try await storage.setString("access", for: .accessToken)
        try await storage.setString("refresh", for: .refreshToken)
        try await storage.setString("u_1", for: .userID)
        let fake = FakeAuthService()
        fake.restoreResult = .success(.init(userID: UserID("u_1"), accessToken: "access", refreshToken: "refresh", email: nil, identityProvider: .password))
        let store = await SessionStore(authService: fake, storage: storage)
        // Bootstrap is fire-and-forget; the very-first read may catch `.hydrating`
        // OR `.signedIn` depending on timing. Assert one of those two.
        let snapshot = await store.state
        XCTAssertTrue(snapshot == .hydrating || snapshot == .signedIn(userID: UserID("u_1")),
                      "unexpected initial state \(snapshot)")
    }

    func testLinkRequiredStateCarriesIdToken() async {
        let store = await SessionStore(authService: FakeAuthService(), storage: InMemoryKeychainStorage())
        await store.markLinkRequired(email: "x@y.z", appleIdToken: "tok", nonce: "n")
        if case .linkRequired(let email, _, _) = await store.state {
            XCTAssertEqual(email, "x@y.z")
        } else {
            XCTFail("expected .linkRequired")
        }
    }

    func testSignInWithAppleSucceeds() async throws {
        let fake = FakeAuthService()
        let session = AuthSession(userID: UserID("u_apple"), accessToken: "a", refreshToken: "r", email: "x@y.z", identityProvider: .apple)
        fake.signInWithAppleResult = .success(session)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        try await store.completeAppleSignIn(idToken: "tok", nonce: "n", email: "x@y.z")
        if case .signedIn(let uid) = store.state {
            XCTAssertEqual(uid, UserID("u_apple"))
        } else {
            XCTFail("expected .signedIn")
        }
    }

    func testSignInWithAppleEmailCollisionTransitionsToLinkRequired() async throws {
        let fake = FakeAuthService()
        fake.signInWithAppleResult = .failure(AppError.emailAlreadyInUse)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        try await store.completeAppleSignIn(idToken: "tok", nonce: "n", email: "x@y.z")
        if case .linkRequired(let email, _, _) = store.state {
            XCTAssertEqual(email, "x@y.z")
        } else {
            XCTFail("expected .linkRequired")
        }
    }
}
