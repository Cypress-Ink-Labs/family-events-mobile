import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class PlanViewModel {
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var lastEmptyRefresh = false

    private let composer: PlanComposer

    public init(composer: PlanComposer) {
        self.composer = composer
    }

    public func refresh(context: PlanContext) async {
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
            // swallow cancellations
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
