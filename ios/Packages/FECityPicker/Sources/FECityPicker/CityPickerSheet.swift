import SwiftUI
import FECore
import FEData
import FEDesignSystem

/// Searchable, sectioned city picker. Replaces the M3.5 stub. Selection
/// writes to `@AppStorage("selected-city-id")` AND calls the supplied
/// `onSelect` so callers can persist to the user profile.
public struct CityPickerSheet: View {
    @AppStorage("selected-city-id") private var storedCityID: String = ""
    @State private var viewModel: CityPickerViewModel
    @Environment(\.dismiss) private var dismiss

    private let currentSelection: CityID?
    private let onSelect: (CitySummary) -> Void

    public init(
        cityRepo: any CityRepository,
        currentSelection: CityID?,
        onSelect: @escaping (CitySummary) -> Void
    ) {
        self._viewModel = State(wrappedValue: CityPickerViewModel(cityRepo: cityRepo))
        self.currentSelection = currentSelection
        self.onSelect = onSelect
    }

    public var body: some View {
        NavigationStack {
            content
                .navigationTitle("Pick your city")
                #if canImport(UIKit)
                .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { dismiss() }
                    }
                }
                .searchable(text: $viewModel.searchText, prompt: "Search cities")
                .task { await viewModel.load() }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.loadState {
        case .idle, .loading:
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.dsBackground)
        case .failed(let message):
            errorView(message: message)
        case .loaded:
            if viewModel.filteredCities.isEmpty {
                emptyView
            } else {
                cityList
            }
        }
    }

    private var cityList: some View {
        List {
            ForEach(viewModel.sectionedCities, id: \.letter) { section in
                Section {
                    ForEach(section.cities) { city in
                        Button {
                            select(city)
                        } label: {
                            row(for: city)
                        }
                        .buttonStyle(.plain)
                    }
                } header: {
                    Text(section.letter)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.dsTextMuted)
                }
            }
        }
        .listStyle(.plain)
        .background(Color.dsBackground)
    }

    private func row(for city: CitySummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(city.name)
                    .font(.body)
                    .foregroundStyle(Color.dsTextPrimary)
                if let region = city.region {
                    Text(region)
                        .font(.caption)
                        .foregroundStyle(Color.dsTextMuted)
                }
            }
            Spacer()
            if currentSelection == city.id {
                Image(systemName: "checkmark")
                    .foregroundStyle(Color.dsAccentPrimary)
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }

    private var emptyView: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 40))
                .foregroundStyle(Color.dsTextMuted)
            Text("No matches")
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
            Text("Try a different search.")
                .foregroundStyle(Color.dsTextMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dsBackground)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 36))
                .foregroundStyle(Color.dsWarning)
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextPrimary)
            Button("Retry") {
                Task { await viewModel.load() }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.dsAccentPrimary)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.dsBackground)
    }

    private func select(_ city: CitySummary) {
        storedCityID = city.id.rawValue
        onSelect(city)
        dismiss()
    }
}
