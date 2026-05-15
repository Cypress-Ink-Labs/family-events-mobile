import Foundation
import FECore

public struct PlanEventsRowDTO: Equatable, Sendable, Codable {
    public let eventID: EventID
    public let score: Double
    public let distanceScore: Double
    public let weatherScore: Double
    public let ageScore: Double
    public let historyAffinity: Double
    public let distanceKm: Double?
    public let dayOffset: Int

    private enum CodingKeys: String, CodingKey {
        case eventID = "event_id"
        case score
        case distanceScore = "distance_score"
        case weatherScore = "weather_score"
        case ageScore = "age_score"
        case historyAffinity = "history_affinity"
        case distanceKm = "distance_km"
        case dayOffset = "day_offset"
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        eventID = EventID(try c.decode(String.self, forKey: .eventID))
        score = try Self.coerceDouble(c, .score)
        distanceScore = try Self.coerceDouble(c, .distanceScore)
        weatherScore = try Self.coerceDouble(c, .weatherScore)
        ageScore = try Self.coerceDouble(c, .ageScore)
        historyAffinity = try Self.coerceDouble(c, .historyAffinity)
        distanceKm = try Self.coerceOptionalDouble(c, .distanceKm)
        dayOffset = try Self.coerceInt(c, .dayOffset)
    }

    public init(eventID: EventID, score: Double, distanceScore: Double, weatherScore: Double, ageScore: Double, historyAffinity: Double, distanceKm: Double?, dayOffset: Int) {
        self.eventID = eventID; self.score = score; self.distanceScore = distanceScore
        self.weatherScore = weatherScore; self.ageScore = ageScore
        self.historyAffinity = historyAffinity; self.distanceKm = distanceKm; self.dayOffset = dayOffset
    }

    public func encode(to encoder: Encoder) throws {
        throw EncodingError.invalidValue(self, .init(codingPath: encoder.codingPath, debugDescription: "PlanEventsRowDTO is decode-only"))
    }

    private static func coerceDouble(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) throws -> Double {
        if let s = try? c.decode(String.self, forKey: key), let d = Double(s) { return d }
        return try c.decode(Double.self, forKey: key)
    }
    private static func coerceOptionalDouble(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) throws -> Double? {
        if c.contains(key) == false { return nil }
        if try c.decodeNil(forKey: key) { return nil }
        return try coerceDouble(c, key)
    }
    private static func coerceInt(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) throws -> Int {
        if let s = try? c.decode(String.self, forKey: key), let i = Int(s) { return i }
        return try c.decode(Int.self, forKey: key)
    }
}
