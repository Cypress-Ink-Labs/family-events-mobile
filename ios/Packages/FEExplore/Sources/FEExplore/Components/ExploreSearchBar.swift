import SwiftUI
import FEDesignSystem

public struct ExploreSearchBar: View {
    @Binding public var text: String

    public init(text: Binding<String>) {
        _text = text
    }

    public var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            field
            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.dsSurfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private var field: some View {
        let base = TextField("Search events…", text: $text)
            .autocorrectionDisabled()
        #if os(iOS)
        base.textInputAutocapitalization(.never)
        #else
        base
        #endif
    }
}
