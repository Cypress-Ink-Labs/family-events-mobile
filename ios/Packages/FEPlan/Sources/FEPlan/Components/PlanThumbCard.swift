import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct PlanThumbCard: View {
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
            badge: nil,
            onTap: onTap
        )
    }

    /// Shared formatter per D7.
    static func subtitle(for event: EventDTO) -> String {
        DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime)
    }
}
