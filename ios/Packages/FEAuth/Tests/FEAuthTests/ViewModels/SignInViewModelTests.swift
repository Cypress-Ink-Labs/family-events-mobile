import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

@MainActor
final class SignInViewModelTests: XCTestCase {
    func testSuccessfulSignInUpdatesSessionStore() async throws {
        let fake = FakeAuthService()
        let session = AuthSession(userID: UserID("u_1"), accessToken: "a", refreshToken: "r", email: "x@y.z", identityProvider: .password)
        fake.signInResult = .success(session)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignInViewModel(authService: fake, sessionStore: store)
        vm.email = "x@y.z"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertNil(vm.errorMessage)
        XCTAssertFalse(vm.isSubmitting)
    }

    func testInvalidEmailBlocksSubmit() async {
        let fake = FakeAuthService()
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignInViewModel(authService: fake, sessionStore: store)
        vm.email = "no-at-sign"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertEqual(vm.errorMessage, "Please enter a valid email.")
    }

    func testInvalidCredentialsErrorIsSurfaced() async {
        let fake = FakeAuthService()
        fake.signInResult = .failure(AppError.invalidCredentials)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignInViewModel(authService: fake, sessionStore: store)
        vm.email = "x@y.z"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertEqual(vm.errorMessage, "Email or password is incorrect.")
    }
}
