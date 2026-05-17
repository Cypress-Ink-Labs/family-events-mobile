import Foundation
import FECore

/// Builds the public `/share/<eventID>` URL the user shares from the
/// detail screen. Mirrors web's `ShareEventButton` which uses
/// `${origin}/share/<id>`. The web origin is read from the `AppWebURL`
/// Info.plist key, falling back to a known production host so dev
/// builds without the key still emit a sensible URL.
public enum EventShareURL {
    public static let fallbackOrigin = "https://family-events.app"

    public static func build(eventID: EventID, origin: String? = nil) -> URL? {
        let base = (origin?.trimmedNonEmpty ?? readInfoPlist() ?? fallbackOrigin)
            .trimmingTrailingSlash
        let path = "/share/" + (eventID.rawValue.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? eventID.rawValue)
        return URL(string: base + path)
    }

    private static func readInfoPlist() -> String? {
        (Bundle.main.object(forInfoDictionaryKey: "AppWebURL") as? String)?.trimmedNonEmpty
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }

    var trimmingTrailingSlash: String {
        hasSuffix("/") ? String(dropLast()) : self
    }
}
