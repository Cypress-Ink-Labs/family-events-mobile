import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class EventDetailViewModel: Refreshable {
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
    public private(set) var lastFetchedAt: Date?

    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let ratingRepo: (any RatingRepo)?
    private let commentRepo: (any CommentRepo)?
    private let userID: UserID
    private let eventID: EventID
    private var commentObservationTask: Task<Void, Never>?

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

    // Note: explicit cancellation lives in `stopObservingComments()`,
    // invoked from the view's `.onDisappear`. Avoiding a `deinit` keeps
    // @MainActor-isolated property access from triggering Swift 6's
    // "nonisolated deinit" diagnostic.

    public func load() async {
        await fetch(bypassCache: false)
    }

    public func loadIfNeeded() async {
        if event != nil, CacheTTL.isFresh(lastFetchedAt: lastFetchedAt, ttl: CacheTTL.default) {
            return
        }
        await fetch(bypassCache: false)
    }

    /// Pull-to-refresh / scene-phase refresh entry point. Always refetches.
    public func refresh() async {
        await fetch(bypassCache: true)
    }

    /// Starts observing live comment changes (poll-and-diff via CommentRepo).
    /// Idempotent — calling twice does not create duplicate subscriptions.
    /// Pair with `stopObservingComments()` when the view disappears.
    public func startObservingComments() {
        guard commentObservationTask == nil, let repo = commentRepo else { return }
        let stream = repo.observeComments(for: eventID)
        let task = Task { [weak self] in
            for await change in stream {
                guard let self else { return }
                await self.applyCommentChange(change)
            }
        }
        commentObservationTask = task
        // Watcher: when this specific task ends (cancel + drain, or natural
        // stream finish), clear `commentObservationTask` only if it still
        // points at us. Without the identity check, a stop+start cycle that
        // races the old task's drain could wipe the *new* task's handle —
        // leaking it from this VM's perspective and breaking idempotency on
        // the next start.
        Task { @MainActor [weak self, task] in
            _ = await task.value
            guard let self else { return }
            if self.commentObservationTask == task {
                self.commentObservationTask = nil
            }
        }
    }

    public func stopObservingComments() {
        commentObservationTask?.cancel()
        commentObservationTask = nil
    }

    private func applyCommentChange(_ change: CommentChange) async {
        switch change {
        case .inserted(let dto):
            if !comments.contains(where: { $0.id == dto.id }) {
                comments.insert(dto, at: 0)
            }
        case .updated(let dto):
            if let idx = comments.firstIndex(where: { $0.id == dto.id }) {
                comments[idx] = dto
            }
        case .deleted(let commentID):
            comments.removeAll { $0.id == commentID }
        }
    }

    private func fetch(bypassCache: Bool) async {
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
            lastFetchedAt = Date()
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
