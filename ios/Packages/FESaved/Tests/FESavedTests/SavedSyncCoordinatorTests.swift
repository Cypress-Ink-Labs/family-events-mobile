import XCTest
import SwiftData
import FECore
import FEData
import FEDataTesting
@testable import FESaved

@MainActor
final class SavedSyncCoordinatorTests: XCTestCase {
    private func makeCoordinator(
        favoriteRepo: any FavoriteRepo,
        eventRepo: FakeEventRepository = FakeEventRepository(),
        subscriptionAudit: RealtimeSubscriptionLifecycleAudit = RealtimeSubscriptionLifecycleAudit(),
        reconnectPolicy: FavoriteSubscriptionReconnectPolicy = FavoriteSubscriptionReconnectPolicy(delay: .zero)
    ) throws -> (SavedSyncCoordinator, ModelContainer) {
        let container = try AppModelContainer.makeInMemory()
        let coord = SavedSyncCoordinator(
            favoriteRepo: favoriteRepo,
            eventRepo: eventRepo,
            modelContainer: container,
            subscriptionAudit: subscriptionAudit,
            reconnectPolicy: reconnectPolicy
        )
        return (coord, container)
    }

    func testRefreshReplacesLocalFavorites() async throws {
        let repo = FakeFavoriteRepo()
        let uid = UserID("u_1")
        let dto = FavoriteDTO(id: "fav_1", userID: uid, eventID: EventID("evt_1"), createdAt: Date())
        repo.favoritesResult = .success([dto])
        let (coord, container) = try makeCoordinator(favoriteRepo: repo)

        await coord.refresh(userID: uid)

        let cached = try container.mainContext.fetch(FetchDescriptor<CachedFavorite>())
        XCTAssertEqual(cached.count, 1)
        XCTAssertEqual(cached.first?.eventID, "evt_1")
    }

    func testRefreshRemovesStaleLocalRows() async throws {
        let repo = FakeFavoriteRepo()
        let uid = UserID("u_1")
        let (coord, container) = try makeCoordinator(favoriteRepo: repo)

        // Seed a stale local favorite that's not on the server.
        let ctx = container.mainContext
        ctx.insert(CachedFavorite(userID: uid.rawValue, eventID: "stale", createdAt: Date(), lastSyncedAt: Date()))
        try ctx.save()

        repo.favoritesResult = .success([])  // server says no favorites
        await coord.refresh(userID: uid)

        XCTAssertEqual(try ctx.fetch(FetchDescriptor<CachedFavorite>()).count, 0)
    }

    func testRefreshWarmsEventCache() async throws {
        let favRepo = FakeFavoriteRepo()
        let eventRepo = FakeEventRepository()
        let uid = UserID("u_1")
        let dto = FavoriteDTO(id: "fav_1", userID: uid, eventID: EventID("evt_1"), createdAt: Date())
        favRepo.favoritesResult = .success([dto])
        eventRepo.fetchByIDsResult = .success([EventDTO.fixture(id: "evt_1", title: "Storytime")])
        let (coord, container) = try makeCoordinator(favoriteRepo: favRepo, eventRepo: eventRepo)

        await coord.refresh(userID: uid)

        let cachedEvents = try container.mainContext.fetch(FetchDescriptor<CachedEvent>())
        XCTAssertEqual(cachedEvents.count, 1)
        XCTAssertEqual(cachedEvents.first?.title, "Storytime")
    }

    func testRefreshSurfacesErrorOnFavoritesFailure() async throws {
        let repo = FakeFavoriteRepo()
        struct Boom: Error {}
        repo.favoritesResult = .failure(Boom())
        let (coord, _) = try makeCoordinator(favoriteRepo: repo)
        await coord.refresh(userID: UserID("u_1"))
        XCTAssertNotNil(coord.errorMessage)
    }

