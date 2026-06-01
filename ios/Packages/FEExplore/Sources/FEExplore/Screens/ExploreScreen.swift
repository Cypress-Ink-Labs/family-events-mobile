import SwiftUI
import FECore
import FEData
import FEDesignSystem

@MainActor
public struct ExploreScreen: View {
    @Bindable var viewModel: ExploreViewModel
    public let onSelectEvent: (EventID) -> Void
    @State private var showFilters = false

    public init(viewModel: ExploreViewModel, onSelectEvent: @escaping (EventID) -> Void) {
        self.viewModel = viewModel
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                ExploreSearchBar(text: $viewModel.filters.keyword)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)

                ExploreCategoryChipRow(activeCategory: $viewModel.filters.activeCategory)

                ExploreActiveFiltersBar(filters: $viewModel.filters)
            }

            ScrollView {
                listContent
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 12)
            }
            .scrollBounceBehavior(.always)
            .refreshable {
                await viewModel.reload()
            }
        }
        .navigationTitle("Explore")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                ExploreFilterButton(activeCount: viewModel.filters.activeCount, action: { showFilters = true })
            }
        }
        .sheet(isPresented: $showFilters) {
            ExploreFilterSheet(filters: $viewModel.filters, onDismiss: { showFilters = false })
        }
        .task {
            await viewModel.loadIfNeeded()
        }
    }

    @ViewBuilder
    private var listContent: some View {
        if viewModel.isLoading && viewModel.events.isEmpty {
            loadingState
        } else if let error = viewModel.errorMessage, viewModel.events.isEmpty {
            errorState(error)
        } else if viewModel.events.isEmpty {
            emptyState
        } else {
            eventList
        }
    }

    @ViewBuilder
    private var loadingState: some View {
        ProgressView()
            .frame(maxWidth: .infinity)
            .padding(.top, 48)
    }

    @ViewBuilder
    private func errorState(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 36))
                .foregroundStyle(.orange)
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
                .padding(.horizontal, 24)
            Button("Retry") { Task { await viewModel.reload() } }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundStyle(Color.dsTextMuted)
            Text("No events match these filters.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
                .padding(.horizontal, 24)
            if viewModel.filters.activeCount > 0 {
                Button("Clear filters") { viewModel.filters = ExploreFilters() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }

    @ViewBuilder
    private var eventList: some View {
        LazyVStack(spacing: 12) {
            ForEach(viewModel.events) { event in
                ExploreCard(event: event, onTap: { onSelectEvent(event.id) })
                    .padding(.horizontal, 16)
                    .onAppear {
                        if event.id == viewModel.events.suffix(3).first?.id {
                            Task { await viewModel.loadNextPage() }
                        }
                    }
            }
            if viewModel.isLoadingMore {
                ProgressView().padding(.vertical, 16)
            }
        }
    }
}
