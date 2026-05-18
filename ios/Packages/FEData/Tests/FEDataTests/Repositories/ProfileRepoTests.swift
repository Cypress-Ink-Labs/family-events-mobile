import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class ProfileRepoProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredProfile() async throws {
        let fake = FakeProfileRepo()
        fake.profileResult = .success(UserProfile(
            id: UserID("u_1"),
            email: "user@example.com",
            displayName: "Taylor",
            avatarURL: "https://example.com/a.png",
            cityPreferenceID: CityID("city_aus"),
            childName: "Leo",
            childAge: 5
        ))

        let got = try await fake.profile(userID: UserID("u_1"))

        XCTAssertEqual(got?.displayName, "Taylor")
        XCTAssertEqual(got?.childName, "Leo")
        XCTAssertEqual(got?.cityPreferenceID, CityID("city_aus"))
        XCTAssertEqual(fake.lastUserID, UserID("u_1"))
    }

    func testFakeUpdateCapturesEditableFields() async throws {
        let fake = FakeProfileRepo()
        let update = UserProfileUpdate(
            displayName: "Taylor",
            cityPreferenceID: CityID("city_aus"),
            childName: "Leo",
            childAge: 5
        )

        let got = try await fake.updateProfile(update, for: UserID("u_1"))

        XCTAssertEqual(fake.lastUpdate, update)
        XCTAssertEqual(got.displayName, "Taylor")
        XCTAssertEqual(got.childAge, 5)
    }

    func testFakeReturnsConfiguredContext() async throws {
        let fake = FakeProfileRepo()
        fake.contextResult = .success((cityID: CityID("city_aus"), kidAge: 5))
        let got = try await fake.currentContext(userID: UserID("u_1"))
        XCTAssertEqual(got.cityID, CityID("city_aus"))
        XCTAssertEqual(got.kidAge, 5)
        XCTAssertEqual(fake.lastUserID, UserID("u_1"))
    }

    func testFakeReturnsEmptyContext() async throws {
        let fake = FakeProfileRepo()
        let got = try await fake.currentContext(userID: UserID("u_no_profile"))
        XCTAssertNil(got.cityID)
        XCTAssertNil(got.kidAge)
    }
}
