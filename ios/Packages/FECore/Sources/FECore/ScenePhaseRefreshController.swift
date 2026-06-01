import Foundation
import Observation

/// Bridges SwiftUI's `ScenePhase` transitions to the currently active
/// tab's view model so users see fresh data on cold start AND on
/// foreground return.
///
/// Usage at the app root:
/// ```swift
/// @Environment(\.scenePhase) private var scenePhase
/// @State private var refreshController = ScenePhaseRefreshController()
///
/// var body: some View {
///     RootView(...)
///         .environment(refreshController)
///         .onChange(of: scenePhase) { _, phase in
///             refreshController.scenePhaseChanged(phase)
///         }
/// }
/// ```
///
/// Each tab's view model conforms to `Refreshable` and registers itself
/// via `bind(_:)` when it becomes the active tab (typically in `.task`).
@MainActor
@Observable
public final class ScenePhaseRefreshController {
    public enum Phase: Sendable, Equatable {
        case active
        case inactive
        case background
    }

    private weak var activeRefreshable: AnyObject?
    private var refreshClosure: (@MainActor () async -> Void)?
    private var lastBackgroundedAt: Date?
    private var inFlight: Task<Void, Never>?

    public init() {}

    /// Register the currently visible tab's view model. Pass `nil` to clear.
    /// Cancels any in-flight refresh tied to the previous binding so stale
    /// updates can't land on the new view model.
    public func bind(_ refreshable: (any Refreshable)?) {
        inFlight?.cancel()
        inFlight = nil
        activeRefreshable = refreshable as AnyObject?
        if let refreshable {
            refreshClosure = { [weak refreshable] in
                await refreshable?.refresh()
            }
        } else {
            refreshClosure = nil
        }
    }

    /// Forward scene-phase transitions from the SwiftUI environment.
    public func scenePhaseChanged(_ phase: Phase) {
        switch phase {
        case .background:
            lastBackgroundedAt = .now
        case .inactive:
            // Transient — about to background or about to foreground. Ignore.
            break
        case .active:
            // Only refresh if we came back from background. Cold-start `.active`
            // (no prior background timestamp) is already handled by each
            // view's `.task` modifier — refreshing here would double-fetch.
            guard lastBackgroundedAt != nil else { return }
            lastBackgroundedAt = nil
            triggerRefresh()
        }
    }

    /// Forces a refresh of the currently bound view model regardless of
    /// scene phase. Exposed for tests; production code uses `scenePhaseChanged`.
    public func triggerRefresh() {
        guard let refreshClosure else { return }
        inFlight?.cancel()
        inFlight = Task { @MainActor in
            await refreshClosure()
        }
    }

    /// Convenience for SwiftUI integration that uses the framework's
    /// `ScenePhase` enum directly.
    #if canImport(SwiftUI)
    public func scenePhaseChanged(_ phase: SwiftUIScenePhaseBridge) {
        scenePhaseChanged(phase.toPhase())
    }
    #endif
}

/// Erases SwiftUI's `ScenePhase` so this package doesn't have to depend on
/// SwiftUI directly. Call sites convert with `bridge.toPhase()`.
public struct SwiftUIScenePhaseBridge: Sendable {
    public enum Raw: Sendable {
        case active
        case inactive
        case background
    }
    public let raw: Raw
    public init(_ raw: Raw) { self.raw = raw }
    public func toPhase() -> ScenePhaseRefreshController.Phase {
        switch raw {
        case .active: return .active
        case .inactive: return .inactive
        case .background: return .background
        }
    }
}
