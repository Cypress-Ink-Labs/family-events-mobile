import Foundation
import SwiftData
import FECore

public struct PlanResult: Sendable {
    public let date: String
    public let dayOffset: Int
    public let weatherFit: String
    public let events: [EventDTO]    // already in rank order (D14a)
    public let rankings: [PlanEventsRowDTO]
}

@MainActor
public final class PlanComposer {
    public let location: any LocationService
    public let weather: any WeatherProviding
    public let planRepo: any PlanRepository
    public let eventRepo: any EventRepository
    public let modelContainer: ModelContainer

    public init(
        location: any LocationService,
        weather: any WeatherProviding,
        planRepo: any PlanRepository,
        eventRepo: any EventRepository,
        modelContainer: ModelContainer
    ) {
        self.location = location
        self.weather = weather
        self.planRepo = planRepo
        self.eventRepo = eventRepo
        self.modelContainer = modelContainer
    }

    public func refresh(
        userID: UserID,
        cityID: CityID?,
        kidAge: Int?,
        today: String
    ) async throws -> PlanResult {
        var coordinate: GeoCoordinate? = nil
        switch await location.requestAuthorization() {
        case .authorized:
            coordinate = await location.currentLocation()
        case .notDetermined, .denied, .restricted:
            coordinate = nil
        }

        // Weather is best-effort. Failure -> default "any" weatherFit (compatible with the
        // RPC's enum and equivalent to the D9 "neutral" intent: no opinion).
        var weatherFit = "any"
        if let coord = coordinate {
            if let snapshot = try? await weather.currentWeather(at: coord) {
                weatherFit = snapshot.weatherFit
            }
        }

        let input = PlanInput(
            userID: userID, date: today,
            cityID: coordinate == nil ? cityID : nil,
            coordinate: coordinate,
            kidAge: kidAge, weatherFit: weatherFit,
            limit: 3, maxDays: 7
        )

        let rankings = try await planRepo.fetchPlan(input: input)

        // D14a: events_enriched orders by start_datetime, NOT by p_event_ids. Re-sort using rankings.
        let ids = rankings.map(\.eventID)
        let events: [EventDTO]
        if ids.isEmpty {
            events = []
        } else {
            let fetched = try await eventRepo.fetch(ids: ids, for: userID)
            let eventsByID = Dictionary(uniqueKeysWithValues: fetched.map { ($0.id, $0) })
            events = rankings.compactMap { eventsByID[$0.eventID] }
        }

        try upsert(events: events, rankings: rankings, today: today)

        let dayOffset = rankings.first?.dayOffset ?? 0
        return PlanResult(date: today, dayOffset: dayOffset, weatherFit: weatherFit, events: events, rankings: rankings)
    }

    private func upsert(events: [EventDTO], rankings: [PlanEventsRowDTO], today: String) throws {
        // D4: preserve prior cache when refresh returns empty rankings.
        if rankings.isEmpty { return }

        let ctx = modelContainer.mainContext
        let now = Date()
        // Replace the whole plan view (we always re-fetch top-N).
        try ctx.delete(model: CachedPlannedEvent.self)
        for event in events { CachedEvent.upsert(event, in: ctx, at: now) }
        for (index, row) in rankings.enumerated() {
            ctx.insert(CachedPlannedEvent(
                eventID: row.eventID.rawValue, dayOffset: row.dayOffset,
                score: row.score, distanceKm: row.distanceKm,
                lastSyncedAt: now, rank: index
            ))
        }
        try ctx.save()
    }
}
