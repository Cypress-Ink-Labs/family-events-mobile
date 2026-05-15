import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct PlanTab: View {
    public let tabTitle = "Plan"
    @State private var viewModel: PlanViewModel
    private let context: PlanContext
    private let cityName: String?
    private let onSelectEvent: (EventID) -> Void

    /// D14c — init creates the ViewModel exactly once via `_viewModel = State(initialValue:)`.
    /// Subsequent body recomputes do not re-create the observable.
    public init(
        composer: PlanComposer,
        context: PlanContext,
        cityName: String? = nil,
        onSelectEvent: @escaping (EventID) -> Void = { _ in }
    ) {
        _viewModel = State(initialValue: PlanViewModel(composer: composer))
        self.context = context
        self.cityName = cityName
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        NavigationStack {
            SaturdayPlanScreen(
                viewModel: viewModel,
                context: context,
                cityName: cityName,
                onSelectEvent: onSelectEvent
            )
        }
    }
}
