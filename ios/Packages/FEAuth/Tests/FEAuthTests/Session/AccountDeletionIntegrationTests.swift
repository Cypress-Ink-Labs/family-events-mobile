import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

@MainActor
final class AccountDeletionIntegrationTests: XCTestCase {
    func testDeleteAccountSignsOutAndClearsStorage() async throws {
        let fake = FakeAuthService()
        let storage = InMemoryKeychainStorage()
        try await storage.setString("a", for: .accessToken)
        try await storage.setString("r", for: .refreshToken)
        try await storage.setString("u_1", for: .userID)
        let store = SessionStore(authService: fake, storage: storage)

        try await store.adopt(.init(userID: UserID("u_1"), accessToken: "a", refreshToken: "r", email: "x@y.z", identityProvider: .password))
        XCTAssertEqual(store.state, .signedIn(userID: UserID("u_1")))

        try await fake.deleteAccount()
        await store.signOut()

        XCTAssertEqual(fake.deleteAccountCallCount, 1)
        XCTAssertEqual(fake.signOutCallCount, 1)
        XCTAssertEqual(store.state, .signedOut)
        let leftoverAccess = try await storage.string(for: .accessToken)
        XCTAssertNil(leftoverAccess)
    }
}
