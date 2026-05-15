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
    /// UI-local optimistic flag. M7 wires real persistence.
    public var isFavorited = false

    private let eventRepo: any EventRepository
    private let userID: UserID
    private let eventID: EventID

    public init(eventRepo: any EventRepository, userID: UserID, eventID: EventID) {
        self.eventRepo = eventRepo
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
        // M4 ships UI-only toggle; M7 wires repository.
        isFavorited.toggle()
    }
}
