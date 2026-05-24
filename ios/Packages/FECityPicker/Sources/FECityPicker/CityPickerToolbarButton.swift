import SwiftUI
import FECore
import FEData
import FEDesignSystem

/// Drop-in toolbar button that displays the current city name and opens
/// `CityPickerSheet` on tap. Use from any tab's `.toolbar` block.
///
/// ```swift
/// .toolbar {
///     ToolbarItem(placement: .topBarLeading) {
///         CityPickerToolbarButton(
///             cityRepo: cityRepo,
///             selection: selection,
///             onSelect: { city in ... }
///         )
///     }
/// }
/// ```
public struct CityPickerToolbarButton: View {
    public struct Selection: Equatable, Sendable {
        public let cityID: CityID?
        public let cityName: String?
        public init(cityID: CityID?, cityName: String?) {
            self.cityID = cityID
            self.cityName = cityName
        }
    }

    @State private var showSheet = false
    private let cityRepo: any CityRepository
    private let selection: Selection
    private let onSelect: (CitySummary) -> Void

    public init(
        cityRepo: any CityRepository,
        selection: Selection,
        onSelect: @escaping (CitySummary) -> Void
    ) {
        self.cityRepo = cityRepo
        self.selection = selection
        self.onSelect = onSelect
    }

    public var body: some View {
        Button { showSheet = true } label: {
            HStack(spacing: 4) {
                Image(systemName: "mappin.and.ellipse")
                    .imageScale(.small)
                Text(selection.cityName ?? "Pick city")
                    .lineLimit(1)
            }
            .foregroundStyle(Color.dsAccentPrimary)
        }
        .sheet(isPresented: $showSheet) {
            CityPickerSheet(
                cityRepo: cityRepo,
                currentSelection: selection.cityID,
                onSelect: onSelect
            )
        }
    }
}