    func testStopObservingLeavesNoLeakedRealtimeSubscription() async throws {
        let repo = ControllableFavoriteRepo()
        let audit = RealtimeSubscriptionLifecycleAudit()
        let (coord, _) = try makeCoordinator(favoriteRepo: repo, subscriptionAudit: audit)

        coord.startObserving(userID: UserID("u_1"))
        await repo.waitForAttachCount(1)

        coord.stopObserving()
        await repo.waitForDetachCount(1)
        await waitUntil { await coord.subscriptionAuditSnapshot().activeSubscriptions == 0 }
        let snapshot = await coord.subscriptionAuditSnapshot()

        XCTAssertEqual(snapshot.activeSubscriptions, 0)
        XCTAssertEqual(snapshot.attachCount, 1)
        XCTAssertEqual(snapshot.detachCount, 1)
        XCTAssertFalse(snapshot.hasLeakedSubscriptions)
    }

    func testEndedStreamReconnectsOnlyWithinPolicyLimit() async throws {
        let repo = FinishingFavoriteRepo()
        let audit = RealtimeSubscriptionLifecycleAudit()
        let (coord, _) = try makeCoordinator(
            favoriteRepo: repo,
            subscriptionAudit: audit,
            reconnectPolicy: FavoriteSubscriptionReconnectPolicy(maxAttempts: 2, delay: .zero)
        )

        coord.startObserving(userID: UserID("u_1"))
        await repo.waitForAttachCount(3)
        await waitUntil { await coord.errorMessage != nil }
        let snapshot = await coord.subscriptionAuditSnapshot()

        XCTAssertEqual(snapshot.activeSubscriptions, 0)
        XCTAssertEqual(snapshot.attachCount, 3)
        XCTAssertEqual(snapshot.detachCount, 3)
        XCTAssertEqual(snapshot.reconnectCount, 2)
        XCTAssertLessThanOrEqual(snapshot.reconnectCount, 2)
        XCTAssertTrue(snapshot.batteryNetworkImpactSummary.contains("reconnects=2"))
    }
}

private final class ControllableFavoriteRepo: FavoriteRepo, @unchecked Sendable {
    private let lock = NSLock()
    private var continuation: AsyncStream<FavoriteChange>.Continuation?
    private var attachCount = 0
    private var detachCount = 0

    func favorites(for userID: UserID) async throws -> [FavoriteDTO] { [] }
    func favorite(eventID: EventID, for userID: UserID) async throws {}
    func unfavorite(eventID: EventID, for userID: UserID) async throws {}

    func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange> {
        AsyncStream { continuation in
            lock.withLock {
                attachCount += 1
                self.continuation = continuation
            }
            continuation.onTermination = { [weak self] _ in
                self?.lock.withLock {
                    self?.detachCount += 1
                    self?.continuation = nil
                }
            }
        }
    }

    func waitForAttachCount(_ expected: Int) async {
        await waitUntil { self.lock.withLock { self.attachCount >= expected } }
    }

    func waitForDetachCount(_ expected: Int) async {
        continuation?.finish()
        await waitUntil { self.lock.withLock { self.detachCount >= expected } }
    }
}

private final class FinishingFavoriteRepo: FavoriteRepo, @unchecked Sendable {
    private let lock = NSLock()
    private var attachCount = 0

    func favorites(for userID: UserID) async throws -> [FavoriteDTO] { [] }
    func favorite(eventID: EventID, for userID: UserID) async throws {}
    func unfavorite(eventID: EventID, for userID: UserID) async throws {}

    func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange> {
        AsyncStream { continuation in
            lock.withLock { attachCount += 1 }
            continuation.finish()
        }
    }

    func waitForAttachCount(_ expected: Int) async {
        await waitUntil { self.lock.withLock { self.attachCount >= expected } }
    }
}

private func waitUntil(
    timeout: Duration = .seconds(1),
    predicate: @escaping @Sendable () async -> Bool
) async {
    let deadline = ContinuousClock.now + timeout
    while ContinuousClock.now < deadline {
        if await predicate() { return }
        try? await Task.sleep(for: .milliseconds(10))
    }
}

private extension NSLock {
    func withLock<T>(_ body: () -> T) -> T {
        lock()
        defer { unlock() }
        return body()
    }
}
