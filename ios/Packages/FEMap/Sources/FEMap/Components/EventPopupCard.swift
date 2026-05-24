import SwiftUI
import FECore
import FEData
import FEDesignSystem

/// Bottom-sheet card shown after tapping a pin. Tapping anywhere opens the
/// full Event Detail screen via the supplied callback.
public struct EventPopupCard: View {
    public let event: EventDTO
    public let onOpenDetail: () -> Void

    public init(event: EventDTO, onOpenDetail: @escaping () -> Void) {
        self.event = event
        self.onOpenDetail = onOpenDetail
    }

    public var body: some View {
        Button(action: onOpenDetail) {
            VStack(alignment: .leading, spacing: 10) {
                Text(event.title)
                    .font(.headline)
                    .foregroundStyle(Color.dsTextPrimary)
                    .multilineTextAlignment(.leading)

                if let venue = event.venueName, !venue.isEmpty {
                    Label(venue, systemImage: "mappin")
                        .font(.subheadline)
                        .foregroundStyle(Color.dsTextMuted)
                }

                HStack(spacing: 8) {
                    if event.isFree {
                        Text("Free")
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.dsAccentSecondarySoft)
                            .foregroundStyle(Color.dsAccentSecondary)
                            .clipShape(Capsule())
                    }
                    Text(DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime))
                        .font(.caption)
                        .foregroundStyle(Color.dsTextMuted)
                }

                Text("View details →")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.dsAccentPrimary)
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.dsSurfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}
