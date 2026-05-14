import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

@MainActor
final class ForgotPasswordViewModelTests: XCTestCase {
    func testSubmitSendsResetEmail() async {
        let fake = FakeAuthService()
        let vm = ForgotPasswordViewModel(authService: fake)
        vm.email = "a@b.c"
        await vm.submit()
        XCTAssertTrue(vm.emailSent)
        XCTAssertNil(vm.errorMessage)
    }
    func testBadEmailRejected() async {
        let fake = FakeAuthService()
        let vm = ForgotPasswordViewModel(authService: fake)
        vm.email = "nope"
        await vm.submit()
        XCTAssertFalse(vm.emailSent)
        XCTAssertEqual(vm.errorMessage, "Please enter a valid email.")
    }
}
