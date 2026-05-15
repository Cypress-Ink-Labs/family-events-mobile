import SwiftUI
import FECore
import FEData
import FEDesignSystem
import FEEventDetail

public struct PlanTab: View {
    public let tabTitle = "Plan"
    @State private var viewModel: PlanViewModel
    @State private var path: [EventID] = []
    private let context: PlanContext
    private let cityName: String?
    private let onSetCity: () -> Void

    /// D14c — init creates the ViewModel exactly once via `_viewModel = State(initialValue:)`.
    /// Subsequent body recomputes do not re-create the observable.
    public init(
        composer: PlanComposer,
        context: PlanContext,
        cityName: String? = nil,
        onSetCity: @escaping () -> Void = {}
    ) {
        _viewModel = State(initialValue: PlanViewModel(composer: composer))
        self.context = context
        self.cityName = cityName
        self.onSetCity = onSetCity
    }

    public var body: some View {
        NavigationStack(path: $path) {
            SaturdayPlanScreen(
                viewModel: viewModel,
                context: context,
                cityName: cityName,
                onSelectEvent: { id in path.append(id) },
                onSetCity: onSetCity
            )
            .navigationDestination(for: EventID.self) { id in
                EventDetailScreen(eventID: id)
            }
        }
    }
}
