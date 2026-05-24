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
        didSet {
            if filters != oldValue {
                filterReloadTask?.cancel()
                filterReloadTask = Task { [weak self] in await self?.reloadFromFilter() }
            }
        }
    }

    private let eventRepo: any EventRepository
    private let userID: UserID
    private let cityID: CityID?
    private let pageSize = 20
    private var loadedPages = 0
    private var generation = 0
    private var filterReloadTask: Task<Void, Never>?

    public init(eventRepo: any EventRepository, userID: UserID, cityID: CityID?) {
        self.eventRepo = eventRepo
        self.userID = userID
        self.cityID = cityID
    }

    public func reload() async {
        filterReloadTask?.cancel()
        filterReloadTask = nil
        await reloadInternal()
    }

    private func reloadFromFilter() async {
        guard !Task.isCancelled else { return }
        await reloadInternal()
    }

    private func reloadInternal() async {
        generation += 1
        let requestGeneration = generation
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        loadedPages = 0
        hasMore = true
        await loadNextPage(generation: requestGeneration, bypassInFlight: true, replaceEvents: true)
    }

    public func loadNextPage() async {
        await loadNextPage(generation: generation, bypassInFlight: false, replaceEvents: false)
    }

    private func loadNextPage(generation requestGeneration: Int, bypassInFlight: Bool, replaceEvents: Bool) async {
        guard (bypassInFlight || !isLoadingMore), hasMore else { return }
        isLoadingMore = true
        defer {
            if generation == requestGeneration {
                isLoadingMore = false
            }
        }
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
            guard generation == requestGeneration else { return }
            let filtered = applyClientFilters(fetched)
            if replaceEvents {
                events = filtered
            } else {
                events.append(contentsOf: filtered)
            }
            loadedPages += 1
            if fetched.count < pageSize { hasMore = false }
        } catch is CancellationError {
            guard generation == requestGeneration else { return }
        } catch where error.isCancellation {
            guard generation == requestGeneration else { return }
        } catch let app as AppError {
            guard generation == requestGeneration else { return }
            errorMessage = app.userMessage
        } catch {
            guard generation == requestGeneration else { return }
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

private extension Error {
    var isCancellation: Bool {
        if let urlError = self as? URLError, urlError.code == .cancelled {
            return true
        }
        let nsError = self as NSError
        return nsError.domain == NSURLErrorDomain && nsError.code == NSURLErrorCancelled
    }
}
