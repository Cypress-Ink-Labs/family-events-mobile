import SwiftUI
import FECore
import FEData
import FEDesignSystem
import FEEventDetail

public struct ExploreTab: View {
    public let tabTitle = "Explore"
    @State private var viewModel: ExploreViewModel
    @State private var path: [EventID] = []
    private let eventRepo: any EventRepository
    private let userID: UserID

    public init(eventRepo: any EventRepository, userID: UserID, cityID: CityID?) {
        self.eventRepo = eventRepo
        self.userID = userID
        _viewModel = State(initialValue: ExploreViewModel(eventRepo: eventRepo, userID: userID, cityID: cityID))
    }

    public var body: some View {
        NavigationStack(path: $path) {
            ExploreScreen(viewModel: viewModel, onSelectEvent: { id in path.append(id) })
                .navigationDestination(for: EventID.self) { id in
                    EventDetailScreen(eventID: id, eventRepo: eventRepo, userID: userID)
                }
        }
    }
}
