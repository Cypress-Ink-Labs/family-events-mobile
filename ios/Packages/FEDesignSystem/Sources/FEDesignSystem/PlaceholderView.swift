import SwiftUI

public struct PlaceholderView: View {
    public let title: String
    public let systemImage: String

    public init(title: String, systemImage: String) {
        self.title = title
        self.systemImage = systemImage
    }

    public var body: some View {
        ContentUnavailableView(title, systemImage: systemImage)
    }
}

#if canImport(UIKit)
#Preview {
    PlaceholderView(title: "Plan", systemImage: "calendar.badge.clock")
}
#endif
