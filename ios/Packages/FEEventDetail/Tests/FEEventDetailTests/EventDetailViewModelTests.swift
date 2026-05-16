import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEEventDetail

@MainActor
final class EventDetailViewModelTests: XCTestCase {
    private func makeVM(
        eventRepo: FakeEventRepository = FakeEventRepository(),
        favoriteRepo: FakeFavoriteRepo = FakeFavoriteRepo(),
        eventID: EventID = EventID("evt_1")
    ) -> EventDetailViewModel {
        EventDetailViewModel(
            eventRepo: eventRepo,
            favoriteRepo: favoriteRepo,
            userID: UserID("u"),
            eventID: eventID
        )
    }

    func testLoadPopulatesEvent() async {
        let repo = FakeEventRepository()
        repo.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_1", title: "Storytime")])
        let vm = makeVM(eventRepo: repo)
        await vm.load()
        XCTAssertEqual(vm.event?.title, "Storytime")
        XCTAssertNil(vm.errorMessage)
        XCTAssertFalse(vm.isLoading)
    }

    func testLoadSurfacesErrorWhenRepoFails() async {
        let repo = FakeEventRepository()
        struct Boom: Error {}
        repo.fetchByIDsResult = .failure(Boom())
        let vm = makeVM(eventRepo: repo)
        await vm.load()
        XCTAssertNotNil(vm.errorMessage)
        XCTAssertNil(vm.event)
    }

    func testLoadReturnsNotFoundWhenRepoYieldsEmpty() async {
        let repo = FakeEventRepository()
        repo.fetchByIDsResult = .success([])
        let vm = makeVM(eventRepo: repo, eventID: EventID("missing"))
        await vm.load()
        XCTAssertEqual(vm.errorMessage, "Event not found.")
    }

    func testToggleFavoriteFlipsFlag() {
        let vm = makeVM()
        XCTAssertFalse(vm.isFavorited)
        vm.toggleFavorite()
        XCTAssertTrue(vm.isFavorited)
    }

    func testToggleFavoritePersistsViaRepo() async throws {
        let fakeRepo = FakeFavoriteRepo()
        let vm = makeVM(favoriteRepo: fakeRepo, eventID: EventID("evt_1"))
        vm.toggleFavorite()
        // Drain pending tasks
        try await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(fakeRepo.favoritedEventIDs, [EventID("evt_1")])
        XCTAssertTrue(fakeRepo.unfavoritedEventIDs.isEmpty)
    }

    func testToggleFavoriteRollsBackOnError() async throws {
        let fakeRepo = FakeFavoriteRepo()
        struct Boom: Error {}
        fakeRepo.favoriteError = Boom()
        let vm = makeVM(favoriteRepo: fakeRepo)
        XCTAssertFalse(vm.isFavorited)
        vm.toggleFavorite()
        // Drain pending tasks
        try await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertFalse(vm.isFavorited)
        XCTAssertNotNil(vm.errorMessage)
    }

    func testToggleFavoriteRapidDoubleTapIsGuarded() async throws {
        let fakeRepo = FakeFavoriteRepo()
        let vm = makeVM(favoriteRepo: fakeRepo, eventID: EventID("evt_1"))
        vm.toggleFavorite()
        vm.toggleFavorite()  // second call should be ignored due to isFavoriteInFlight
        try await Task.sleep(nanoseconds: 50_000_000)
        // Only one repo call should have been made
        XCTAssertEqual(fakeRepo.favoritedEventIDs.count, 1)
        XCTAssertTrue(fakeRepo.unfavoritedEventIDs.isEmpty)
    }
}
