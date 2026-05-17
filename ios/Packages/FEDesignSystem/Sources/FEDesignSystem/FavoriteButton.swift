import SwiftUI

public struct FavoriteButton: View {
    @Binding public var isFavorited: Bool
    public let onToggle: () -> Void

    public init(isFavorited: Binding<Bool>, onToggle: @escaping () -> Void) {
        _isFavorited = isFavorited
        self.onToggle = onToggle
    }

    public var body: some View {
        Button(action: onToggle) {
            Image(systemName: isFavorited ? "heart.fill" : "heart")
                .font(.title3)
                .foregroundStyle(isFavorited ? Color.dsAccentSecondary : Color.dsTextMuted)
                .frame(minWidth: DesignTokens.Touch.min, minHeight: DesignTokens.Touch.min)
                .background(Circle().fill(Color.dsSurface.opacity(0.95)))
                .overlay(Circle().stroke(Color.dsBorder, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isFavorited ? "Unfavorite" : "Favorite")
    }
}
