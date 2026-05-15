import XCTest
import SwiftData
import FECore
import FEData
import FEDataTesting
@testable import FEPlan

@MainActor
final class PlanViewModelTests: XCTestCase {
    private func makeComposer(
        plan: FakePlanRepository,
        events: FakeEventRepository = FakeEventRepository(),
        location: FakeLocationService = FakeLocationService(),
        weather: FakeWeatherProviding = FakeWeatherProviding()
    ) throws -> (PlanComposer, ModelContainer) {
        let container = try AppModelContainer.makeInMemory()
        return (
            PlanComposer(
                location: location, weather: weather,
                planRepo: plan, eventRepo: events,
                modelContainer: container
            ),
            container
        )
    }

    func testRefreshSucceedsAndClearsErrorMessage() async throws {
        let plan = FakePlanRepository()
        let events = FakeEventRepository()
        let location = FakeLocationService()
        location.authorizationStub = .authorized
        location.locationStub = GeoCoordinate(latitude: 30, longitude: -97)
        plan.fetchPlanResult = .success([
            PlanEventsRowDTO(eventID: EventID("evt_a"), score: 1, distanceScore: 1, weatherScore: 1, ageScore: 1, historyAffinity: 0, distanceKm: 1, dayOffset: 0),
        ])
        events.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_a", title: "Hero")])
        let (composer, _) = try makeComposer(plan: plan, events: events, location: location)
        let vm = PlanViewModel(composer: composer)
        await vm.refresh(context: PlanContext(userID: UserID("u")))
        XCTAssertNil(vm.errorMessage)
        XCTAssertFalse(vm.isLoading)
        XCTAssertFalse(vm.lastEmptyRefresh)
    }

    func testRefreshSurfacesErrorWhenComposerThrows() async throws {
        let plan = FakePlanRepository()
        struct Boom: Error {}
        plan.fetchPlanResult = .failure(Boom())
        let (composer, _) = try makeComposer(plan: plan)
        let vm = PlanViewModel(composer: composer)
        await vm.refresh(context: PlanContext(userID: UserID("u")))
        XCTAssertNotNil(vm.errorMessage)
        XCTAssertFalse(vm.isLoading)
    }

    func testEmptyRefreshSetsLastEmptyRefresh() async throws {
        let plan = FakePlanRepository()
        plan.fetchPlanResult = .success([])
        let (composer, _) = try makeComposer(plan: plan)
        let vm = PlanViewModel(composer: composer)
        await vm.refresh(context: PlanContext(userID: UserID("u")))
        XCTAssertNil(vm.errorMessage)
        XCTAssertTrue(vm.lastEmptyRefresh)
    }
}
