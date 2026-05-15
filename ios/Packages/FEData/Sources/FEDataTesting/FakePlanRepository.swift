import Foundation
import FECore
import FEData

public final class FakePlanRepository: PlanRepository, @unchecked Sendable {
    public var fetchPlanResult: Result<[PlanEventsRowDTO], Error> = .success([])
    public var artificialDelay: Duration?   // used by cancellation tests (D9 #4)
    private(set) public var lastInput: PlanInput?   // D9 capture
    private(set) public var callCount = 0
    public init() {}
    public func fetchPlan(input: PlanInput) async throws -> [PlanEventsRowDTO] {
        callCount += 1
        lastInput = input
        if let delay = artificialDelay {
            try await Task.sleep(for: delay)  // respects task cancellation
        }
        return try fetchPlanResult.get()
    }
}
