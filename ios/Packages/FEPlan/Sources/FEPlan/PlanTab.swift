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
    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo

    public init(
        composer: PlanComposer,
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        context: PlanContext,
        cityName: String? = nil,
        onSetCity: @escaping () -> Void = {}
    ) {
        _viewModel = State(initialValue: PlanViewModel(composer: composer))
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
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
                EventDetailScreen(eventID: id, eventRepo: eventRepo, favoriteRepo: favoriteRepo, userID: context.userID)
            }
        }
    }
}
