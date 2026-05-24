import Foundation

/// Mirrors web's `safeImageSrc` + picsum fallback so iOS gets the same
/// resilience: filters non-http(s) URLs out, returns a deterministic
/// picsum.photos placeholder seeded by the eventID when no usable image
/// is present.
///
/// Why a placeholder at all: scraped events frequently land with an
/// empty `images` array, or with `data:` / relative URLs the SwiftUI
/// AsyncImage can't load. Without a fallback, cards render as empty
/// rectangles and the layout looks broken even when the data is fine.
public enum SafeImageURL {
    private static let allowedSchemes: Set<String> = ["http", "https"]

    /// Returns the first usable image URL from `images`, or a picsum
    /// placeholder seeded by `seed` (typically the event ID).
    /// `aspect` controls placeholder dimensions: "card" (640×360) or
    /// "hero" (1200×630).
    public static func resolve(
        images: [String],
        seed: String,
        aspect: PlaceholderAspect = .card
    ) -> URL {
        for raw in images {
            if let safe = sanitize(raw) {
                return safe
            }
        }
        return placeholder(seed: seed, aspect: aspect)
    }

    /// Returns the first usable URL or `nil` (no placeholder). Useful when
    /// the caller wants to render an alternative visual when no image is
    /// present rather than a stock photo.
    public static func firstSafe(images: [String]) -> URL? {
        for raw in images {
            if let safe = sanitize(raw) { return safe }
        }
        return nil
    }

    public enum PlaceholderAspect: Sendable {
        case card  // 640×360
        case hero  // 1200×630

        var path: String {
            switch self {
            case .card: return "640/360"
            case .hero: return "1200/630"
            }
        }
    }

    public static func placeholder(seed: String, aspect: PlaceholderAspect = .card) -> URL {
        let cleanSeed = seed.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? "event"
        return URL(string: "https://picsum.photos/seed/\(cleanSeed)/\(aspect.path)")!
    }

    private static func sanitize(_ raw: String) -> URL? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        guard let url = URL(string: trimmed) else { return nil }
        guard let scheme = url.scheme?.lowercased(), allowedSchemes.contains(scheme) else {
            return nil
        }
        guard let host = url.host, !host.isEmpty else { return nil }
        return url
    }
}
