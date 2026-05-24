import SwiftUI
import SwiftData
import FECore
import FEData
import FEDesignSystem

@MainActor
public struct SavedScreen: View {
    @Bindable var coordinator: SavedSyncCoordinator
    public let userID: UserID
    public let onSelectEvent: (EventID) -> Void
    public let onOpenProfile: () -> Void

    @Query(sort: \CachedFavorite.createdAt, order: .reverse) private var favorites: [CachedFavorite]
    @Query private var cachedEvents: [CachedEvent]
    @State private var filter: SavedFilter = .upcoming

    public init(
        coordinator: SavedSyncCoordinator,
        userID: UserID,
        onSelectEvent: @escaping (EventID) -> Void,
        onOpenProfile: @escaping () -> Void
    ) {
        self.coordinator = coordinator
        self.userID = userID
        self.onSelectEvent = onSelectEvent
        self.onOpenProfile = onOpenProfile
    }

    private var eventsByID: [String: CachedEvent] {
        Dictionary(uniqueKeysWithValues: cachedEvents.map { ($0.id, $0) })
    }

    private var resolvedFavorites: [(CachedFavorite, CachedEvent)] {
        favorites.compactMap { fav in
            guard fav.userID == userID.rawValue else { return nil }
            guard let event = eventsByID[fav.eventID] else { return nil }
            return (fav, event)
        }
    }

    private var filteredFavorites: [(CachedFavorite, CachedEvent)] {
        resolvedFavorites.filter { _, event in
            filter.includes(eventStart: event.startDatetime)
        }
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Picker("Filter", selection: $filter) {
                    ForEach(SavedFilter.allCases) { value in
                        Text(value.title).tag(value)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                content
            }
        }
        .navigationTitle("Saved")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: onOpenProfile) {
                    Image(systemName: "person.crop.circle")
                }
                .accessibilityLabel("Profile")
            }
        }
        .task {
            await coordinator.refresh(userID: userID)
            coordinator.startObserving(userID: userID)
        }
        .onDisappear {
            coordinator.stopObserving()
        }
        .refreshable {
            await coordinator.refresh(userID: userID)
        }
    }

    @ViewBuilder
    private var content: some View {
        if coordinator.isRefreshing && resolvedFavorites.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 64)
        } else if let err = coordinator.errorMessage, resolvedFavorites.isEmpty {
            errorState(err)
        } else if resolvedFavorites.isEmpty {
            emptyState
        } else if filteredFavorites.isEmpty {
            filterEmptyState
        } else {
            list
        }
    }

    @ViewBuilder
    private var filterEmptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: filter == .past ? "clock.arrow.circlepath" : "calendar.badge.clock")
                .font(.system(size: 40))
                .foregroundStyle(Color.dsTextMuted)
            Text(filter == .past ? "No past events yet" : "No upcoming saved events")
                .font(.headline)
            Text(filter == .past
                 ? "Past saved events will show up here once your favorites have passed."
                 : "Save more upcoming events from Explore or Plan.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }

    @ViewBuilder
    private func errorState(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle").font(.system(size: 36)).foregroundStyle(.orange)
            Text(message).foregroundStyle(Color.dsTextMuted).multilineTextAlignment(.center).padding(.horizontal, 24)
            Button("Retry") { Task { await coordinator.refresh(userID: userID) } }.buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity).padding(.top, 64)
    }

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "heart").font(.system(size: 48)).foregroundStyle(Color.dsTextMuted)
            Text("No saved events yet").font(.headline)
            Text("Tap the heart on any event to save it for later.").foregroundStyle(Color.dsTextMuted).multilineTextAlignment(.center).padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity).padding(.top, 64)
    }

    @ViewBuilder
    private var list: some View {
        LazyVStack(spacing: 12) {
            ForEach(filteredFavorites, id: \.0.compositeKey) { _, event in
                EventCard(
                    title: event.title,
                    subtitle: subtitle(for: event),
                    imageURL: SafeImageURL.resolve(
                        images: event.imageURLs,
                        seed: event.id,
                        aspect: .card
                    ),
                    badge: event.isFree ? "Free" : nil,
                    onTap: { onSelectEvent(EventID(event.id)) }
                )
                .padding(.horizontal, 16)
            }
        }
        .padding(.vertical, 12)
    }

    private func subtitle(for event: CachedEvent) -> String {
        let base = DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime)
        if let venue = event.venueName, !venue.isEmpty {
            return "\(base) · \(venue)"
        }
        return base
    }
}
