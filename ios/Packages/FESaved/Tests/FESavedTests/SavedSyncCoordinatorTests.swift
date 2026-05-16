import XCTest
import SwiftData
import FECore
import FEData
import FEDataTesting
@testable import FESaved

@MainActor
final class SavedSyncCoordinatorTests: XCTestCase {
    private func makeCoordinator(
        favoriteRepo: FakeFavoriteRepo,
        eventRepo: FakeEventRepository = FakeEventRepository()
    ) throws -> (SavedSyncCoordinator, ModelContainer) {
        let container = try AppModelContainer.makeInMemory()
        let coord = SavedSyncCoordinator(
            favoriteRepo: favoriteRepo,
            eventRepo: eventRepo,
            modelContainer: container
        )
        return (coord, container)
    }

    func testRefreshReplacesLocalFavorites() async throws {
        let repo = FakeFavoriteRepo()
        let uid = UserID("u_1")
        let dto = FavoriteDTO(id: "fav_1", userID: uid, eventID: EventID("evt_1"), createdAt: Date())
        repo.favoritesResult = .success([dto])
        let (coord, container) = try makeCoordinator(favoriteRepo: repo)

        await coord.refresh(userID: uid)

        let cached = try container.mainContext.fetch(FetchDescriptor<CachedFavorite>())
        XCTAssertEqual(cached.count, 1)
        XCTAssertEqual(cached.first?.eventID, "evt_1")
    }

    func testRefreshRemovesStaleLocalRows() async throws {
        let repo = FakeFavoriteRepo()
        let uid = UserID("u_1")
        let (coord, container) = try makeCoordinator(favoriteRepo: repo)

        // Seed a stale local favorite that's not on the server.
        let ctx = container.mainContext
        ctx.insert(CachedFavorite(userID: uid.rawValue, eventID: "stale", createdAt: Date(), lastSyncedAt: Date()))
        try ctx.save()

        repo.favoritesResult = .success([])  // server says no favorites
        await coord.refresh(userID: uid)

        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedFavorite>()).count, 0)
    }

    func testRefreshWarmsEventCache() async throws {
        let favRepo = FakeFavoriteRepo()
        let eventRepo = FakeEventRepository()
        let uid = UserID("u_1")
        let dto = FavoriteDTO(id: "fav_1", userID: uid, eventID: EventID("evt_1"), createdAt: Date())
        favRepo.favoritesResult = .success([dto])
        eventRepo.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_1", title: "Storytime")])
        let (coord, container) = try makeCoordinator(favoriteRepo: favRepo, eventRepo: eventRepo)

        await coord.refresh(userID: uid)

        let cachedEvents = try container.mainContext.fetch(FetchDescriptor<CachedEvent>())
        XCTAssertEqual(cachedEvents.count, 1)
        XCTAssertEqual(cachedEvents.first?.title, "Storytime")
    }

    func testRefreshSurfacesErrorOnFavoritesFailure() async throws {
        let repo = FakeFavoriteRepo()
        struct Boom: Error {}
        repo.favoritesResult = .failure(Boom())
        let (coord, _) = try makeCoordinator(favoriteRepo: repo)
        await coord.refresh(userID: UserID("u_1"))
        XCTAssertNotNil(coord.errorMessage)
    }
}
