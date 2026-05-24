import Foundation

/// Anything that owns network-backed state and can be re-fetched on demand
/// (pull-to-refresh, scene-phase activation, manual retry).
///
/// View models conform to `Refreshable` so a single
/// `ScenePhaseRefreshController` can drive whichever tab is currently active
/// without knowing each view model's concrete type.
@MainActor
public protocol Refreshable: AnyObject {
    func refresh() async
}
