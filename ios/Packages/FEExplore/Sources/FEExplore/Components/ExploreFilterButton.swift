import SwiftUI
import FEDesignSystem

public struct ExploreFilterButton: View {
    public let activeCount: Int
    public let action: () -> Void

    public init(activeCount: Int, action: @escaping () -> Void) {
        self.activeCount = activeCount
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Label {
                Text("Filter")
            } icon: {
                Image(systemName: "line.3.horizontal.decrease.circle")
            }
            .overlay(alignment: .topTrailing) {
                if activeCount > 0 {
                    Text("\(activeCount)")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color.dsSurface)
                        .padding(4)
                        .background(Color.dsAccentPrimary)
                        .clipShape(Circle())
                        .offset(x: 6, y: -6)
                }
            }
        }
        .accessibilityLabel(activeCount > 0 ? "Filter, \(activeCount) active" : "Filter")
    }
}
