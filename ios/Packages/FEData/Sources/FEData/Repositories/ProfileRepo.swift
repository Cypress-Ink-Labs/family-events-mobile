import Foundation
import FECore
import Supabase

public struct UserProfile: Equatable, Sendable {
    public let id: UserID
    public let email: String?
    public let displayName: String?
    public let avatarURL: String?
    public let cityPreferenceID: CityID?
    public let childName: String?
    public let childAge: Int?

    public init(
        id: UserID,
        email: String?,
        displayName: String?,
        avatarURL: String?,
        cityPreferenceID: CityID?,
        childName: String?,
        childAge: Int?
    ) {
        self.id = id
        self.email = email
        self.displayName = displayName
        self.avatarURL = avatarURL
        self.cityPreferenceID = cityPreferenceID
        self.childName = childName
        self.childAge = childAge
    }
}

public struct UserProfileUpdate: Equatable, Sendable {
    public let displayName: String?
    public let cityPreferenceID: CityID?
    public let childName: String?
    public let childAge: Int?

    public init(
        displayName: String?,
        cityPreferenceID: CityID?,
        childName: String?,
        childAge: Int?
    ) {
        self.displayName = displayName
        self.cityPreferenceID = cityPreferenceID
        self.childName = childName
        self.childAge = childAge
    }
}

public protocol ProfileRepo: Sendable {
    func profile(userID: UserID) async throws -> UserProfile?
    func updateProfile(_ update: UserProfileUpdate, for userID: UserID) async throws -> UserProfile
    func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?)
}

public final class SupabaseProfileRepo: ProfileRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func profile(userID: UserID) async throws -> UserProfile? {
        let response: PostgrestResponse<[UserProfileRow]> = try await supabase.client
            .from("user_profiles")
            .select(Self.profileSelection)
            .eq("id", value: userID.rawValue)
            .limit(1)
            .execute()
        return response.value.first?.toProfile()
    }

    public func updateProfile(_ update: UserProfileUpdate, for userID: UserID) async throws -> UserProfile {
        struct Payload: Encodable {
            let display_name: String?
            let city_preference_id: String?
            let child_name: String?
            let child_age: Int?

            enum CodingKeys: String, CodingKey {
                case display_name
                case city_preference_id
                case child_name
                case child_age
            }

            func encode(to encoder: Encoder) throws {
                var container = encoder.container(keyedBy: CodingKeys.self)
                try encodeNullable(display_name, for: .display_name, into: &container)
                try encodeNullable(city_preference_id, for: .city_preference_id, into: &container)
                try encodeNullable(child_name, for: .child_name, into: &container)
                try encodeNullable(child_age, for: .child_age, into: &container)
            }

            private func encodeNullable<T: Encodable>(
                _ value: T?,
                for key: CodingKeys,
                into container: inout KeyedEncodingContainer<CodingKeys>
            ) throws {
                if let value {
                    try container.encode(value, forKey: key)
                } else {
                    try container.encodeNil(forKey: key)
                }
            }
        }

        let response: PostgrestResponse<[UserProfileRow]> = try await supabase.client
            .from("user_profiles")
            .update(Payload(
                display_name: update.displayName,
                city_preference_id: update.cityPreferenceID?.rawValue,
                child_name: update.childName,
                child_age: update.childAge
            ))
            .eq("id", value: userID.rawValue)
            .select(Self.profileSelection)
            .execute()
        guard let row = response.value.first else {
            throw AppError.notFound
        }
        return row.toProfile()
    }

    public func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?) {
        guard let profile = try await profile(userID: userID) else {
            return (nil, nil)
        }
        return (profile.cityPreferenceID, profile.childAge)
    }

    private static let profileSelection = "id,email,display_name,avatar_url,city_preference_id,child_name,child_age"
}

private struct UserProfileRow: Decodable {
    let id: String
    let email: String?
    let display_name: String?
    let avatar_url: String?
    let city_preference_id: String?
    let child_name: String?
    let child_age: Int?

    func toProfile() -> UserProfile {
        UserProfile(
            id: UserID(id),
            email: email,
            displayName: display_name,
            avatarURL: avatar_url,
            cityPreferenceID: city_preference_id.map(CityID.init),
            childName: child_name,
            childAge: child_age
        )
    }
}
