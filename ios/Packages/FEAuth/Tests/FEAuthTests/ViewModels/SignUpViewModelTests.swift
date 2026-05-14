import XCTest
import FECore
@testable import FEAuth

@MainActor
final class SignUpViewModelTests: XCTestCase {
    func testSuccessfulSignUpSignsIn() async throws {
        let fake = FakeAuthService()
        fake.signUpResult = .success(.init(userID: UserID("u_2"), accessToken: "a", refreshToken: "r", email: "a@b.c", identityProvider: .password))
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignUpViewModel(authService: fake, sessionStore: store)
        vm.email = "a@b.c"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertNil(vm.errorMessage)
        XCTAssertFalse(vm.pendingConfirmation)
    }

    func testSignUpRequiringConfirmation() async {
        let fake = FakeAuthService()
        fake.signUpResult = .failure(AppError.emailNotConfirmed)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignUpViewModel(authService: fake, sessionStore: store)
        vm.email = "a@b.c"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertTrue(vm.pendingConfirmation)
        XCTAssertNil(vm.errorMessage)
    }

    func testEmailAlreadyInUseSurfacesError() async {
        let fake = FakeAuthService()
        fake.signUpResult = .failure(AppError.emailAlreadyInUse)
        let store = SessionStore(authService: fake, storage: InMemoryKeychainStorage())
        let vm = SignUpViewModel(authService: fake, sessionStore: store)
        vm.email = "a@b.c"
        vm.password = "longenough"
        await vm.submit()
        XCTAssertEqual(vm.errorMessage, AppError.emailAlreadyInUse.userMessage)
    }
}
