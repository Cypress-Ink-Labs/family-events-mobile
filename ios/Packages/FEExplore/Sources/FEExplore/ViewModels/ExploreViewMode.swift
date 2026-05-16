import SwiftUI

// MARK: - ExploreViewMode

public enum ExploreViewMode: String, Sendable {
    case list
    case map
}

// MARK: - ExploreViewModeToggle

public struct ExploreViewModeToggle: View {
    @Binding public var mode: ExploreViewMode

    public init(mode: Binding<ExploreViewMode>) { _mode = mode }

    public var body: some View {
        Picker("View mode", selection: $mode) {
            Image(systemName: "list.bullet").tag(ExploreViewMode.list)
            Image(systemName: "map").tag(ExploreViewMode.map)
        }
        .pickerStyle(.segmented)
        .frame(width: 120)
    }
}
