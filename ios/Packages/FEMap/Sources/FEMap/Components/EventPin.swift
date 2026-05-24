import SwiftUI
import FEData
import FEDesignSystem

/// Single-event pin. Coral when the event is free, neighborhood-green otherwise.
/// Matches the web map's "free first" visual hierarchy.
public struct EventPin: View {
    public let event: EventDTO
    public let isSelected: Bool

    public init(event: EventDTO, isSelected: Bool = false) {
        self.event = event
        self.isSelected = isSelected
    }

    private var tint: Color {
        event.isFree ? Color.dsAccentSecondary : Color.dsAccentPrimary
    }

    public var body: some View {
        ZStack {
            Circle()
                .fill(Color.dsSurface)
                .frame(width: isSelected ? 36 : 28, height: isSelected ? 36 : 28)
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
            Circle()
                .fill(tint)
                .frame(width: isSelected ? 26 : 18, height: isSelected ? 26 : 18)
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color.dsSurface)
            }
        }
        .animation(.spring(duration: 0.25), value: isSelected)
        .accessibilityLabel(Text("\(event.title), \(event.isFree ? "free" : "paid")"))
    }
}
