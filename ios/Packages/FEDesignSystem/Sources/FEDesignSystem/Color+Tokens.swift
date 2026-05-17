import SwiftUI

/// Bridges generated `DesignTokens.Color.Light` / `.Dark` constants into a
/// colorScheme-aware accessor. Web mirrors this with light + dark CSS vars
/// — using these helpers keeps iOS and web visually in sync.
///
/// Usage: `Color.dsBackground` resolves to the warm-paper light value in
/// `light` colorScheme and the warm-dark value in `dark`. Token names mirror
/// the design-system JSON keys.
public extension Color {
    /// Returns a token color that adapts to the current colorScheme.
    /// SwiftUI evaluates this each render, so toggling the system appearance
    /// re-resolves the color without a view rebuild.
    static func dsToken(
        light: Color,
        dark: Color
    ) -> Color {
        #if canImport(UIKit)
        return Color(uiColor: UIColor { trait in
            switch trait.userInterfaceStyle {
            case .dark: return UIColor(dark)
            default: return UIColor(light)
            }
        })
        #else
        return light
        #endif
    }

    // MARK: - Surfaces

    static let dsBackground = Color.dsToken(
        light: DesignTokens.Color.Light.bg,
        dark: DesignTokens.Color.Dark.bg
    )
    static let dsSurface = Color.dsToken(
        light: DesignTokens.Color.Light.surface,
        dark: DesignTokens.Color.Dark.surface
    )
    static let dsSurfaceRaised = Color.dsToken(
        light: DesignTokens.Color.Light.surfaceRaised,
        dark: DesignTokens.Color.Dark.surfaceRaised
    )

    // MARK: - Text

    static let dsTextPrimary = Color.dsToken(
        light: DesignTokens.Color.Light.textPrimary,
        dark: DesignTokens.Color.Dark.textPrimary
    )
    static let dsTextMuted = Color.dsToken(
        light: DesignTokens.Color.Light.textMuted,
        dark: DesignTokens.Color.Dark.textMuted
    )
    static let dsBorder = Color.dsToken(
        light: DesignTokens.Color.Light.border,
        dark: DesignTokens.Color.Dark.border
    )

    // MARK: - Accents

    /// Neighborhood green. Brand anchor — primary CTAs, active states, brand mark.
    static let dsAccentPrimary = Color.dsToken(
        light: DesignTokens.Color.Light.accentPrimary,
        dark: DesignTokens.Color.Dark.accentPrimary
    )
    static let dsAccentPrimarySoft = Color.dsToken(
        light: DesignTokens.Color.Light.accentPrimarySoft,
        dark: DesignTokens.Color.Dark.accentPrimarySoft
    )
    /// Flyer coral. Action color — Save, Plan, This Weekend, RSVP.
    static let dsAccentSecondary = Color.dsToken(
        light: DesignTokens.Color.Light.accentSecondary,
        dark: DesignTokens.Color.Dark.accentSecondary
    )
    static let dsAccentSecondarySoft = Color.dsToken(
        light: DesignTokens.Color.Light.accentSecondarySoft,
        dark: DesignTokens.Color.Dark.accentSecondarySoft
    )
    /// Civic blue. Location, admin states, source credibility.
    static let dsAccentTertiary = Color.dsToken(
        light: DesignTokens.Color.Light.accentTertiary,
        dark: DesignTokens.Color.Dark.accentTertiary
    )
    static let dsAccentTertiarySoft = Color.dsToken(
        light: DesignTokens.Color.Light.accentTertiarySoft,
        dark: DesignTokens.Color.Dark.accentTertiarySoft
    )
    /// School-bus yellow. Kid affordances only — age fit, free, stroller, indoor backup.
    /// Do NOT use as a general accent.
    static let dsAccentKid = Color.dsToken(
        light: DesignTokens.Color.Light.accentKid,
        dark: DesignTokens.Color.Dark.accentKid
    )
    static let dsAccentKidSoft = Color.dsToken(
        light: DesignTokens.Color.Light.accentKidSoft,
        dark: DesignTokens.Color.Dark.accentKidSoft
    )

    // MARK: - Semantic

    static let dsSuccess = Color.dsToken(
        light: DesignTokens.Color.Light.success,
        dark: DesignTokens.Color.Dark.success
    )
    static let dsWarning = Color.dsToken(
        light: DesignTokens.Color.Light.warning,
        dark: DesignTokens.Color.Dark.warning
    )
    static let dsError = Color.dsToken(
        light: DesignTokens.Color.Light.error,
        dark: DesignTokens.Color.Dark.error
    )
    static let dsInfo = Color.dsToken(
        light: DesignTokens.Color.Light.info,
        dark: DesignTokens.Color.Dark.info
    )
}
