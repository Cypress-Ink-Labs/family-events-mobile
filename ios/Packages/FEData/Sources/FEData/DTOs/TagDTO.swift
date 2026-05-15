import Foundation

public struct TagDTO: Equatable, Sendable, Codable {
    public let id: String
    public let name: String
    public let slug: String
    public let color: String

    public init(id: String, name: String, slug: String, color: String) {
        self.id = id
        self.name = name
        self.slug = slug
        self.color = color
    }

    private enum CodingKeys: String, CodingKey { case id, name, slug, color }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        slug = try c.decode(String.self, forKey: .slug)
        color = (try? c.decodeIfPresent(String.self, forKey: .color)) ?? ""
    }
}
