import SwiftUI
import FECore
import FEData
import FEDesignSystem

@MainActor
public struct CalendarScreen: View {
    @Bindable var viewModel: CalendarViewModel
    public let onSelectEvent: (EventID) -> Void

    public init(viewModel: CalendarViewModel, onSelectEvent: @escaping (EventID) -> Void) {
        self.viewModel = viewModel
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                monthHeader
                CalendarMonthGrid(
                    month: viewModel.displayedMonth,
                    selectedDate: viewModel.selectedDate,
                    hasEvents: { date in viewModel.hasEvents(on: date) },
                    onSelect: { date in viewModel.selectedDate = date }
                )
                .padding(.horizontal, 16)

                if let message = viewModel.errorMessage {
                    errorBar(message)
                }

                Divider().padding(.vertical, 8)

                CalendarDayList(
                    date: viewModel.selectedDate,
                    events: viewModel.events(on: viewModel.selectedDate),
                    onSelectEvent: onSelectEvent
                )
            }
            .padding(.vertical, 16)
        }
        .navigationTitle("Calendar")
        .refreshable { await viewModel.refresh() }
        .task { await viewModel.loadIfNeeded() }
        .overlay {
            if viewModel.isLoading && viewModel.eventsByDay.isEmpty {
                ProgressView()
                    .controlSize(.large)
            }
        }
    }

    private var monthHeader: some View {
        HStack {
            Button {
                Task { await viewModel.moveMonth(by: -1) }
            } label: {
                Image(systemName: "chevron.left")
            }
            Spacer()
            Text(monthTitle)
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
            Spacer()
            Button {
                Task { await viewModel.moveMonth(by: 1) }
            } label: {
                Image(systemName: "chevron.right")
            }
        }
        .padding(.horizontal, 16)
    }

    private var monthTitle: String {
        let formatter = DateFormatter()
        formatter.setLocalizedDateFormatFromTemplate("MMMM y")
        return formatter.string(from: viewModel.displayedMonth)
    }

    private func errorBar(_ message: String) -> some View {
        HStack {
            Image(systemName: "exclamationmark.triangle")
                .foregroundStyle(Color.dsWarning)
            Text(message)
                .font(.caption)
                .foregroundStyle(Color.dsTextPrimary)
            Spacer()
            Button("Retry") {
                Task { await viewModel.refresh() }
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .background(Color.dsSurfaceRaised, in: RoundedRectangle(cornerRadius: 8))
        .padding(.horizontal, 16)
    }
}
