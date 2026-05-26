import SwiftUI
import FEDesignSystem

public struct ExploreActiveFiltersBar: View {
    @Binding public var filters: ExploreFilters

    public init(filters: Binding<ExploreFilters>) {
        _filters = filters
    }

    public var body: some View {
        if filters.activeCount > 0 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    if !filters.keyword.isEmpty {
                        chip(label: "\"\(filters.keyword)\"") {
                            filters.keyword = ""
                        }
                    }
                    if filters.dateFilter != .anytime {
                        chip(label: filters.dateFilter.rawValue) {
                            filters.dateFilter = .anytime
                        }
                    }
                    if filters.onlyFree {
                        chip(label: "Free") {
                            filters.onlyFree = false
                        }
                    }
                    if let af = filters.ageFilter {
                        chip(label: af.rawValue) {
                            filters.ageFilter = nil
                        }
                    }
                    if let slug = filters.activeCategory {
                        chip(label: categoryLabel(for: slug)) {
                            filters.activeCategory = nil
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
            }
        }
    }

    private func categoryLabel(for slug: String) -> String {
        switch slug {
        case "playgroup": return "Playgroups"
        case "music": return "Music & Movement"
        case "outdoor": return "Outdoor Fun"
        case "storytime": return "Indoor Storytime"
        default: return slug
        }
    }

    @ViewBuilder
    private func chip(label: String, onRemove: @escaping () -> Void) -> some View {
        HStack(spacing: 4) {
            Text(label)
                .font(.subheadline)
            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.caption.weight(.semibold))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.dsAccentPrimarySoft)
        .clipShape(Capsule())
    }
}
