import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class NotificationPreferencesViewModel {
    public private(set) var preferences: NotificationPreferences = .defaults
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?

    private let userID: UserID
    private let repo: any NotificationPreferencesRepo

    public init(userID: UserID, repo: any NotificationPreferencesRepo) {
        self.userID = userID
        self.repo = repo
    }

    public func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            preferences = try await repo.fetch(userID: userID)
        } catch {
            preferences = .defaults
        }
    }

    public func set(_ keyPath: WritableKeyPath<NotificationPreferences, Bool>, to value: Bool) async {
        let previous = preferences
        preferences[keyPath: keyPath] = value
        do {
            preferences = try await repo.upsert(preferences, for: userID)
        } catch {
            preferences = previous
            errorMessage = "Couldn't save notification preferences."
        }
    }
}
