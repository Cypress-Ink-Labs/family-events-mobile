import Foundation
import FECore

public struct EventQuery: Equatable, Sendable {
    public let cityID: CityID?
    public let dateFrom: Date?
    public let dateTo: Date?
    public let limit: Int
    public let offset: Int

    public init(cityID: CityID? = nil, dateFrom: Date? = nil, dateTo: Date? = nil, limit: Int = 20, offset: Int = 0) {
        self.cityID = cityID
        self.dateFrom = dateFrom
        self.dateTo = dateTo
        self.limit = limit
        self.offset = offset
    }

    /// Stable hash signature for cache keying. Excludes offset so paginated
    /// pages of the same filter set share a signature.
    public var signature: String {
        let iso = ISO8601DateFormatter()
        return "\(cityID?.rawValue ?? "_"):\(dateFrom.map { iso.string(from: $0) } ?? "_"):\(dateTo.map { iso.string(from: $0) } ?? "_"):\(limit)"
    }
}
