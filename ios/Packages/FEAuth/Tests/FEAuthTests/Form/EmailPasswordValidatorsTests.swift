import XCTest
@testable import FEAuth

final class EmailPasswordValidatorsTests: XCTestCase {
    func testValidEmailAccepted() {
        XCTAssertNil(EmailPasswordValidators.emailError("alice@example.com"))
        XCTAssertNil(EmailPasswordValidators.emailError("a.b+c@sub.domain.co.uk"))
    }
    func testInvalidEmailRejected() {
        XCTAssertNotNil(EmailPasswordValidators.emailError(""))
        XCTAssertNotNil(EmailPasswordValidators.emailError("no-at-sign"))
        XCTAssertNotNil(EmailPasswordValidators.emailError("trailing@"))
        XCTAssertNotNil(EmailPasswordValidators.emailError("@no-local"))
    }
    func testPasswordMinLength() {
        XCTAssertNotNil(EmailPasswordValidators.passwordError(""))
        XCTAssertNotNil(EmailPasswordValidators.passwordError("short"))
        XCTAssertNil(EmailPasswordValidators.passwordError("longenough"))
    }
}
