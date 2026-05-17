import SwiftUI

/// Legacy color aliases preserved during the v2 token migration. New code
/// should prefer `Color.dsBackground` / `Color.dsTextPrimary` / etc. from
/// `Color+Tokens.swift`. These aliases keep existing call sites in feature
/// packages compiling while they migrate to the explicit `ds*` names.
public extension Color {
    /// `dsAccentPrimary` (neighborhood green). Was the SwiftUI accent color.
    static let appAccent = Color.dsAccentPrimary

    /// `dsBackground` (warm paper / warm dark per appearance).
    static let appBackground = Color.dsBackground

    /// `dsSurfaceRaised` (elevated panels). Was systemSecondaryBackground.
    static let appSecondaryBackground = Color.dsSurfaceRaised

    /// `dsTextPrimary` (warm charcoal / warm off-white per appearance).
    static let appLabel = Color.dsTextPrimary

    /// `dsTextMuted`.
    static let appSecondaryLabel = Color.dsTextMuted
}
