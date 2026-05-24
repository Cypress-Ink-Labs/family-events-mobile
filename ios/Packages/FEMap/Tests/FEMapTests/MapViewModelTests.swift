import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEMap

@MainActor
final class MapViewModelTests: XCTestCase {
    private func event(id: String, lat: Double?, lng: Double?, isFree: Bool = false) -> EventDTO {
        EventDTO(
            id: EventID(id),
            title: "Event \(id)",
            description: nil,
            startDatetime: Date(timeIntervalSince1970: 1_700_000_000),
            endDatetime: nil,
            timezone: "UTC",
            venueName: "Venue",
            address: nil,
            cityID: CityID("c1"),
            latitude: lat,
            longitude: lng,
            ageMin: nil,
            ageMax: nil,
            price: isFree ? nil : 10,
            isFree: isFree,
            sourceURL: nil,
            sourceName: nil,
            sourceID: nil,
            images: [],
            status: "published",
            aiConfidence: nil,
            aiTagProvider: nil,
            isFeatured: false,
            viewCount: 0,
            createdAt: Date(timeIntervalSince1970: 0),
            updatedAt: Date(timeIntervalSince1970: 0),
            tags: [],
            avgRating: 0,
            ratingCount: 0,
            isFavorited: false
        )
    }

    func test_refresh_populatesEventsAndTimestamp() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([
            event(id: "e1", lat: 30.0, lng: -97.0),
            event(id: "e2", lat: nil, lng: nil),  // unmappable — filtered out
        ])
        let vm = MapViewModel(eventRepo: repo, userID: UserID("u1"), cityID: CityID("c1"))
        XCTAssertNil(vm.lastFetchedAt)
        await vm.refresh()
        XCTAssertEqual(vm.events.map(\.id.rawValue), ["e1"])
        XCTAssertNotNil(vm.lastFetchedAt)
    }

    func test_loadIfNeeded_skipsWhenFresh() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([event(id: "e1", lat: 30.0, lng: -97.0)])
        let vm = MapViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil)
        await vm.loadIfNeeded()
        let initialCallCount = repo.fetchListCallCount
        await vm.loadIfNeeded()
        XCTAssertEqual(repo.fetchListCallCount, initialCallCount, "should not re-fetch within TTL")
    }

    func test_refresh_bypassesCache() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([event(id: "e1", lat: 30.0, lng: -97.0)])
        let vm = MapViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil)
        await vm.loadIfNeeded()
        let initialCallCount = repo.fetchListCallCount
        await vm.refresh()
        XCTAssertEqual(repo.fetchListCallCount, initialCallCount + 1)
    }

    func test_eventsInRegion_filtersOutOfBounds() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([
            event(id: "in", lat: 30.0, lng: -97.0),
            event(id: "out", lat: 60.0, lng: -10.0),
        ])
        let vm = MapViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil)
        await vm.refresh()
        let region = MapRegion(centerLatitude: 30, centerLongitude: -97, latitudeDelta: 1, longitudeDelta: 1)
        XCTAssertEqual(vm.eventsInRegion(region).map(\.id.rawValue), ["in"])
    }

    func test_updateCity_triggersRefresh() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([])
        let vm = MapViewModel(eventRepo: repo, userID: UserID("u1"), cityID: CityID("a"))
        await vm.loadIfNeeded()
        let initialCallCount = repo.fetchListCallCount
        await vm.updateCity(CityID("b"))
        XCTAssertGreaterThan(repo.fetchListCallCount, initialCallCount)
    }
}
