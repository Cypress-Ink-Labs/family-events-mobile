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
}
