import SwiftUI

public extension Color {
    static let appAccent = Color.accentColor

    #if canImport(UIKit)
    static let appBackground = Color(.systemBackground)
    static let appSecondaryBackground = Color(.secondarySystemBackground)
    static let appLabel = Color(.label)
    static let appSecondaryLabel = Color(.secondaryLabel)
    #else
    static let appBackground = Color(NSColor.windowBackgroundColor)
    static let appSecondaryBackground = Color(NSColor.controlBackgroundColor)
    static let appLabel = Color(NSColor.labelColor)
    static let appSecondaryLabel = Color(NSColor.secondaryLabelColor)
    #endif
}
