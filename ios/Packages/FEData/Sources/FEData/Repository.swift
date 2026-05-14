import Foundation

/// Every domain repository conforms. `refresh` is the network-pull side; the
/// SwiftData-observation side is exposed per-repo with concrete return types.
public protocol Repository: Sendable {
    func refresh() async throws
}
