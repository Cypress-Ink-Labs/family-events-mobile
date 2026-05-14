import SwiftUI

public enum AppTypography: Sendable {
    case titleLarge
    case titleMedium
    case body
    case caption

    public var font: Font {
        switch self {
        case .titleLarge: return .largeTitle.weight(.bold)
        case .titleMedium: return .title3.weight(.semibold)
        case .body: return .body
        case .caption: return .caption
        }
    }
}

public extension View {
    func appTypography(_ style: AppTypography) -> some View {
        font(style.font)
    }
}
