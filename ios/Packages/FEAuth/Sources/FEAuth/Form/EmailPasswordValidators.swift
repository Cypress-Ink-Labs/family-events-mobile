import Foundation

public enum EmailPasswordValidators {
    public static func emailError(_ value: String) -> String? {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "Email is required." }
        let regex = #"^[^\s@]+@[^\s@]+\.[^\s@]+$"#
        if trimmed.range(of: regex, options: .regularExpression) == nil {
            return "Please enter a valid email."
        }
        return nil
    }

    public static func passwordError(_ value: String) -> String? {
        if value.count < 8 { return "Password must be at least 8 characters." }
        return nil
    }
}
