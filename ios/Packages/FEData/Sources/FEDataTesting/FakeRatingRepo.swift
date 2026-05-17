import Foundation
import FECore
import FEData

public final class FakeRatingRepo: RatingRepo, @unchecked Sendable {
    public var userRatingResult: RatingDTO?
    public var userRatingError: Error?
    public var upsertResult: RatingDTO?
    public var upsertError: Error?
    public private(set) var upsertedScores: [Int] = []

    public init(userRatingResult: RatingDTO? = nil, upsertResult: RatingDTO? = nil) {
        self.userRatingResult = userRatingResult
        self.upsertResult = upsertResult
    }

    public func userRating(for userID: UserID, eventID: EventID) async throws -> RatingDTO? {
        if let userRatingError { throw userRatingError }
        return userRatingResult
    }

    public func upsertRating(score: Int, for userID: UserID, eventID: EventID) async throws -> RatingDTO {
        if let upsertError { throw upsertError }
        upsertedScores.append(score)
        if let upsertResult { return upsertResult }
        return RatingDTO(
            id: UUID().uuidString,
            userID: userID,
            eventID: eventID,
            score: score,
            createdAt: Date()
        )
    }
}
