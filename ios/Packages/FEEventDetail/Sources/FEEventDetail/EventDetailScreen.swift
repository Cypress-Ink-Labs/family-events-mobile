import SwiftUI
import FECore
import FEDesignSystem

public struct EventDetailScreen: View {
    public let eventID: EventID

    public init(eventID: EventID) {
        self.eventID = eventID
    }

    public var body: some View {
        PlaceholderView(title: "Event \(eventID.rawValue)", systemImage: "calendar")
    }
}
