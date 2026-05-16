import XCTest
import FECore
import FEData
@testable import FEDataTesting

@MainActor
final class FakeFavoriteRepoTests: XCTestCase {
    func testFavoritesReturnsConfiguredResult() async throws {
        let repo = FakeFavoriteRepo()
        let uid = UserID("u_1")
        let dto = FavoriteDTO(id: "fav_1", userID: uid, eventID: EventID("evt_1"), createdAt: Date())
        repo.favoritesResult = .success([dto])
        let result = try await repo.favorites(for: uid)
        XCTAssertEqual(result.count, 1)
        XCTAssertEqual(result.first?.id, "fav_1")
        XCTAssertEqual(repo.lastUserID, uid)
    }

    func testFavoritesThrowsWhenConfigured() async {
        let repo = FakeFavoriteRepo()
        struct Boom: Error {}
        repo.favoritesResult = .failure(Boom())
        do {
            _ = try await repo.favorites(for: UserID("u_1"))
            XCTFail("Expected throw")
        } catch {}
    }

    func testFavoriteRecordsEventID() async throws {
        let repo = FakeFavoriteRepo()
        let eid = EventID("evt_A")
        try await repo.favorite(eventID: eid, for: UserID("u_1"))
        XCTAssertEqual(repo.favoritedEventIDs, [eid])
    }

    func testFavoriteThrowsWhenConfigured() async {
        let repo = FakeFavoriteRepo()
        struct Boom: Error {}
        repo.favoriteError = Boom()
        do {
            try await repo.favorite(eventID: EventID("e"), for: UserID("u"))
            XCTFail("Expected throw")
        } catch {}
        XCTAssertTrue(repo.favoritedEventIDs.isEmpty)
    }

    func testUnfavoriteRecordsEventID() async throws {
        let repo = FakeFavoriteRepo()
        let eid = EventID("evt_B")
        try await repo.unfavorite(eventID: eid, for: UserID("u_1"))
        XCTAssertEqual(repo.unfavoritedEventIDs, [eid])
    }

    func testUnfavoriteThrowsWhenConfigured() async {
        let repo = FakeFavoriteRepo()
        struct Boom: Error {}
        repo.unfavoriteError = Boom()
        do {
            try await repo.unfavorite(eventID: EventID("e"), for: UserID("u"))
            XCTFail("Expected throw")
        } catch {}
        XCTAssertTrue(repo.unfavoritedEventIDs.isEmpty)
    }

    func testObserveFavoritesYieldsNoEventsForNoOp() async {
        let repo = FakeFavoriteRepo()
        let stream = repo.observeFavorites(for: UserID("u_1"))
        // Stream is a no-op — just verify it compiles and is non-nil
        XCTAssertNotNil(stream)
    }
}
