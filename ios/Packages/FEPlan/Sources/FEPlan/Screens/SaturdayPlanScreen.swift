import SwiftUI
import SwiftData
import FECore
import FEData
import FEDesignSystem

@MainActor
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
        cachedPlan.dropFirst().prefix(6).compactMap { row in
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
        .scrollBounceBehavior(.always)
        .refreshable { await viewModel.forceRefresh(context: context) }
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
                .font(.dsCaptionXs)
            Spacer()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.dsSurfaceRaised)
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
                alsoThisWeek
            }
        } else {
            emptyView
        }
    }

    @ViewBuilder
    private var alsoThisWeek: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Also this week")
                .font(.dsCaption2xs)
                .textCase(.uppercase)
                .tracking(1.2)
                .foregroundStyle(Color.dsAccentSecondary)
            ScrollView(.horizontal) {
                HStack(alignment: .top, spacing: 12) {
                    ForEach(secondaryEvents, id: \.id) { event in
                        PlanCarouselCard(event: event, onTap: { onSelectEvent(event.id) })
                    }
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            .padding(.horizontal, -16)
        }
    }

    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 12) {
            Text(message).foregroundStyle(.red).font(.dsBody)
            Button("Retry") { Task { await viewModel.refresh(context: context) } }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding()
    }

    @ViewBuilder
    private var emptyView: some View {
        PlanEmptyStateView(
            hasCitySet: context.cityID != nil,
            lastEmptyRefresh: viewModel.lastEmptyRefresh,
            onSetCity: onSetCity
        )
    }
}
