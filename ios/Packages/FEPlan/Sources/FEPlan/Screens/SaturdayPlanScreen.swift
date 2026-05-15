import SwiftUI
import SwiftData
import FECore
import FEData
import FEDesignSystem

public struct SaturdayPlanScreen: View {
    @Bindable var viewModel: PlanViewModel
    public let context: PlanContext
    public let cityName: String?
    public let onSelectEvent: (EventID) -> Void

    @Query(sort: \CachedPlannedEvent.rank) private var cachedPlan: [CachedPlannedEvent]
    @Query private var cachedEvents: [CachedEvent]

    public init(viewModel: PlanViewModel, context: PlanContext, cityName: String?, onSelectEvent: @escaping (EventID) -> Void) {
        self.viewModel = viewModel
        self.context = context
        self.cityName = cityName
        self.onSelectEvent = onSelectEvent
    }

    private var eventsByID: [String: CachedEvent] {
        Dictionary(uniqueKeysWithValues: cachedEvents.map { ($0.id, $0) })
    }

    private var heroEvent: EventDTO? {
        guard let first = cachedPlan.first, let cached = eventsByID[first.eventID] else { return nil }
        return cached.asEventDTO()
    }

    private var secondaryEvents: [EventDTO] {
        cachedPlan.dropFirst().prefix(2).compactMap { row in
            eventsByID[row.eventID]?.asEventDTO()
        }
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                PlanContextBar(cityName: cityName, kidAge: context.kidAge)

                if viewModel.lastEmptyRefresh && !cachedPlan.isEmpty {
                    staleBanner
                }

                content
            }
            .padding()
        }
        .refreshable { await viewModel.refresh(context: context) }
        .task { await viewModel.refresh(context: context) }
        .navigationTitle("Plan")
    }

    @ViewBuilder
    private var header: some View {
        Text("This week's plan")
            .appTypography(.caption)
            .foregroundStyle(.tint)
        Text("Best family options this week")
            .appTypography(.titleLarge)
    }

    @ViewBuilder
    private var staleBanner: some View {
        HStack(spacing: 8) {
            Image(systemName: "clock.arrow.circlepath")
            Text("No new plan today — showing yesterday's events.")
                .appTypography(.caption)
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.appSecondaryBackground)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && heroEvent == nil {
            ProgressView().frame(maxWidth: .infinity).padding(.top, 48)
        } else if let err = viewModel.errorMessage, heroEvent == nil {
            errorView(err)
        } else if let hero = heroEvent {
            PlanHeroCard(event: hero, onTap: { onSelectEvent(hero.id) })
            if !secondaryEvents.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(secondaryEvents, id: \.id) { event in
                        PlanThumbCard(event: event, onTap: { onSelectEvent(event.id) })
                    }
                }
            }
        } else {
            emptyView
        }
    }

    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 12) {
            Text(message).foregroundStyle(.red).appTypography(.body)
            Button("Retry") { Task { await viewModel.refresh(context: context) } }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding()
    }

    @ViewBuilder
    private var emptyView: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No family plans found nearby in the next 7 days.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .appTypography(.body)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }
}
