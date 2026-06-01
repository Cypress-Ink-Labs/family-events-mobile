import SwiftUI
import FECore
import FEData
import FEDesignSystem
import FEEventDetail

@MainActor
public struct ExploreTab: View {
    public let tabTitle = "Explore"
    @Environment(ScenePhaseRefreshController.self) private var refreshController
    @State private var viewModel: ExploreViewModel
    @State private var path: [EventID] = []
    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let ratingRepo: (any RatingRepo)?
    private let commentRepo: (any CommentRepo)?
    private let userID: UserID

    public init(
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        ratingRepo: (any RatingRepo)? = nil,
        commentRepo: (any CommentRepo)? = nil,
        userID: UserID,
        cityID: CityID?
    ) {
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
        self.ratingRepo = ratingRepo
        self.commentRepo = commentRepo
        self.userID = userID
        _viewModel = State(initialValue: ExploreViewModel(eventRepo: eventRepo, userID: userID, cityID: cityID))
    }

    public var body: some View {
        NavigationStack(path: $path) {
            ExploreScreen(viewModel: viewModel, onSelectEvent: { id in path.append(id) })
                .navigationDestination(for: EventID.self) { id in
                    EventDetailScreen(
                        eventID: id,
                        eventRepo: eventRepo,
                        favoriteRepo: favoriteRepo,
                        ratingRepo: ratingRepo,
                        commentRepo: commentRepo,
                        userID: userID
                    )
                }
        }
        .onAppear { refreshController.bind(viewModel) }
    }
}
