import SwiftUI
import FEDesignSystem

/// Six-row × seven-column grid for the displayed month. Cells outside the
/// month are dimmed; tap selects a day.
public struct CalendarMonthGrid: View {
    public let month: Date
    public let selectedDate: Date
    public let calendar: Calendar
    public let hasEvents: (Date) -> Bool
    public let onSelect: (Date) -> Void

    public init(
        month: Date,
        selectedDate: Date,
        calendar: Calendar = .current,
        hasEvents: @escaping (Date) -> Bool,
        onSelect: @escaping (Date) -> Void
    ) {
        self.month = month
        self.selectedDate = selectedDate
        self.calendar = calendar
        self.hasEvents = hasEvents
        self.onSelect = onSelect
    }

    public var body: some View {
        VStack(spacing: 8) {
            weekdayHeader
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
                ForEach(days, id: \.self) { date in
                    dayCell(for: date)
                }
            }
        }
    }

    private var weekdayHeader: some View {
        HStack {
            ForEach(calendar.veryShortWeekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .font(.caption.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(Color.dsTextMuted)
            }
        }
    }

    private var days: [Date] {
        let monthStart = calendar.startOfMonth(for: month)
        let monthInterval = calendar.dateInterval(of: .month, for: monthStart) ?? DateInterval(start: monthStart, duration: 0)
        let firstWeekday = calendar.component(.weekday, from: monthInterval.start)
        let leadingBlanks = (firstWeekday - calendar.firstWeekday + 7) % 7
        let gridStart = calendar.date(byAdding: .day, value: -leadingBlanks, to: monthInterval.start) ?? monthInterval.start
        return (0..<42).compactMap { offset in
            calendar.date(byAdding: .day, value: offset, to: gridStart)
        }
    }

    private func dayCell(for date: Date) -> some View {
        let inMonth = calendar.isDate(date, equalTo: month, toGranularity: .month)
        let isSelected = calendar.isDate(date, inSameDayAs: selectedDate)
        let isToday = calendar.isDateInToday(date)
        let dayNum = calendar.component(.day, from: date)

        return Button {
            onSelect(date)
        } label: {
            VStack(spacing: 2) {
                Text("\(dayNum)")
                    .font(.callout.weight(isSelected ? .semibold : .regular))
                    .foregroundStyle(textColor(inMonth: inMonth, isSelected: isSelected, isToday: isToday))
                Circle()
                    .fill(hasEvents(date) ? Color.dsAccentSecondary : Color.clear)
                    .frame(width: 4, height: 4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .background(isSelected ? Color.dsAccentPrimary : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .disabled(!inMonth)
    }

    private func textColor(inMonth: Bool, isSelected: Bool, isToday: Bool) -> Color {
        if isSelected { return Color.dsSurface }
        if !inMonth { return Color.dsTextMuted.opacity(0.4) }
        if isToday { return Color.dsAccentPrimary }
        return Color.dsTextPrimary
    }
}

