import XCTest
@testable import FEAuth

final class AppleSignInCoordinatorTests: XCTestCase {
    func testNonceIsLongAndAlphanumeric() {
        let n1 = AppleSignInCoordinator.generateNonce()
        let n2 = AppleSignInCoordinator.generateNonce()
        XCTAssertGreaterThanOrEqual(n1.count, 32)
        XCTAssertNotEqual(n1, n2)
        for ch in n1 {
            XCTAssertTrue(ch.isLetter || ch.isNumber || ch == "-" || ch == "_" || ch == ".")
        }
    }

    func testHashesNonceWithSHA256() {
        let nonce = "deadbeef"
        let hashed = AppleSignInCoordinator.sha256(nonce)
        XCTAssertEqual(hashed.count, 64)
    }
}
