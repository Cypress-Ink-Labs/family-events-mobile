import Foundation
import FECore
import FEData

@Observable
@MainActor
public final class ExploreViewModel {
    public private(set) var isLoading = false
    public private(set) var isLoadingMore = false
    public private(set) var errorMessage: String?
    public private(set) var events: [EventDTO] = []
    public private(set) var hasMore = true
    public var filters = ExploreFilters() {
        didSet { if filters != oldValue { Task { await reload() } } }
    }

    private let eventRepo: any EventRepository
    private let userID: UserID
    private let cityID: CityID?
    private let pageSize = 20
    private var loadedPages = 0

    public init(eventRepo: any EventRepository, userID: UserID, cityID: CityID?) {
        self.eventRepo = eventRepo
        self.userID = userID
        self.cityID = cityID
    }

    public func reload() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        loadedPages = 0
        events = []
        hasMore = true
        await loadNextPage()
    }

    public func loadNextPage() async {
        guard !isLoadingMore, hasMore else { return }
        isLoadingMore = true
        defer { isLoadingMore = false }
        let range = filters.dateRange
        let query = EventQuery(
            cityID: cityID,
            dateFrom: range.from,
            dateTo: range.to,
            limit: pageSize,
            offset: loadedPages * pageSize
        )
        do {
            let fetched = try await eventRepo.fetchList(query: query, for: userID)
            events.append(contentsOf: applyClientFilters(fetched))
            loadedPages += 1
            if fetched.count < pageSize { hasMore = false }
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    private func applyClientFilters(_ input: [EventDTO]) -> [EventDTO] {
        let keyword = filters.keyword.lowercased().trimmingCharacters(in: .whitespaces)
        return input.filter { event in
            if filters.onlyFree && !event.isFree { return false }
            if !keyword.isEmpty {
                let haystack = "\(event.title) \(event.description ?? "") \(event.venueName ?? "")".lowercased()
                if !haystack.contains(keyword) { return false }
            }
            return true
        }
    }
}
