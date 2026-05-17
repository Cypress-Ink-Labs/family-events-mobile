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
    public private(set) var userRating: RatingDTO?
    public private(set) var isRatingInFlight = false
    public private(set) var comments: [CommentDTO] = []
    public private(set) var isCommentInFlight = false
    public private(set) var commentError: String?

    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let ratingRepo: (any RatingRepo)?
    private let commentRepo: (any CommentRepo)?
    private let userID: UserID
    private let eventID: EventID

    public init(
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        ratingRepo: (any RatingRepo)? = nil,
        commentRepo: (any CommentRepo)? = nil,
        userID: UserID,
        eventID: EventID
    ) {
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
        self.ratingRepo = ratingRepo
        self.commentRepo = commentRepo
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
        await loadRatingAndComments()
    }

    private func loadRatingAndComments() async {
        async let ratingTask: RatingDTO? = {
            guard let repo = ratingRepo else { return nil }
            return try? await repo.userRating(for: userID, eventID: eventID)
        }()
        async let commentsTask: [CommentDTO] = {
            guard let repo = commentRepo else { return [] }
            return (try? await repo.comments(for: eventID)) ?? []
        }()
        userRating = await ratingTask
        comments = await commentsTask
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

    public func setRating(_ score: Int) async {
        guard let repo = ratingRepo, !isRatingInFlight else { return }
        let previous = userRating
        isRatingInFlight = true
        defer { isRatingInFlight = false }
        do {
            let updated = try await repo.upsertRating(score: score, for: userID, eventID: eventID)
            userRating = updated
        } catch let app as AppError {
            userRating = previous
            errorMessage = app.userMessage
        } catch {
            userRating = previous
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    public func addComment(_ body: String) async {
        guard let repo = commentRepo, !isCommentInFlight else { return }
        let trimmed = body.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isCommentInFlight = true
        commentError = nil
        defer { isCommentInFlight = false }
        do {
            let inserted = try await repo.addComment(body: trimmed, for: userID, eventID: eventID)
            comments.insert(inserted, at: 0)
        } catch let app as AppError {
            commentError = app.userMessage
        } catch {
            commentError = AppError.unknown(error).userMessage
        }
    }
}
