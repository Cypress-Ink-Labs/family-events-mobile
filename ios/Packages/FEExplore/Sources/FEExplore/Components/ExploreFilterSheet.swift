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
