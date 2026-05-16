import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class EventDetailViewModel {
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var event: EventDTO?
    public private(set) var isFavorited = false
    public private(set) var isFavoriteInFlight = false

    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let userID: UserID
    private let eventID: EventID

    public init(
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        userID: UserID,
        eventID: EventID
    ) {
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
        self.userID = userID
        self.eventID = eventID
    }

    public func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            let events = try await eventRepo.fetch(ids: [eventID], for: userID)
            event = events.first
            if event == nil {
                errorMessage = "Event not found."
            } else {
                isFavorited = event?.isFavorited ?? false
            }
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    public func toggleFavorite() {
        guard !isFavoriteInFlight else { return }
        isFavoriteInFlight = true
        let wasFavorited = isFavorited
        isFavorited.toggle()  // optimistic
        Task {
            do {
                if wasFavorited {
                    try await favoriteRepo.unfavorite(eventID: eventID, for: userID)
                } else {
                    try await favoriteRepo.favorite(eventID: eventID, for: userID)
                }
            } catch let app as AppError {
                isFavorited = wasFavorited
                errorMessage = app.userMessage
            } catch {
                isFavorited = wasFavorited
                errorMessage = AppError.unknown(error).userMessage
            }
            isFavoriteInFlight = false
        }
    }
}
