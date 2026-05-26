import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEExplore

@MainActor
final class ExploreViewModelTests: XCTestCase {
    private let userID = UserID("u1")
    private let cityID = CityID("city1")

    // MARK: - reload

    func testReloadSetsEventsFromRepo() async throws {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([
            .fixture(id: "e1", title: "Storytime"),
            .fixture(id: "e2", title: "Art Class"),
        ])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: cityID)
        await vm.reload()
        XCTAssertEqual(vm.events.count, 2)
        XCTAssertFalse(vm.isLoading)
        XCTAssertNil(vm.errorMessage)
    }

    func testReloadResetsState() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([.fixture(id: "e1", title: "X")])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
        fake.fetchListResult = .success([])
        await vm.reload()
        XCTAssertEqual(vm.events.count, 0)
    }

    // MARK: - loadNextPage

    func testLoadNextPageAppendsEvents() async {
        let fake = FakeEventRepository()
        let page1 = (0..<20).map { EventDTO.fixture(id: "e\($0)", title: "E\($0)") }
        let page2 = (20..<25).map { EventDTO.fixture(id: "e\($0)", title: "E\($0)") }
        fake.fetchListResult = .success(page1)
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.loadNextPage()
        XCTAssertEqual(vm.events.count, 20)
        XCTAssertTrue(vm.hasMore)

        fake.fetchListResult = .success(page2)
        await vm.loadNextPage()
        XCTAssertEqual(vm.events.count, 25)
        XCTAssertFalse(vm.hasMore) // 5 < 20 → end
    }

    func testLoadNextPageStopsWhenHasMoreFalse() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([]) // 0 < 20 → hasMore = false
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.loadNextPage()
        XCTAssertFalse(vm.hasMore)

        let countBefore = fake.fetchListCallCount
        await vm.loadNextPage() // should no-op
        XCTAssertEqual(fake.fetchListCallCount, countBefore)
    }

    func testLoadNextPagePassesCorrectOffset() async {
        let fake = FakeEventRepository()
        let page = (0..<20).map { EventDTO.fixture(id: "e\($0)", title: "E\($0)") }
        fake.fetchListResult = .success(page)
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: cityID)
        await vm.loadNextPage()
        XCTAssertEqual(fake.lastListQuery?.offset, 0)
        fake.fetchListResult = .success([.fixture(id: "e99", title: "Last")])
        await vm.loadNextPage()
        XCTAssertEqual(fake.lastListQuery?.offset, 20)
    }

    // MARK: - filter change triggers reload

    func testFilterChangeTriggersReload() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.reload()
        let countBefore = fake.fetchListCallCount
        vm.filters.keyword = "park"
        // Give the didSet Task a chance to run
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertGreaterThan(fake.fetchListCallCount, countBefore)
    }

    // MARK: - client filters

    func testOnlyFreeFilter() async {
        let fake = FakeEventRepository()
        let freeEvent = EventDTO.fixture(id: "e1", title: "Free")
        let paidEvent = EventDTO.fixture(id: "e2", title: "Paid", isFree: false, price: 10.0)
        fake.fetchListResult = .success([freeEvent, paidEvent])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        vm.filters.onlyFree = true
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
        XCTAssertEqual(vm.events.first?.title, "Free")
    }

    func testKeywordFilter() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([
            .fixture(id: "e1", title: "Storytime at the Library"),
            .fixture(id: "e2", title: "Art Workshop"),
        ])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        vm.filters.keyword = "storytime"
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
        XCTAssertEqual(vm.events.first?.id.rawValue, "e1")
    }

    // MARK: - error path

    func testErrorMessageSetOnAppError() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .failure(AppError.notFound)
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.reload()
        XCTAssertNotNil(vm.errorMessage)
        XCTAssertTrue(vm.events.isEmpty)
    }

    func testErrorMessageSetOnUnknownError() async {
        let fake = FakeEventRepository()
        struct SomeError: Error {}
        fake.fetchListResult = .failure(SomeError())
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.reload()
        XCTAssertNotNil(vm.errorMessage)
    }

    func testCancelledReloadDoesNotSurfaceError() async {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([.fixture(id: "e1", title: "Storytime")])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)

        fake.fetchListResult = .failure(URLError(.cancelled))
        await vm.reload()

        XCTAssertNil(vm.errorMessage)
        XCTAssertEqual(vm.events.count, 1)
        XCTAssertFalse(vm.isLoading)
    }

    func testTaskCancelledReloadDoesNotSurfaceError() async {
        let fake = FakeEventRepository()
        fake.artificialDelay = .seconds(1)
        fake.fetchListResult = .success([.fixture(id: "e1", title: "Storytime")])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)

        let task = Task { await vm.reload() }
        try? await Task.sleep(nanoseconds: 50_000_000)
        task.cancel()
        await task.value

        XCTAssertNil(vm.errorMessage)
        XCTAssertTrue(vm.events.isEmpty)
        XCTAssertFalse(vm.isLoading)
    }

    // MARK: - age filter

    func testAgeFilterNarrowsList() async {
        let fake = FakeEventRepository()
        let infant = EventDTO.fixture(id: "e1", title: "Baby Swim", ageMin: 0, ageMax: 1)
        let older = EventDTO.fixture(id: "e2", title: "Soccer Camp", ageMin: 5, ageMax: 10)
        fake.fetchListResult = .success([infant, older])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        vm.filters.ageFilter = .zeroToOne
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
        XCTAssertEqual(vm.events.first?.id.rawValue, "e1")
    }

    func testAgeFilterOpenEndedMax() async {
        // nineAndUp has max == nil → fMax == Int.max; event with ageMax nil treated as 99 passes
        let fake = FakeEventRepository()
        let event = EventDTO.fixture(id: "e1", title: "Teen Workshop", ageMin: 12, ageMax: nil)
        fake.fetchListResult = .success([event])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        vm.filters.ageFilter = .nineAndUp
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
    }

    func testCategoryFilterNarrowsList() async {
        let fake = FakeEventRepository()
        let playgroupTag = TagDTO(id: "t1", name: "Playgroup", slug: "playgroup", color: "#abc")
        let musicTag = TagDTO(id: "t2", name: "Music", slug: "music", color: "#def")
        let playgroupEvent = EventDTO.fixture(id: "e1", title: "Playgroup Fun", tags: [playgroupTag])
        let musicEvent = EventDTO.fixture(id: "e2", title: "Music Class", tags: [musicTag])
        fake.fetchListResult = .success([playgroupEvent, musicEvent])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)
        vm.filters.activeCategory = "playgroup"
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)
        XCTAssertEqual(vm.events.first?.id.rawValue, "e1")
    }

    func testClearingFiltersRestoresList() async {
        let fake = FakeEventRepository()
        let infant = EventDTO.fixture(id: "e1", title: "Baby Swim", ageMin: 0, ageMax: 1)
        let older = EventDTO.fixture(id: "e2", title: "Soccer Camp", ageMin: 5, ageMax: 10)
        fake.fetchListResult = .success([infant, older])
        let vm = ExploreViewModel(eventRepo: fake, userID: userID, cityID: nil)

        vm.filters.ageFilter = .zeroToOne
        await vm.reload()
        XCTAssertEqual(vm.events.count, 1)

        vm.filters.ageFilter = nil
        await vm.reload()
        XCTAssertEqual(vm.events.count, 2)
    }
}
