import XCTest
import SwiftData
import FECore
@testable import FEData
import FEDataTesting

@MainActor
final class PlanComposerTests: XCTestCase {
    private func makeContainer() throws -> ModelContainer {
        try AppModelContainer.makeInMemory()
    }

    func testHappyPathHydratesAndCaches() async throws {
        let container = try makeContainer()
        let location = FakeLocationService()
        location.authorizationStub = .authorized
        location.locationStub = GeoCoordinate(latitude: 30.27, longitude: -97.74)
        let weather = FakeWeatherProviding()
        weather.snapshotStub = WeatherSnapshot(temperatureCelsius: 24, precipitationChance: 0.1, conditionCode: "clear")
        let plan = FakePlanRepository()
        plan.fetchPlanResult = .success([
            PlanEventsRowDTO(eventID: EventID("evt_a"), score: 0.92, distanceScore: 1, weatherScore: 1, ageScore: 1, historyAffinity: 0, distanceKm: 1.0, dayOffset: 0),
            PlanEventsRowDTO(eventID: EventID("evt_b"), score: 0.81, distanceScore: 1, weatherScore: 1, ageScore: 1, historyAffinity: 0, distanceKm: 2.5, dayOffset: 0),
        ])
        let events = FakeEventRepository()
        // Deliberately return in REVERSE order to prove D14a re-sorts to rank order.
        events.fetchByIDsResult = .success([
            EventDTO.fixture(id: "evt_b", title: "Thumb"),
            EventDTO.fixture(id: "evt_a", title: "Hero"),
        ])
        let composer = PlanComposer(
            location: location, weather: weather,
            planRepo: plan, eventRepo: events,
            modelContainer: container
        )
        let result = try await composer.refresh(userID: UserID("u_1"), cityID: nil, kidAge: 5, today: "2026-05-15")
        XCTAssertEqual(result.events.map(\.title), ["Hero", "Thumb"], "D14a: events must be re-sorted to rank order")
        XCTAssertEqual(result.weatherSnapshot?.temperatureCelsius, 24)
        let ctx = container.mainContext
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 2)
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 2)
        // Verify rank assignment.
        let cached = try ctx.fetch(FetchDescriptor<CachedPlannedEvent>(sortBy: [SortDescriptor(\.rank)]))
        XCTAssertEqual(cached.first?.eventID, "evt_a")
    }

    func testFallsBackToCityWhenLocationDenied() async throws {
        let container = try makeContainer()
        let location = FakeLocationService()
        location.authorizationStub = .denied
        let weather = FakeWeatherProviding()
        let plan = FakePlanRepository()
        plan.fetchPlanResult = .success([])
        let events = FakeEventRepository()
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)
        _ = try await composer.refresh(userID: UserID("u_1"), cityID: CityID("city_aus"), kidAge: nil, today: "2026-05-15")
        XCTAssertEqual(location.requestAuthorizationCallCount, 1)
        // D9: PlanInput went out with the city, no coordinate.
        XCTAssertEqual(plan.lastInput?.cityID, CityID("city_aus"))
        XCTAssertNil(plan.lastInput?.coordinate)
        XCTAssertEqual(plan.lastInput?.weatherFit, "any", "no coordinate -> no weather fetch -> default any")
    }

    func testEmptyRankingsPreservesCache() async throws {
        // D4: prior cache exists, mid-week refresh returns empty -> cache untouched.
        let container = try makeContainer()
        let ctx = container.mainContext
        // Seed prior cache.
        let priorEvent = CachedEvent(id: "evt_prior", title: "Yesterday", startDatetime: Date(), lastSyncedAt: Date())
        let priorPlan = CachedPlannedEvent(eventID: "evt_prior", dayOffset: 0, score: 0.5, distanceKm: 1.0, lastSyncedAt: Date(), rank: 0)
        ctx.insert(priorEvent); ctx.insert(priorPlan)
        try ctx.save()

        let location = FakeLocationService(); location.authorizationStub = .denied
        let weather = FakeWeatherProviding()
        let plan = FakePlanRepository(); plan.fetchPlanResult = .success([])
        let events = FakeEventRepository()
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)
        _ = try await composer.refresh(userID: UserID("u_1"), cityID: nil, kidAge: nil, today: "2026-05-15")

        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 1, "prior cache preserved (D4)")
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 1)
    }

    // D9 error-path tests:

    func testWeatherErrorIsSwallowed() async throws {
        let container = try makeContainer()
        let location = FakeLocationService()
        location.authorizationStub = .authorized
        location.locationStub = GeoCoordinate(latitude: 30, longitude: -97)
        let weather = FakeWeatherProviding()
        struct Boom: Error {}
        weather.errorStub = Boom()
        let plan = FakePlanRepository(); plan.fetchPlanResult = .success([])
        let events = FakeEventRepository()
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)
        _ = try await composer.refresh(userID: UserID("u"), cityID: nil, kidAge: nil, today: "2026-05-15")
        XCTAssertEqual(plan.lastInput?.weatherFit, "any", "weather throw -> default any (composer continues)")
    }

    func testPlanRepoErrorPropagatesAndCacheUntouched() async throws {
        let container = try makeContainer()
        let location = FakeLocationService(); location.authorizationStub = .denied
        let weather = FakeWeatherProviding()
        let plan = FakePlanRepository()
        struct PlanBoom: Error {}
        plan.fetchPlanResult = .failure(PlanBoom())
        let events = FakeEventRepository()
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)
        do {
            _ = try await composer.refresh(userID: UserID("u"), cityID: nil, kidAge: nil, today: "2026-05-15")
            XCTFail("expected throw")
        } catch is PlanBoom {
            // ok
        } catch {
            XCTFail("wrong error: \(error)")
        }
        let ctx = container.mainContext
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 0)
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 0)
    }

    func testEventRepoErrorPropagatesAndCacheUntouched() async throws {
        let container = try makeContainer()
        let location = FakeLocationService(); location.authorizationStub = .denied
        let weather = FakeWeatherProviding()
        let plan = FakePlanRepository()
        plan.fetchPlanResult = .success([
            PlanEventsRowDTO(eventID: EventID("evt_a"), score: 0.9, distanceScore: 1, weatherScore: 1, ageScore: 1, historyAffinity: 0, distanceKm: nil, dayOffset: 0),
        ])
        let events = FakeEventRepository()
        struct EventBoom: Error {}
        events.fetchByIDsResult = .failure(EventBoom())
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)
        do {
            _ = try await composer.refresh(userID: UserID("u"), cityID: nil, kidAge: nil, today: "2026-05-15")
            XCTFail("expected throw")
        } catch is EventBoom {
            // ok
        } catch {
            XCTFail("wrong error: \(error)")
        }
        let ctx = container.mainContext
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 0)
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 0, "no half-written cache")
    }

    func testCancellationMidRefreshLeavesNoHalfWrittenCache() async throws {
        let container = try makeContainer()
        let location = FakeLocationService(); location.authorizationStub = .denied
        let weather = FakeWeatherProviding()
        let plan = FakePlanRepository()
        plan.artificialDelay = .seconds(2)
        plan.fetchPlanResult = .success([
            PlanEventsRowDTO(eventID: EventID("evt_a"), score: 0.9, distanceScore: 1, weatherScore: 1, ageScore: 1, historyAffinity: 0, distanceKm: nil, dayOffset: 0),
        ])
        let events = FakeEventRepository()
        events.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_a", title: "Hero")])
        let composer = PlanComposer(location: location, weather: weather, planRepo: plan, eventRepo: events, modelContainer: container)

        let task = Task { @MainActor in
            do {
                _ = try await composer.refresh(userID: UserID("u"), cityID: nil, kidAge: nil, today: "2026-05-15")
            } catch {
                // expected (cancellation)
            }
        }
        // Yield then cancel.
        try await Task.sleep(for: .milliseconds(100))
        task.cancel()
        _ = await task.value
        let ctx = container.mainContext
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedEvent>()).count, 0)
        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedPlannedEvent>()).count, 0)
    }
}
