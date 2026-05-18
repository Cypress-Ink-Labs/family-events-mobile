import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEAuth
import FEAuthTesting

@MainActor
final class ProfileViewModelTests: XCTestCase {
    func testLoadPopulatesProfileAndCities() async {
        let profile = UserProfile(
            id: UserID("u_1"),
            email: "user@example.com",
            displayName: "Taylor",
            avatarURL: nil,
            cityPreferenceID: CityID("city_chi"),
            childName: "Leo",
            childAge: 6
        )
        let profileRepo = FakeProfileRepo()
        profileRepo.profileResult = .success(profile)
        let cityRepo = FakeCityRepository()
        cityRepo.citiesResult = .success([CitySummary(id: CityID("city_chi"), name: "Chicago", region: "IL")])
        let model = ProfileViewModel(
            userID: UserID("u_1"),
            profileRepo: profileRepo,
            cityRepo: cityRepo,
            authService: FakeAuthService()
        )

        await model.load()

        XCTAssertEqual(model.displayName, "Taylor")
        XCTAssertEqual(model.childName, "Leo")
        XCTAssertEqual(model.childAgeText, "6")
        XCTAssertEqual(model.selectedCityID, CityID("city_chi"))
        XCTAssertEqual(model.cities.map(\.name), ["Chicago"])
    }

    func testLoadPreservesProfileWhenCityFetchFails() async {
        let profile = UserProfile(
            id: UserID("u_1"),
            email: "user@example.com",
            displayName: "Jordan",
            avatarURL: nil,
            cityPreferenceID: CityID("city_nyc"),
            childName: "Sam",
            childAge: 8
        )
        let profileRepo = FakeProfileRepo()
        profileRepo.profileResult = .success(profile)
        let cityRepo = FakeCityRepository()
        cityRepo.citiesResult = .failure(AppError.networkError)
        let model = ProfileViewModel(
            userID: UserID("u_1"),
            profileRepo: profileRepo,
            cityRepo: cityRepo,
            authService: FakeAuthService()
        )

        await model.load()

        XCTAssertEqual(model.displayName, "Jordan")
        XCTAssertEqual(model.childName, "Sam")
        XCTAssertEqual(model.childAgeText, "8")
        XCTAssertEqual(model.selectedCityID, CityID("city_nyc"))
        XCTAssertTrue(model.cities.isEmpty)
        XCTAssertNotNil(model.errorMessage)
    }

    func testSaveValidatesChildAge() async {
        let profileRepo = FakeProfileRepo()
        let model = ProfileViewModel(
            userID: UserID("u_1"),
            profileRepo: profileRepo,
            cityRepo: FakeCityRepository(),
            authService: FakeAuthService()
        )
        model.childAgeText = "19"

        let saved = await model.save()

        XCTAssertNil(saved)
        XCTAssertEqual(model.errorMessage, "Child's age must be between 0 and 18.")
        XCTAssertNil(profileRepo.lastUpdate)
    }

    func testSavePersistsEditableFields() async {
        let profileRepo = FakeProfileRepo()
        let model = ProfileViewModel(
            userID: UserID("u_1"),
            profileRepo: profileRepo,
            cityRepo: FakeCityRepository(),
            authService: FakeAuthService()
        )
        model.displayName = " Taylor "
        model.childName = " Leo "
        model.childAgeText = "5"
        model.selectedCityID = CityID("city_aus")

        let saved = await model.save()

        XCTAssertEqual(saved?.displayName, "Taylor")
        XCTAssertEqual(profileRepo.lastUpdate, UserProfileUpdate(
            displayName: "Taylor",
            cityPreferenceID: CityID("city_aus"),
            childName: "Leo",
            childAge: 5
        ))
    }

    func testChangePasswordValidatesInputs() async {
        let auth = FakeAuthService()
        let profileRepo = FakeProfileRepo()
        profileRepo.profileResult = .success(UserProfile(
            id: UserID("u_1"),
            email: "user@example.com",
            displayName: nil,
            avatarURL: nil,
            cityPreferenceID: nil,
            childName: nil,
            childAge: nil
        ))
        let model = ProfileViewModel(
            userID: UserID("u_1"),
            profileRepo: profileRepo,
            cityRepo: FakeCityRepository(),
            authService: auth
        )
        await model.load()

        let changed = await model.changePassword(
            currentPassword: "old-password",
            newPassword: "new-password",
            confirmPassword: "new-password"
        )

        XCTAssertTrue(changed)
        XCTAssertEqual(auth.changePasswordCallCount, 1)
        XCTAssertEqual(auth.lastChangePasswordInput?.email, "user@example.com")
        XCTAssertEqual(auth.lastChangePasswordInput?.currentPassword, "old-password")
        XCTAssertEqual(auth.lastChangePasswordInput?.newPassword, "new-password")
    }
}
