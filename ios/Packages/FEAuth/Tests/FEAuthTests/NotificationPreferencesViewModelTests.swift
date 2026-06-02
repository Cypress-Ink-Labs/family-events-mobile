import XCTest
import FECore
import FEData
@testable import FEAuth
import FEDataTesting

@MainActor
final class NotificationPreferencesViewModelTests: XCTestCase {
    private let userID = UserID("test-user")
    private var repo: FakeNotificationPreferencesRepo!
    private var sut: NotificationPreferencesViewModel!

    override func setUp() {
        super.setUp()
        repo = FakeNotificationPreferencesRepo()
        sut = NotificationPreferencesViewModel(userID: userID, repo: repo)
    }

    // MARK: load()

    func test_load_appliesServerPreferences() async {
        let expected = NotificationPreferences(
            reminderEmail: false,
            reminderPush: true,
            changeEmail: false,
            changePush: false,
            digestEmail: true,
            digestPush: true
        )
        repo.fetchResult = .success(expected)

        await sut.load()

        XCTAssertEqual(sut.preferences, expected)
        XCTAssertNil(sut.errorMessage)
    }

    func test_load_fallsBackToDefaults_onError() async {
        repo.fetchResult = .failure(URLError(.badServerResponse))

        await sut.load()

        XCTAssertEqual(sut.preferences, .defaults)
    }

    // MARK: set(_:to:)

    func test_set_optimisticallyUpdatesAndUpserts() async {
        await sut.load()
        let originalValue = sut.preferences.reminderEmail

        await sut.set(\.reminderEmail, to: !originalValue)

        XCTAssertEqual(sut.preferences.reminderEmail, !originalValue)
        XCTAssertEqual(repo.upsertCallCount, 1)
        XCTAssertEqual(repo.lastUpsertedUserID, userID)
        XCTAssertNil(sut.errorMessage)
    }

    func test_set_rollsBackAndSetsError_onUpsertFailure() async {
        let original = sut.preferences
        repo.upsertResult = .failure(URLError(.badServerResponse))

        await sut.set(\.reminderEmail, to: !original.reminderEmail)

        XCTAssertEqual(sut.preferences.reminderEmail, original.reminderEmail)
        XCTAssertNotNil(sut.errorMessage)
    }

    func test_set_userIDPassedToUpsert() async {
        await sut.set(\.digestPush, to: true)
        XCTAssertEqual(repo.lastUpsertedUserID, userID)
    }
}
