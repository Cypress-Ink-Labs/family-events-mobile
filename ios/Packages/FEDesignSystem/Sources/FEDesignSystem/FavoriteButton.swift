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
                .foregroundStyle(isFavorited ? Color.pink : Color.secondary)
                .padding(8)
                .background(Circle().fill(Color.appBackground.opacity(0.95)))
                .overlay(Circle().stroke(Color.appSecondaryBackground, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isFavorited ? "Unfavorite" : "Favorite")
    }
}
