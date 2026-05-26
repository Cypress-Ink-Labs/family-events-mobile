import SwiftUI

public struct ExploreFilterSheet: View {
    @Binding public var filters: ExploreFilters
    public let onDismiss: () -> Void

    public init(filters: Binding<ExploreFilters>, onDismiss: @escaping () -> Void) {
        _filters = filters
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationStack {
            Form {
                Section("Date") {
                    Picker("Date", selection: $filters.dateFilter) {
                        ForEach(ExploreFilters.DateFilter.allCases, id: \.self) { option in
                            Text(option.rawValue).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section("Price") {
                    Toggle("Free events only", isOn: $filters.onlyFree)
                }

                Section("Age") {
                    Picker("Age", selection: $filters.ageFilter) {
                        Text("Any age").tag(Optional<ExploreFilters.AgeFilter>.none)
                        ForEach(ExploreFilters.AgeFilter.allCases, id: \.self) { af in
                            Text(af.rawValue).tag(Optional(af))
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section("Category") {
                    ForEach(
                        [("Playgroups", "playgroup"),
                         ("Music & Movement", "music"),
                         ("Outdoor Fun", "outdoor"),
                         ("Indoor Storytime", "storytime")],
                        id: \.1
                    ) { label, slug in
                        Button {
                            filters.activeCategory = (filters.activeCategory == slug) ? nil : slug
                        } label: {
                            HStack {
                                Text(label)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if filters.activeCategory == slug {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle("Filters")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        filters = ExploreFilters()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}
