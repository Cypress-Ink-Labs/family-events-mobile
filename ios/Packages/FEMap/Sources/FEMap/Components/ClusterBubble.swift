import SwiftUI
import FEDesignSystem

/// Annotation rendered when MapKit collapses many pins into one cluster.
public struct ClusterBubble: View {
    public let count: Int

    public init(count: Int) {
        self.count = count
    }

    public var body: some View {
        Text("\(count)")
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.dsSurface)
            .frame(minWidth: 32, minHeight: 32)
            .padding(.horizontal, 6)
            .background(Color.dsAccentPrimary, in: Capsule())
            .shadow(color: .black.opacity(0.18), radius: 3, x: 0, y: 1)
            .accessibilityLabel(Text("\(count) events"))
    }
}
