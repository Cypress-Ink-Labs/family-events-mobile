import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

@MainActor
final class ResetPasswordViewModelTests: XCTestCase {
    func testSuccessfulReset() async {
        let fake = FakeAuthService()
        let vm = ResetPasswordViewModel(token: "tok_xyz", authService: fake)
        vm.newPassword = "longenough"
        await vm.submit()
        XCTAssertTrue(vm.didReset)
        XCTAssertNil(vm.errorMessage)
    }
    func testShortPasswordRejected() async {
        let fake = FakeAuthService()
        let vm = ResetPasswordViewModel(token: "tok_xyz", authService: fake)
        vm.newPassword = "short"
        await vm.submit()
        XCTAssertFalse(vm.didReset)
        XCTAssertEqual(vm.errorMessage, "Password must be at least 8 characters.")
    }
}
