import SwiftUI
import FECore
import FEData
import FEDesignSystem

/// Single-day list rendered below the month grid.
public struct CalendarDayList: View {
    public let date: Date
    public let events: [EventDTO]
    public let onSelectEvent: (EventID) -> Void

    public init(date: Date, events: [EventDTO], onSelectEvent: @escaping (EventID) -> Void) {
        self.date = date
        self.events = events
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(date, style: .date)
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
                .padding(.horizontal, 16)
                .padding(.top, 8)

            if events.isEmpty {
                emptyState
            } else {
                ForEach(events) { event in
                    Button {
                        onSelectEvent(event.id)
                    } label: {
                        row(for: event)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func row(for event: EventDTO) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(event.startDatetime, style: .time)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.dsAccentPrimary)
                Text(event.title)
                    .font(.body)
                    .foregroundStyle(Color.dsTextPrimary)
                    .multilineTextAlignment(.leading)
                if let venue = event.venueName, !venue.isEmpty {
                    Text(venue)
                        .font(.caption)
                        .foregroundStyle(Color.dsTextMuted)
                }
            }
            Spacer()
            if event.isFree {
                Text("Free")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.dsAccentSecondarySoft)
                    .foregroundStyle(Color.dsAccentSecondary)
                    .clipShape(Capsule())
            }
            Image(systemName: "chevron.right")
                .imageScale(.small)
                .foregroundStyle(Color.dsTextMuted)
        }
        .padding()
        .background(Color.dsSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 32))
                .foregroundStyle(Color.dsTextMuted)
            Text("No events on this day.")
                .foregroundStyle(Color.dsTextMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}
