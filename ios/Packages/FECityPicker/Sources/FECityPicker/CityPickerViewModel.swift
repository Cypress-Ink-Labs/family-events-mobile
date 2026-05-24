import Foundation
import Observation
import FECore
import FEData

@MainActor
@Observable
public final class CityPickerViewModel {
    public enum LoadState: Equatable, Sendable {
        case idle
        case loading
        case loaded
        case failed(String)
    }

    public private(set) var allCities: [CitySummary] = []
    public private(set) var loadState: LoadState = .idle
    public var searchText: String = ""

    private let cityRepo: any CityRepository

    public init(cityRepo: any CityRepository) {
        self.cityRepo = cityRepo
    }

    public func load() async {
        loadState = .loading
        do {
            let result = try await cityRepo.cities()
            allCities = result
            loadState = .loaded
        } catch {
            loadState = .failed((error as? AppError)?.userMessage ?? "Couldn't load cities.")
        }
    }

    /// Filtered + sorted view of `allCities` based on the current
    /// `searchText`. Matching is case-insensitive against name and region.
    public var filteredCities: [CitySummary] {
        let trimmed = searchText.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return allCities }
        let needle = trimmed.lowercased()
        return allCities.filter { city in
            if city.name.lowercased().contains(needle) { return true }
            if let region = city.region, region.lowercased().contains(needle) { return true }
            return false
        }
    }

    /// Cities grouped by leading letter for SwiftUI's `Section` index.
    /// Letters are uppercased; non-letters bucket into "#".
    public var sectionedCities: [(letter: String, cities: [CitySummary])] {
        let groups = Dictionary(grouping: filteredCities) { city -> String in
            guard let first = city.name.first else { return "#" }
            let upper = String(first).uppercased()
            return upper.range(of: "[A-Z]", options: .regularExpression) != nil ? upper : "#"
        }
        return groups
            .map { (letter: $0.key, cities: $0.value.sorted { $0.name < $1.name }) }
            .sorted { $0.letter < $1.letter }
    }
}
