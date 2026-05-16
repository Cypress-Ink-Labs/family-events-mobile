import Foundation
import Observation
import SwiftData
import FECore
import FEData

/// Bridges Supabase favorites realtime + initial fetch into the local SwiftData
/// CachedFavorite table. SavedScreen reads from SwiftData via @Query; this
/// coordinator owns the write side.
@Observable
@MainActor
public final class SavedSyncCoordinator {
    public private(set) var isRefreshing = false
    public private(set) var errorMessage: String?

    private let favoriteRepo: any FavoriteRepo
    private let eventRepo: any EventRepository
    private let modelContainer: ModelContainer
    private var observationTask: Task<Void, Never>?

    public init(
        favoriteRepo: any FavoriteRepo,
        eventRepo: any EventRepository,
        modelContainer: ModelContainer
    ) {
        self.favoriteRepo = favoriteRepo
        self.eventRepo = eventRepo
        self.modelContainer = modelContainer
    }

    public func refresh(userID: UserID) async {
        isRefreshing = true
        errorMessage = nil
        defer { isRefreshing = false }
        do {
            let favorites = try await favoriteRepo.favorites(for: userID)
            try replaceLocal(favorites: favorites, userID: userID)
            await warmEventCache(for: favorites, userID: userID)
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    public func startObserving(userID: UserID) {
        stopObserving()
        observationTask = Task { [weak self] in
            guard let self else { return }
            let stream = self.favoriteRepo.observeFavorites(for: userID)
            for await change in stream {
                self.apply(change: change, userID: userID)
            }
        }
    }

    public func stopObserving() {
        observationTask?.cancel()
        observationTask = nil
    }

    // MARK: - private

    private func apply(change: FavoriteChange, userID: UserID) {
        let ctx = modelContainer.mainContext
        let uid = userID.rawValue
        switch change {
        case .inserted(let dto):
            let key = "\(uid)::\(dto.eventID.rawValue)"
            let descriptor = FetchDescriptor<CachedFavorite>(predicate: #Predicate { $0.compositeKey == key })
            if (try? ctx.fetch(descriptor))?.isEmpty == false { return }
            ctx.insert(CachedFavorite(
                userID: uid,
                eventID: dto.eventID.rawValue,
                createdAt: dto.createdAt,
                lastSyncedAt: Date()
            ))
            try? ctx.save()
        case .deleted(let eventID):
            let key = "\(uid)::\(eventID.rawValue)"
            let descriptor = FetchDescriptor<CachedFavorite>(predicate: #Predicate { $0.compositeKey == key })
            if let existing = (try? ctx.fetch(descriptor)) {
                for fav in existing { ctx.delete(fav) }
                try? ctx.save()
            }
        }
    }

    private func replaceLocal(favorites: [FavoriteDTO], userID: UserID) throws {
        let ctx = modelContainer.mainContext
        let uid = userID.rawValue
        // Wipe any CachedFavorite rows belonging to this user that aren't in the new set.
        let descriptor = FetchDescriptor<CachedFavorite>(predicate: #Predicate { $0.userID == uid })
        let existing = try ctx.fetch(descriptor)
        let serverEventIDs = Set(favorites.map { $0.eventID.rawValue })
        for row in existing where !serverEventIDs.contains(row.eventID) {
            ctx.delete(row)
        }
        let existingEventIDs = Set(existing.map { $0.eventID })
        let now = Date()
        for dto in favorites where !existingEventIDs.contains(dto.eventID.rawValue) {
            ctx.insert(CachedFavorite(
                userID: uid,
                eventID: dto.eventID.rawValue,
                createdAt: dto.createdAt,
                lastSyncedAt: now
            ))
        }
        try ctx.save()
    }

    private func warmEventCache(for favorites: [FavoriteDTO], userID: UserID) async {
        guard !favorites.isEmpty else { return }
        let ids = favorites.map(\.eventID)
        if let events = try? await eventRepo.fetch(ids: ids, for: userID) {
            let ctx = modelContainer.mainContext
            let now = Date()
            for event in events {
                CachedEvent.upsert(event, in: ctx, at: now)
            }
            try? ctx.save()
        }
    }
}
