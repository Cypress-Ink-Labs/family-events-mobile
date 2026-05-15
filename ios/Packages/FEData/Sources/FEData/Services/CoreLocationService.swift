import Foundation
import FECore
#if canImport(CoreLocation)
import CoreLocation
#endif

#if canImport(CoreLocation)
public final class CoreLocationService: LocationService, Sendable {
    private let auth: any LocationAuthorizationProvider
    private let updates: any CLLocationUpdatesProvider
    private let timeoutSeconds: TimeInterval

    public init(
        auth: any LocationAuthorizationProvider = CLLocationManagerAuthorizationProvider(),
        updates: any CLLocationUpdatesProvider = RealCLLocationUpdatesProvider(),
        timeoutSeconds: TimeInterval = 10
    ) {
        self.auth = auth
        self.updates = updates
        self.timeoutSeconds = timeoutSeconds
    }

    public func currentAuthorization() async -> LocationAuthorizationStatus {
        await auth.current()
    }

    public func requestAuthorization() async -> LocationAuthorizationStatus {
        await auth.requestWhenInUse()
    }

    public func currentLocation() async -> GeoCoordinate? {
        let status = await auth.current()
        guard status == .authorized else { return nil }
        return await withTaskGroup(of: GeoCoordinate?.self) { group in
            let stream: AsyncStream<CLLocationUpdate> = updates.liveUpdates()
            let timeout = timeoutSeconds
            group.addTask {
                for await update in stream {
                    if let loc = update.location {
                        return GeoCoordinate(latitude: loc.coordinate.latitude, longitude: loc.coordinate.longitude)
                    }
                }
                return nil
            }
            group.addTask {
                try? await Task.sleep(for: .seconds(timeout))
                return nil
            }
            let first = await group.next() ?? nil
            group.cancelAll()
            return first
        }
    }
}
#endif
