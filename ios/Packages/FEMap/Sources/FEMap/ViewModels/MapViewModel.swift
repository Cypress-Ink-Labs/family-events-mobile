import Foundation
import Observation
import FECore
import FEData

@MainActor
@Observable
public final class MapViewModel: Refreshable {
    public private(set) var events: [EventDTO] = []
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var lastFetchedAt: Date?

    private let eventRepo: any EventRepository
    private let userID: UserID
    private var cityID: CityID?

    public init(eventRepo: any EventRepository, userID: UserID, cityID: CityID?) {
        self.eventRepo = eventRepo
        self.userID = userID
        self.cityID = cityID
    }

    public func updateCity(_ cityID: CityID?) async {
        guard cityID != self.cityID else { return }
        self.cityID = cityID
        await refresh()
    }

    public func refresh() async {
        await load(bypassCache: true)
    }

    public func loadIfNeeded() async {
        if CacheTTL.isFresh(lastFetchedAt: lastFetchedAt, ttl: CacheTTL.default) && !events.isEmpty {
            return
        }
        await load(bypassCache: false)
    }

    private func load(bypassCache: Bool) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        let query = EventQuery(
            cityID: cityID,
            dateFrom: Date(),
            dateTo: nil,
            limit: 200,
            offset: 0
        )
        do {
            let fetched = try await eventRepo.fetchList(query: query, for: userID)
            events = fetched.filter { $0.latitude != nil && $0.longitude != nil }
            lastFetchedAt = Date()
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    /// Returns only events whose coordinates fall inside the given map region.
    /// Mirrors web's supercluster bbox-filter pattern (cluster lib filters on
    /// visible bounds rather than re-fetching server-side).
    public func eventsInRegion(_ region: MapRegion) -> [EventDTO] {
        events.filter { event in
            guard let lat = event.latitude, let lng = event.longitude else { return false }
            return region.contains(latitude: lat, longitude: lng)
        }
    }
}

/// Plain-Swift representation of a map's visible region. Decoupled from
/// MapKit so view models stay testable on macOS without Map renderers.
public struct MapRegion: Sendable, Equatable {
    public let centerLatitude: Double
    public let centerLongitude: Double
    public let latitudeDelta: Double
    public let longitudeDelta: Double

    public init(
        centerLatitude: Double,
        centerLongitude: Double,
        latitudeDelta: Double,
        longitudeDelta: Double
    ) {
        self.centerLatitude = centerLatitude
        self.centerLongitude = centerLongitude
        self.latitudeDelta = latitudeDelta
        self.longitudeDelta = longitudeDelta
    }

    public func contains(latitude: Double, longitude: Double) -> Bool {
        let minLat = centerLatitude - latitudeDelta / 2
        let maxLat = centerLatitude + latitudeDelta / 2
        let minLng = centerLongitude - longitudeDelta / 2
        let maxLng = centerLongitude + longitudeDelta / 2
        return latitude >= minLat && latitude <= maxLat
            && longitude >= minLng && longitude <= maxLng
    }
}
