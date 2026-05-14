import XCTest
import FECore
@testable import FEAuth

@MainActor
final class LinkAccountViewModelTests: XCTestCase {
    func testCorrectPasswordSignsInExistingAccount() async {
        let fake = FakeAuthService()
        fake.signInResult = .success(.init(userID: UserID("u_exist"), accessToken: "a", refreshToken: "r", email: "x@y.z", identityProvider: .password))
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = LinkAccountViewModel(email: "x@y.z", authService: fake, sessionStore: store)
        vm.password = "longenough"
        await vm.submit()
        XCTAssertNil(vm.errorMessage)
    }

    func testWrongPasswordSurfacesError() async {
        let fake = FakeAuthService()
        fake.signInResult = .failure(AppError.invalidCredentials)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = LinkAccountViewModel(email: "x@y.z", authService: fake, sessionStore: store)
        vm.password = "wrong-but-long"
        await vm.submit()
        XCTAssertEqual(vm.errorMessage, AppError.invalidCredentials.userMessage)
    }
}
