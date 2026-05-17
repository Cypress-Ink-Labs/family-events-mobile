import SwiftUI
import SwiftData
import FECore
import FEData
import FEDesignSystem

/// Forces inline nav title on iOS so the large-title transition doesn't
/// overlap the screen's own H1 mid-scroll. No-op on macOS where the
/// modifier isn't available.
private struct InlineNavTitle: ViewModifier {
    func body(content: Content) -> some View {
        #if os(iOS)
        content.navigationBarTitleDisplayMode(.inline)
        #else
        content
        #endif
    }
}

public struct SaturdayPlanScreen: View {
    @Bindable var viewModel: PlanViewModel
    public let context: PlanContext
    public let cityName: String?
    public let onSelectEvent: (EventID) -> Void
    public let onSetCity: () -> Void

    @Query(sort: \CachedPlannedEvent.rank) private var cachedPlan: [CachedPlannedEvent]
    @Query private var cachedEvents: [CachedEvent]

    public init(viewModel: PlanViewModel, context: PlanContext, cityName: String?, onSelectEvent: @escaping (EventID) -> Void, onSetCity: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.context = context
        self.cityName = cityName
        self.onSelectEvent = onSelectEvent
        self.onSetCity = onSetCity
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

                if let weather = viewModel.lastWeatherSnapshot {
                    WeatherStrip(snapshot: weather)
                }

                if viewModel.lastEmptyRefresh && !cachedPlan.isEmpty {
                    staleBanner
                }

                content
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
        }
        .scrollContentBackground(.hidden)
        .background(Color.dsBackground.ignoresSafeArea())
        .refreshable { await viewModel.refresh(context: context) }
        .task { await viewModel.refresh(context: context) }
        .navigationTitle("Plan")
        .modifier(InlineNavTitle())
    }

    @ViewBuilder
    private var header: some View {
        Text("This week's plan")
            .font(.dsCaption2xs)
            .textCase(.uppercase)
            .tracking(1.2)
            .foregroundStyle(Color.dsAccentSecondary)
        Text("Best family options this week")
            .font(.dsTitleXl)
            .foregroundStyle(Color.dsTextPrimary)
        Text(
            "A ranked shortlist from the next 7 days, tuned by distance, weather, age fit, and saved events."
        )
        .font(.dsBodySm)
        .foregroundStyle(Color.dsTextMuted)
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
        if context.cityID == nil && viewModel.lastEmptyRefresh {
            VStack(spacing: 16) {
                Image(systemName: "mappin.slash")
                    .font(.system(size: 48))
                    .foregroundStyle(.secondary)
                Text("No location set")
                    .appTypography(.titleMedium)
                Text("Pick a city to see family events nearby.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .appTypography(.body)
                Button("Set your city") { onSetCity() }
                    .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 48)
        } else {
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
}
