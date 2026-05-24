import SwiftUI
import FECore
import FEData
import FEDesignSystem
import FEEventDetail
import FECityPicker

@MainActor
public struct MapTab: View {
    public let tabTitle = "Map"

    @State private var viewModel: MapViewModel
    @State private var path: [EventID] = []
    @State private var citySelection: CityPickerToolbarButton.Selection

    private let eventRepo: any EventRepository
    private let cityRepo: any CityRepository
    private let favoriteRepo: any FavoriteRepo
    private let ratingRepo: (any RatingRepo)?
    private let commentRepo: (any CommentRepo)?
    private let userID: UserID
    private let onCityChanged: (CitySummary) -> Void

    public init(
        eventRepo: any EventRepository,
        cityRepo: any CityRepository,
        favoriteRepo: any FavoriteRepo,
        ratingRepo: (any RatingRepo)? = nil,
        commentRepo: (any CommentRepo)? = nil,
        userID: UserID,
        cityID: CityID?,
        cityName: String?,
        onCityChanged: @escaping (CitySummary) -> Void
    ) {
        self.eventRepo = eventRepo
        self.cityRepo = cityRepo
        self.favoriteRepo = favoriteRepo
        self.ratingRepo = ratingRepo
        self.commentRepo = commentRepo
        self.userID = userID
        self.onCityChanged = onCityChanged
        _viewModel = State(initialValue: MapViewModel(eventRepo: eventRepo, userID: userID, cityID: cityID))
        _citySelection = State(initialValue: CityPickerToolbarButton.Selection(cityID: cityID, cityName: cityName))
    }

    public var body: some View {
        NavigationStack(path: $path) {
            MapScreen(viewModel: viewModel, onSelectEvent: { id in
                path.append(id)
            })
            .toolbar {
                ToolbarItem(placement: cityPickerPlacement) {
                    CityPickerToolbarButton(
                        cityRepo: cityRepo,
                        selection: citySelection,
                        onSelect: { city in
                            citySelection = .init(cityID: city.id, cityName: city.name)
                            onCityChanged(city)
                            Task { await viewModel.updateCity(city.id) }
                        }
                    )
                }
            }
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
    }

    private var cityPickerPlacement: ToolbarItemPlacement {
        #if os(iOS)
        return .topBarLeading
        #else
        return .navigation
        #endif
    }
}
