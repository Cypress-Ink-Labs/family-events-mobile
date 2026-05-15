import Foundation

public enum DateFormatting {
    public static let isoFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = TimeZone(identifier: "UTC")
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    public static let cardSubtitleFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE, MMM d · h:mm a"
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale.current
        return f
    }()

    public static func todayDateKey(in timeZone: TimeZone = .current, date: Date = Date()) -> String {
        let f = isoFormatter
        f.timeZone = timeZone
        return f.string(from: date)
    }

    public static func addDays(toDateKey key: String, days: Int, in timeZone: TimeZone = .current) -> String {
        let f = isoFormatter
        f.timeZone = timeZone
        guard let date = f.date(from: key) else { return key }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let shifted = cal.date(byAdding: .day, value: days, to: date) ?? date
        return f.string(from: shifted)
    }
}
