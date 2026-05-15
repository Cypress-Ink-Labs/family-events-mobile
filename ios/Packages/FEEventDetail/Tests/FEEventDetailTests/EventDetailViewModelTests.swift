import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEEventDetail

@MainActor
final class EventDetailViewModelTests: XCTestCase {
    func testLoadPopulatesEvent() async {
        let repo = FakeEventRepository()
        repo.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_1", title: "Storytime")])
        let vm = EventDetailViewModel(eventRepo: repo, userID: UserID("u"), eventID: EventID("evt_1"))
        await vm.load()
        XCTAssertEqual(vm.event?.title, "Storytime")
        XCTAssertNil(vm.errorMessage)
        XCTAssertFalse(vm.isLoading)
    }

    func testLoadSurfacesErrorWhenRepoFails() async {
        let repo = FakeEventRepository()
        struct Boom: Error {}
        repo.fetchByIDsResult = .failure(Boom())
        let vm = EventDetailViewModel(eventRepo: repo, userID: UserID("u"), eventID: EventID("evt_1"))
        await vm.load()
        XCTAssertNotNil(vm.errorMessage)
        XCTAssertNil(vm.event)
    }

    func testLoadReturnsNotFoundWhenRepoYieldsEmpty() async {
        let repo = FakeEventRepository()
        repo.fetchByIDsResult = .success([])
        let vm = EventDetailViewModel(eventRepo: repo, userID: UserID("u"), eventID: EventID("missing"))
        await vm.load()
        XCTAssertEqual(vm.errorMessage, "Event not found.")
    }

    func testToggleFavoriteFlipsFlag() {
        let repo = FakeEventRepository()
        let vm = EventDetailViewModel(eventRepo: repo, userID: UserID("u"), eventID: EventID("e"))
        XCTAssertFalse(vm.isFavorited)
        vm.toggleFavorite()
        XCTAssertTrue(vm.isFavorited)
        vm.toggleFavorite()
        XCTAssertFalse(vm.isFavorited)
    }
}
