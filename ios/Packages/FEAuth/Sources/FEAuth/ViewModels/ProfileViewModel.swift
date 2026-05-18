import Foundation
import Observation
import FECore
import FEData

@Observable
@MainActor
public final class ProfileViewModel {
    public let userID: UserID
    private let profileRepo: any ProfileRepo
    private let cityRepo: any CityRepository
    private let authService: any AuthService

    public private(set) var profile: UserProfile?
    public private(set) var cities: [CitySummary] = []
    public private(set) var isLoading = false
    public private(set) var isSaving = false
    public private(set) var isChangingPassword = false
    public var errorMessage: String?
    public var displayName = ""
    public var childName = ""
    public var childAgeText = ""
    public var selectedCityID: CityID?

    public init(
        userID: UserID,
        profileRepo: any ProfileRepo,
        cityRepo: any CityRepository,
        authService: any AuthService
    ) {
        self.userID = userID
        self.profileRepo = profileRepo
        self.cityRepo = cityRepo
        self.authService = authService
    }

    public var hasUnsavedChanges: Bool {
        let baseline = profile ?? emptyProfile()
        return trimmedNil(displayName) != baseline.displayName
            || trimmedNil(childName) != baseline.childName
            || parsedChildAgeForComparison() != baseline.childAge
            || selectedCityID != baseline.cityPreferenceID
    }

    public func load() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            async let fetchedProfile = profileRepo.profile(userID: userID)
            async let fetchedCities = cityRepo.cities()
            let loadedProfile = try await fetchedProfile ?? emptyProfile()
            let loadedCities = try await fetchedCities
            profile = loadedProfile
            cities = loadedCities
            resetDrafts(from: loadedProfile)
        } catch let error as AppError {
            errorMessage = error.userMessage
            if profile == nil {
                profile = emptyProfile()
                resetDrafts(from: profile)
            }
        } catch {
            errorMessage = "Couldn't load your profile."
            if profile == nil {
                profile = emptyProfile()
                resetDrafts(from: profile)
            }
        }
    }

    public func discardChanges() {
        resetDrafts(from: profile ?? emptyProfile())
    }

    @discardableResult
    public func save() async -> UserProfile? {
        let validatedAge = validateChildAge()
        guard validatedAge.isValid else {
            return nil
        }

        isSaving = true
        errorMessage = nil
        defer { isSaving = false }

        do {
            let saved = try await profileRepo.updateProfile(
                UserProfileUpdate(
                    displayName: trimmedNil(displayName),
                    cityPreferenceID: selectedCityID,
                    childName: trimmedNil(childName),
                    childAge: validatedAge.value
                ),
                for: userID
            )
            profile = saved
            resetDrafts(from: saved)
            return saved
        } catch let error as AppError {
            errorMessage = error.userMessage
        } catch {
            errorMessage = "Couldn't save your profile."
        }
        return nil
    }

    public func changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) async -> Bool {
        guard let email = profile?.email?.trimmingCharacters(in: .whitespacesAndNewlines), !email.isEmpty else {
            errorMessage = "Email is unavailable for this account."
            return false
        }
        guard !currentPassword.isEmpty else {
            errorMessage = "Enter your current password."
            return false
        }
        guard newPassword.count >= 6 else {
            errorMessage = "New password must be at least 6 characters."
            return false
        }
        guard newPassword == confirmPassword else {
            errorMessage = "New passwords don't match."
            return false
        }
        guard newPassword != currentPassword else {
            errorMessage = "New password must differ from the current one."
            return false
        }

        isChangingPassword = true
        errorMessage = nil
        defer { isChangingPassword = false }

        do {
            try await authService.changePassword(
                email: email,
                currentPassword: currentPassword,
                newPassword: newPassword
            )
            return true
        } catch let error as AppError {
            errorMessage = error.userMessage
        } catch {
            errorMessage = "Couldn't update your password."
        }
        return false
    }

    private func resetDrafts(from profile: UserProfile?) {
        guard let profile else { return }
        displayName = profile.displayName ?? ""
        childName = profile.childName ?? ""
        childAgeText = profile.childAge.map(String.init) ?? ""
        selectedCityID = profile.cityPreferenceID
    }

    private func emptyProfile() -> UserProfile {
        UserProfile(
            id: userID,
            email: nil,
            displayName: nil,
            avatarURL: nil,
            cityPreferenceID: nil,
            childName: nil,
            childAge: nil
        )
    }

    private func validateChildAge() -> (isValid: Bool, value: Int?) {
        let trimmed = childAgeText.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return (true, nil)
        }
        guard let age = Int(trimmed), (0...18).contains(age) else {
            errorMessage = "Child's age must be between 0 and 18."
            return (false, nil)
        }
        return (true, age)
    }

    private func parsedChildAgeForComparison() -> Int? {
        let trimmed = childAgeText.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : Int(trimmed)
    }
}

private func trimmedNil(_ value: String) -> String? {
    let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? nil : trimmed
}
