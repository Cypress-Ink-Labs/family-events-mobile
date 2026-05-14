import Foundation

public enum KeychainKey: String, Sendable, CaseIterable {
    case accessToken = "supabase.accessToken"
    case refreshToken = "supabase.refreshToken"
    case userID = "supabase.userID"
}
