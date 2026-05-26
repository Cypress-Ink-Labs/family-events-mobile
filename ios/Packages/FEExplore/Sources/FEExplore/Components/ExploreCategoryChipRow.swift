import SwiftUI
import FEDesignSystem

public struct ExploreCategoryChipRow: View {
    @Binding public var activeCategory: String?

    public init(activeCategory: Binding<String?>) {
        _activeCategory = activeCategory
    }

    private let categories: [(label: String, slug: String)] = [
        ("Playgroups", "playgroup"),
        ("Music & Movement", "music"),
        ("Outdoor Fun", "outdoor"),
        ("Indoor Storytime", "storytime")
    ]

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories, id: \.slug) { cat in
                    Button {
                        activeCategory = (activeCategory == cat.slug) ? nil : cat.slug
                    } label: {
                        Text(cat.label)
                            .font(.subheadline)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(activeCategory == cat.slug ? Color.dsAccentPrimarySoft : Color.secondary.opacity(0.1))
                            .foregroundStyle(activeCategory == cat.slug ? Color.dsAccentPrimary : Color.primary)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }
}
