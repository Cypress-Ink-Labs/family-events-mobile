import Foundation

public protocol InfoPlistReader {
    func object(forInfoDictionaryKey key: String) -> Any?
}

extension Bundle: InfoPlistReader {}

public struct EnvConfig: Sendable {
    public let supabaseURL: URL
    public let supabaseAnonKey: String

    public init(supabaseURL: URL, supabaseAnonKey: String) {
        self.supabaseURL = supabaseURL
        self.supabaseAnonKey = supabaseAnonKey
    }

    public static func load(from reader: InfoPlistReader = Bundle.main) throws -> EnvConfig {
        guard let urlString = reader.object(forInfoDictionaryKey: "SupabaseURL") as? String,
              let url = URL(string: urlString) else {
            throw AppError.config("SupabaseURL")
        }
        guard let key = reader.object(forInfoDictionaryKey: "SupabaseAnonKey") as? String else {
            throw AppError.config("SupabaseAnonKey")
        }
        return EnvConfig(supabaseURL: url, supabaseAnonKey: key)
    }
}
