import Foundation

public protocol InfoPlistReader {
    func object(forInfoDictionaryKey key: String) -> Any?
}

extension Bundle: InfoPlistReader {}

public struct EnvConfig: Sendable {
    public let supabaseURL: URL
    public let supabaseAnonKey: String
    public let iosGoogleClientID: String?

    public var googleSignInEnabled: Bool {
        guard let id = iosGoogleClientID else { return false }
        return !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    public init(supabaseURL: URL, supabaseAnonKey: String, iosGoogleClientID: String? = nil) {
        self.supabaseURL = supabaseURL
        self.supabaseAnonKey = supabaseAnonKey
        self.iosGoogleClientID = iosGoogleClientID
    }

    public static func load(from reader: InfoPlistReader = Bundle.main) throws -> EnvConfig {
        guard let urlString = reader.object(forInfoDictionaryKey: "SupabaseURL") as? String else {
            throw AppError.config("SupabaseURL")
        }
        let trimmedUrlString = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedUrlString.isEmpty,
              let url = URL(string: trimmedUrlString) else {
            throw AppError.config("SupabaseURL")
        }
        guard let key = reader.object(forInfoDictionaryKey: "SupabaseAnonKey") as? String else {
            throw AppError.config("SupabaseAnonKey")
        }
        let trimmedKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedKey.isEmpty else {
            throw AppError.config("SupabaseAnonKey")
        }
        // Google client ID is optional — blank/missing disables the Google button
        // without throwing, so a partially-configured build still launches.
        let rawGoogleID = reader.object(forInfoDictionaryKey: "IOSGoogleClientID") as? String
        let googleID = rawGoogleID?.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedGoogleID = (googleID?.isEmpty == false) ? googleID : nil
        return EnvConfig(supabaseURL: url, supabaseAnonKey: trimmedKey, iosGoogleClientID: trimmedGoogleID)
    }
}
