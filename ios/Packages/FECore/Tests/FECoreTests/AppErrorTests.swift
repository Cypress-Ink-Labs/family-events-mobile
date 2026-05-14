import XCTest
@testable import FECore

final class AppErrorTests: XCTestCase {
    func testNetworkErrorCarriesUnderlying() {
        let underlying = NSError(domain: "test", code: 42)
        let err = AppError.network(underlying)
        XCTAssertEqual(err.userMessage, "Network problem. Please try again.")
    }

    func testUnauthorizedHasFriendlyMessage() {
        XCTAssertEqual(AppError.unauthorized.userMessage, "You're signed out. Please sign in again.")
    }

    func testNotFoundHasFriendlyMessage() {
        XCTAssertEqual(AppError.notFound.userMessage, "We couldn't find that.")
    }

    func testUnknownWrapsUnderlying() {
        let err = AppError.unknown(NSError(domain: "x", code: 0))
        XCTAssertTrue(err.userMessage.contains("Something went wrong"))
    }

    func testInvalidCredentialsHasFriendlyMessage() {
        XCTAssertEqual(AppError.invalidCredentials.userMessage, "Email or password is incorrect.")
    }

    func testEmailAlreadyInUseHasFriendlyMessage() {
        XCTAssertEqual(AppError.emailAlreadyInUse.userMessage, "An account with that email already exists.")
    }

    func testEmailNotConfirmedHasFriendlyMessage() {
        XCTAssertEqual(AppError.emailNotConfirmed.userMessage, "Please confirm your email before signing in.")
    }

    func testAppleSignInCancelledIsTreatedAsNoOp() {
        XCTAssertEqual(AppError.appleSignInCancelled.userMessage, "")
    }

    func testPasswordResetEmailSentIsInformational() {
        XCTAssertEqual(AppError.passwordResetEmailSent.userMessage, "Check your email for a reset link.")
    }
}
