import SwiftUI
import SwiftData
import FECore
import FEData
import FEDesignSystem
import FEEventDetail

public struct SavedTab: View {
    public let tabTitle = "Saved"
    @State private var coordinator: SavedSyncCoordinator
    @State private var path: [EventID] = []
    private let favoriteRepo: any FavoriteRepo
    private let eventRepo: any EventRepository
    private let userID: UserID
    private let onOpenProfile: () -> Void

    public init(
        favoriteRepo: any FavoriteRepo,
        eventRepo: any EventRepository,
        modelContainer: ModelContainer,
        userID: UserID,
        onOpenProfile: @escaping () -> Void
    ) {
        self.favoriteRepo = favoriteRepo
        self.eventRepo = eventRepo
        self.userID = userID
        self.onOpenProfile = onOpenProfile
        _coordinator = State(initialValue: SavedSyncCoordinator(
            favoriteRepo: favoriteRepo,
            eventRepo: eventRepo,
            modelContainer: modelContainer
        ))
    }

    public var body: some View {
        NavigationStack(path: $path) {
            SavedScreen(
                coordinator: coordinator,
                userID: userID,
                onSelectEvent: { id in path.append(id) },
                onOpenProfile: onOpenProfile
            )
            .navigationDestination(for: EventID.self) { id in
                EventDetailScreen(
                    eventID: id,
                    eventRepo: eventRepo,
                    favoriteRepo: favoriteRepo,
                    userID: userID
                )
            }
        }
    }
}
