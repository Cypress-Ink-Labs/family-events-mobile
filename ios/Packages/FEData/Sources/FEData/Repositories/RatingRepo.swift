import Foundation
import FECore

public protocol RatingRepo: Sendable {
    func userRating(for userID: UserID, eventID: EventID) async throws -> RatingDTO?
    func upsertRating(score: Int, for userID: UserID, eventID: EventID) async throws -> RatingDTO
}
