import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct ExploreCard: View {
    public let event: EventDTO
    public let onTap: () -> Void

    public init(event: EventDTO, onTap: @escaping () -> Void) {
        self.event = event
        self.onTap = onTap
    }

    public var body: some View {
        EventCard(
            title: event.title,
            subtitle: Self.subtitle(for: event),
            imageURL: event.images.first.flatMap(URL.init(string:)),
            badge: event.isFree ? "Free" : nil,
            onTap: onTap
        )
    }

    static func subtitle(for event: EventDTO) -> String {
        let base = DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime)
        if let venue = event.venueName, !venue.isEmpty {
            return "\(base) · \(venue)"
        }
        return base
    }
}
