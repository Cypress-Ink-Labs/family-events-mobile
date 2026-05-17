import SwiftUI

/// Legacy typography enum preserved during the v2 token migration. The four
/// cases now route through `AppFont` (Fraunces / DM Sans tokens) so callers
/// automatically pick up the new type system. New code should prefer
/// `Font.dsTitleLg` / `Font.dsBody` / etc. from `Typography+Tokens.swift`.
public enum AppTypography: Sendable {
    case titleLarge
    case titleMedium
    case body
    case caption

    public var font: Font {
        switch self {
        case .titleLarge: return Font.dsTitle2xl // 36pt Fraunces medium
        case .titleMedium: return Font.dsTitleLg // 22pt Fraunces medium
        case .body: return Font.dsBody // 16pt DM Sans
        case .caption: return Font.dsCaptionXs // 12pt Geist Mono
        }
    }
}

public extension View {
    func appTypography(_ style: AppTypography) -> some View {
        font(style.font)
    }
}
