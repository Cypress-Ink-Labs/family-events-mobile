import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class PlanViewModel: Refreshable {
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var lastEmptyRefresh = false
    public private(set) var lastWeatherSnapshot: WeatherSnapshot?
    public private(set) var lastFetchedAt: Date?

    private let composer: PlanComposer
    private var lastContext: PlanContext?

    public init(composer: PlanComposer) {
        self.composer = composer
    }

    public func refresh() async {
        guard let context = lastContext else { return }
        await refresh(context: context)
    }

    public func refresh(context: PlanContext) async {
        lastContext = context
        if CacheTTL.isFresh(lastFetchedAt: lastFetchedAt, ttl: CacheTTL.plan) && !lastEmptyRefresh {
            return
        }
        await forceRefresh(context: context)
    }

    public func forceRefresh(context: PlanContext) async {
        lastContext = context
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        let today = DateFormatting.todayDateKey()
        do {
            let result = try await composer.refresh(
                userID: context.userID,
                cityID: context.cityID,
                kidAge: context.kidAge,
                today: today
            )
            lastEmptyRefresh = result.rankings.isEmpty
            lastWeatherSnapshot = result.weatherSnapshot
            lastFetchedAt = Date()
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch is CancellationError {
            // swallow cancellations
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
