import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class NotificationPreferencesRepoTests: XCTestCase {
    private let testUserID = UserID("test-user-id")

    // MARK: - NotificationPreferences DTO

    func testDefaultsMatchDBDefaults() {
        let defaults = NotificationPreferences.defaults
        XCTAssertTrue(defaults.reminderEmail)
        XCTAssertTrue(defaults.reminderPush)
        XCTAssertTrue(defaults.changeEmail)
        XCTAssertTrue(defaults.changePush)
        XCTAssertTrue(defaults.digestEmail)
        XCTAssertFalse(defaults.digestPush) // digest_push DB default is false
    }

    func testEquatable() {
        let a = NotificationPreferences.defaults
        var b = NotificationPreferences.defaults
        XCTAssertEqual(a, b)
        b.digestEmail = false
        XCTAssertNotEqual(a, b)
    }

    func testAllFieldsInitializedCorrectly() {
        let prefs = NotificationPreferences(
            reminderEmail: false,
            reminderPush: true,
            changeEmail: false,
            changePush: true,
            digestEmail: false,
            digestPush: true
        )
        XCTAssertFalse(prefs.reminderEmail)
        XCTAssertTrue(prefs.reminderPush)
        XCTAssertFalse(prefs.changeEmail)
        XCTAssertTrue(prefs.changePush)
        XCTAssertFalse(prefs.digestEmail)
        XCTAssertTrue(prefs.digestPush)
    }

    // MARK: - FakeNotificationPreferencesRepo - fetch

    func testFetchReturnsDefaultsInitially() async throws {
        let repo = FakeNotificationPreferencesRepo()
        let prefs = try await repo.fetch(userID: testUserID)
        XCTAssertEqual(prefs, .defaults)
        XCTAssertEqual(repo.lastFetchedUserID, testUserID)
        XCTAssertEqual(repo.fetchCallCount, 1)
    }

    func testFetchReturnsConfiguredResult() async throws {
        let repo = FakeNotificationPreferencesRepo()
        let custom = NotificationPreferences(
            reminderEmail: false, reminderPush: false,
            changeEmail: true, changePush: true,
            digestEmail: false, digestPush: false
        )
        repo.fetchResult = .success(custom)
        let prefs = try await repo.fetch(userID: testUserID)
        XCTAssertEqual(prefs, custom)
    }

    func testFetchPropagatesError() async {
        let repo = FakeNotificationPreferencesRepo()
        repo.fetchResult = .failure(NSError(domain: "test", code: 1))
        do {
            _ = try await repo.fetch(userID: testUserID)
            XCTFail("Expected throw")
        } catch {
            XCTAssertNotNil(error)
        }
    }

    func testFetchIncrementCallCount() async throws {
        let repo = FakeNotificationPreferencesRepo()
        _ = try await repo.fetch(userID: testUserID)
        _ = try await repo.fetch(userID: testUserID)
        XCTAssertEqual(repo.fetchCallCount, 2)
    }

    // MARK: - FakeNotificationPreferencesRepo - upsert

    func testUpsertTracksUserIDAndPreferences() async throws {
        let repo = FakeNotificationPreferencesRepo()
        var custom = NotificationPreferences.defaults
        custom.digestPush = true
        _ = try await repo.upsert(custom, for: testUserID)
        XCTAssertEqual(repo.lastUpsertedUserID, testUserID)
        XCTAssertEqual(repo.lastUpsertedPreferences, custom)
        XCTAssertEqual(repo.upsertCallCount, 1)
    }

    func testUpsertReturnsUpsertedPreferences() async throws {
        let repo = FakeNotificationPreferencesRepo()
        var custom = NotificationPreferences.defaults
        custom.digestPush = true
        let result = try await repo.upsert(custom, for: testUserID)
        XCTAssertEqual(result, custom)
    }

    func testUpsertUpdatesSubsequentFetch() async throws {
        let repo = FakeNotificationPreferencesRepo()
        var custom = NotificationPreferences.defaults
        custom.reminderPush = false
        _ = try await repo.upsert(custom, for: testUserID)
        let fetched = try await repo.fetch(userID: testUserID)
        XCTAssertEqual(fetched, custom)
    }

    func testUpsertReturnsConfiguredResult() async throws {
        let repo = FakeNotificationPreferencesRepo()
        let serverResponse = NotificationPreferences(
            reminderEmail: false, reminderPush: false,
            changeEmail: false, changePush: false,
            digestEmail: false, digestPush: false
        )
        repo.upsertResult = .success(serverResponse)
        let result = try await repo.upsert(.defaults, for: testUserID)
        XCTAssertEqual(result, serverResponse)
    }

    func testUpsertPropagatesError() async {
        let repo = FakeNotificationPreferencesRepo()
        repo.upsertResult = .failure(NSError(domain: "test", code: 2))
        do {
            _ = try await repo.upsert(.defaults, for: testUserID)
            XCTFail("Expected throw")
        } catch {
            XCTAssertNotNil(error)
        }
    }
}
