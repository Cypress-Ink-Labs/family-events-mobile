import Foundation
import FECore

public struct EventDTO: Equatable, Sendable, Codable {
    public let id: EventID
    public let title: String
    public let description: String?
    public let startDatetime: Date
    public let endDatetime: Date?
    public let timezone: String
    public let venueName: String?
    public let address: String?
    public let cityID: CityID?
    public let latitude: Double?
    public let longitude: Double?
    public let ageMin: Int?
    public let ageMax: Int?
    public let price: Double?
    public let isFree: Bool
    public let sourceURL: String?
    public let sourceName: String?
    public let sourceID: String?
    public let images: [String]
    public let status: String
    public let aiConfidence: Double?
    public let aiTagProvider: String?
    public let isFeatured: Bool
    public let viewCount: Int
    public let createdAt: Date
    public let updatedAt: Date
    public let tags: [TagDTO]
    public let avgRating: Double
    public let ratingCount: Int
    public let isFavorited: Bool

    private enum CodingKeys: String, CodingKey {
        case id, title, description, timezone, address, latitude, longitude
        case startDatetime = "start_datetime"
        case endDatetime = "end_datetime"
        case venueName = "venue_name"
        case cityID = "city_id"
        case ageMin = "age_min"
        case ageMax = "age_max"
        case price
        case isFree = "is_free"
        case sourceURL = "source_url"
        case sourceName = "source_name"
        case sourceID = "source_id"
        case images, status
        case aiConfidence = "ai_confidence"
        case aiTagProvider = "ai_tag_provider"
        case isFeatured = "is_featured"
        case viewCount = "view_count"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case tags
        case avgRating = "avg_rating"
        case ratingCount = "rating_count"
        case isFavorited = "is_favorited"
    }

    private static let isoFrac: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    private static let isoPlain: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    /// Strict ISO8601 parser. TODO #1: throws on malformed input instead of falling back to epoch 0.
    static func parseDate(_ s: String, path: [CodingKey] = []) throws -> Date {
        if let d = isoFrac.date(from: s) { return d }
        if let d = isoPlain.date(from: s) { return d }
        throw DecodingError.dataCorrupted(.init(codingPath: path, debugDescription: "Invalid ISO8601 date: \(s)"))
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = EventID(try c.decode(String.self, forKey: .id))
        title = try c.decode(String.self, forKey: .title)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        startDatetime = try Self.parseDate(try c.decode(String.self, forKey: .startDatetime), path: c.codingPath + [CodingKeys.startDatetime])
        if let endStr = try c.decodeIfPresent(String.self, forKey: .endDatetime) {
            endDatetime = try Self.parseDate(endStr, path: c.codingPath + [CodingKeys.endDatetime])
        } else { endDatetime = nil }
        timezone = try c.decode(String.self, forKey: .timezone)
        venueName = try c.decodeIfPresent(String.self, forKey: .venueName)
        address = try c.decodeIfPresent(String.self, forKey: .address)
        cityID = (try c.decodeIfPresent(String.self, forKey: .cityID)).map(CityID.init)
        latitude = try c.decodeIfPresent(Double.self, forKey: .latitude)
        longitude = try c.decodeIfPresent(Double.self, forKey: .longitude)
        ageMin = try c.decodeIfPresent(Int.self, forKey: .ageMin)
        ageMax = try c.decodeIfPresent(Int.self, forKey: .ageMax)
        price = try c.decodeIfPresent(Double.self, forKey: .price)
        isFree = try c.decode(Bool.self, forKey: .isFree)
        sourceURL = try c.decodeIfPresent(String.self, forKey: .sourceURL)
        sourceName = try c.decodeIfPresent(String.self, forKey: .sourceName)
        sourceID = try c.decodeIfPresent(String.self, forKey: .sourceID)
        images = (try? c.decodeIfPresent([String].self, forKey: .images)) ?? []
        status = try c.decode(String.self, forKey: .status)
        aiConfidence = try c.decodeIfPresent(Double.self, forKey: .aiConfidence)
        aiTagProvider = try c.decodeIfPresent(String.self, forKey: .aiTagProvider)
        isFeatured = try c.decode(Bool.self, forKey: .isFeatured)
        viewCount = try c.decode(Int.self, forKey: .viewCount)
        createdAt = try Self.parseDate(try c.decode(String.self, forKey: .createdAt), path: c.codingPath + [CodingKeys.createdAt])
        updatedAt = try Self.parseDate(try c.decode(String.self, forKey: .updatedAt), path: c.codingPath + [CodingKeys.updatedAt])
        tags = (try? c.decode([TagDTO].self, forKey: .tags)) ?? []
        avgRating = (try? c.decodeIfPresent(Double.self, forKey: .avgRating)) ?? 0
        ratingCount = (try? c.decodeIfPresent(Int.self, forKey: .ratingCount)) ?? 0
        isFavorited = (try? c.decodeIfPresent(Bool.self, forKey: .isFavorited)) ?? false
    }

    public func encode(to encoder: Encoder) throws {
        throw EncodingError.invalidValue(self, .init(codingPath: encoder.codingPath, debugDescription: "EventDTO is decode-only"))
    }

    public init(
        id: EventID, title: String, description: String?,
        startDatetime: Date, endDatetime: Date?, timezone: String,
        venueName: String?, address: String?, cityID: CityID?,
        latitude: Double?, longitude: Double?, ageMin: Int?, ageMax: Int?,
        price: Double?, isFree: Bool, sourceURL: String?, sourceName: String?,
        sourceID: String?, images: [String], status: String,
        aiConfidence: Double?, aiTagProvider: String?, isFeatured: Bool,
        viewCount: Int, createdAt: Date, updatedAt: Date,
        tags: [TagDTO], avgRating: Double, ratingCount: Int, isFavorited: Bool
    ) {
        self.id = id; self.title = title; self.description = description
        self.startDatetime = startDatetime; self.endDatetime = endDatetime; self.timezone = timezone
        self.venueName = venueName; self.address = address; self.cityID = cityID
        self.latitude = latitude; self.longitude = longitude
        self.ageMin = ageMin; self.ageMax = ageMax
        self.price = price; self.isFree = isFree
        self.sourceURL = sourceURL; self.sourceName = sourceName; self.sourceID = sourceID
        self.images = images; self.status = status
        self.aiConfidence = aiConfidence; self.aiTagProvider = aiTagProvider
        self.isFeatured = isFeatured; self.viewCount = viewCount
        self.createdAt = createdAt; self.updatedAt = updatedAt
        self.tags = tags; self.avgRating = avgRating; self.ratingCount = ratingCount
        self.isFavorited = isFavorited
    }
}
