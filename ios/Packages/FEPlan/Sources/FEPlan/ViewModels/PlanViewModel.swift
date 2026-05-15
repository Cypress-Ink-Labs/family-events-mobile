import Foundation
import Observation
import FECore
import FEData

/// Refresh state only (D1). Data flows from SwiftData @Query in SaturdayPlanScreen.
/// PlanViewModel exposes:
///   - isLoading (true while composer.refresh runs)
///   - errorMessage (non-nil if last refresh failed)
///   - lastEmptyRefresh (true if last completed refresh returned 0 rankings — used for
///     the "no new plan today — showing yesterday's" banner)
@Observable
@MainActor
public final class PlanViewModel {
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var lastEmptyRefresh = false

    private let composer: PlanComposer
    private let context: PlanContext

    public init(composer: PlanComposer, context: PlanContext) {
        self.composer = composer
        self.context = context
    }

    public func refresh() async {
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
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch is CancellationError {
            // user-initiated cancellation — don't surface as an error
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
