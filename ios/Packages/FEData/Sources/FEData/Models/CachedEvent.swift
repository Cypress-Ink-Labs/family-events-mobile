import Foundation
import SwiftData

@Model
public final class CachedEvent {
    @Attribute(.unique) public var id: String
    public var title: String
    public var eventDescription: String?
    public var startDatetime: Date
    public var endDatetime: Date?
    public var timezone: String
    public var venueName: String?
    public var address: String?
    public var cityID: String?
    public var latitude: Double?
    public var longitude: Double?
    public var ageMin: Int?
    public var ageMax: Int?
    public var price: Double?
    public var isFree: Bool
    public var imageURLs: [String]
    public var avgRating: Double
    public var ratingCount: Int
    public var isFavorited: Bool
    public var lastSyncedAt: Date

    public init(
        id: String, title: String, eventDescription: String? = nil,
        startDatetime: Date, endDatetime: Date? = nil, timezone: String = "UTC",
        venueName: String? = nil, address: String? = nil, cityID: String? = nil,
        latitude: Double? = nil, longitude: Double? = nil,
        ageMin: Int? = nil, ageMax: Int? = nil, price: Double? = nil,
        isFree: Bool = false, imageURLs: [String] = [],
        avgRating: Double = 0, ratingCount: Int = 0, isFavorited: Bool = false,
        lastSyncedAt: Date
    ) {
        self.id = id; self.title = title; self.eventDescription = eventDescription
        self.startDatetime = startDatetime; self.endDatetime = endDatetime; self.timezone = timezone
        self.venueName = venueName; self.address = address; self.cityID = cityID
        self.latitude = latitude; self.longitude = longitude
        self.ageMin = ageMin; self.ageMax = ageMax
        self.price = price; self.isFree = isFree
        self.imageURLs = imageURLs
        self.avgRating = avgRating; self.ratingCount = ratingCount; self.isFavorited = isFavorited
        self.lastSyncedAt = lastSyncedAt
    }
}

extension CachedEvent {
    public static func upsert(_ dto: EventDTO, in context: ModelContext, at syncedAt: Date) {
        let id = dto.id.rawValue
        let descriptor = FetchDescriptor<CachedEvent>(predicate: #Predicate { $0.id == id })
        let existing = (try? context.fetch(descriptor))?.first
        let target = existing ?? CachedEvent(
            id: id, title: dto.title, startDatetime: dto.startDatetime,
            lastSyncedAt: syncedAt
        )
        target.title = dto.title
        target.eventDescription = dto.description
        target.startDatetime = dto.startDatetime
        target.endDatetime = dto.endDatetime
        target.timezone = dto.timezone
        target.venueName = dto.venueName
        target.address = dto.address
        target.cityID = dto.cityID?.rawValue
        target.latitude = dto.latitude
        target.longitude = dto.longitude
        target.ageMin = dto.ageMin
        target.ageMax = dto.ageMax
        target.price = dto.price
        target.isFree = dto.isFree
        target.imageURLs = dto.images
        target.avgRating = dto.avgRating
        target.ratingCount = dto.ratingCount
        target.isFavorited = dto.isFavorited
        target.lastSyncedAt = syncedAt
        if existing == nil { context.insert(target) }
    }
}
