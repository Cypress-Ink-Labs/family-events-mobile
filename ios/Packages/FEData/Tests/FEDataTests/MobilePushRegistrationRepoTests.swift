import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class MobilePushRegistrationRepoTests: XCTestCase {
    func testFakeRegistersMobileTokenWithPlatformAndUser() async throws {
        let repo = FakeMobilePushRegistrationRepo()
        let userID = UserID("user-1")

        try await repo.registerMobilePushToken("apns-token", platform: .ios, for: userID)

        XCTAssertEqual(repo.lastToken, "apns-token")
        XCTAssertEqual(repo.lastPlatform, .ios)
        XCTAssertEqual(repo.lastUserID, userID)
        XCTAssertEqual(repo.registerCallCount, 1)
    }
}
