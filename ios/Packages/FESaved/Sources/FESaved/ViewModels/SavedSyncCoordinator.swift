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
public final class SavedSyncCoordinator: Refreshable {
    public private(set) var isRefreshing = false
    public private(set) var errorMessage: String?

    private let favoriteRepo: any FavoriteRepo
    private let eventRepo: any EventRepository
    private let modelContainer: ModelContainer
    private let subscriptionAudit: RealtimeSubscriptionLifecycleAudit
    private let reconnectPolicy: FavoriteSubscriptionReconnectPolicy
    private var observationTask: Task<Void, Never>?
    private var observationAttemptID: UUID?
    private var lastUserID: UserID?

    public init(
        favoriteRepo: any FavoriteRepo,
        eventRepo: any EventRepository,
        modelContainer: ModelContainer,
        subscriptionAudit: RealtimeSubscriptionLifecycleAudit = RealtimeSubscriptionLifecycleAudit(),
        reconnectPolicy: FavoriteSubscriptionReconnectPolicy = FavoriteSubscriptionReconnectPolicy()
    ) {
        self.favoriteRepo = favoriteRepo
        self.eventRepo = eventRepo
        self.modelContainer = modelContainer
        self.subscriptionAudit = subscriptionAudit
        self.reconnectPolicy = reconnectPolicy
    }

    public func refresh() async {
        guard let userID = lastUserID else { return }
        await refresh(userID: userID)
    }

    public func refresh(userID: UserID) async {
        lastUserID = userID
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
        let attemptID = UUID()
        observationAttemptID = attemptID
        observationTask = Task { [weak self] in
            var reconnectAttempts = 0
            while !Task.isCancelled {
                guard let coordinator = self else { return }
                await coordinator.subscriptionAudit.recordAttach()
                let stream = coordinator.favoriteRepo.observeFavorites(for: userID)
                for await change in stream {
                    if Task.isCancelled { break }
                    guard let coordinator = self else { return }
                    coordinator.apply(change: change, userID: userID)
                }
                guard let coordinator = self else { return }
                await coordinator.subscriptionAudit.recordDetach()
                if Task.isCancelled { break }
                guard coordinator.observationAttemptID == attemptID else { break }
                guard reconnectAttempts < coordinator.reconnectPolicy.maxAttempts else {
                    coordinator.errorMessage = "Favorites realtime subscription stopped. Pull to refresh."
                    break
                }
                reconnectAttempts += 1
                await coordinator.subscriptionAudit.recordReconnect()
                try? await Task.sleep(for: coordinator.reconnectPolicy.delay)
            }
            guard let coordinator = self else { return }
            if coordinator.observationAttemptID == attemptID {
                coordinator.observationAttemptID = nil
                coordinator.observationTask = nil
            }
        }
    }

    public func stopObserving() {
        guard let observationTask else { return }
        observationAttemptID = nil
        observationTask.cancel()
        self.observationTask = nil
    }

    public func subscriptionAuditSnapshot() async -> RealtimeSubscriptionAuditSnapshot {
        await subscriptionAudit.snapshot()
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

public struct FavoriteSubscriptionReconnectPolicy: Equatable, Sendable {
    public let maxAttempts: Int
    public let delay: Duration

    public init(maxAttempts: Int = 3, delay: Duration = .seconds(5)) {
        self.maxAttempts = max(0, maxAttempts)
        self.delay = delay
    }
}
