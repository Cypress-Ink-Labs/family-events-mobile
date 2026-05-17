import SwiftUI
import FECore
import FEData
import FEDesignSystem

/// Compact, fixed-width card for the horizontal "Also this week" strip
/// under the hero on the Plan screen. Locks width to a phone-friendly
/// 220pt so cards never inherit AsyncImage's intrinsic pixel size — the
/// same bug that bit `EventCard` inside `LazyVGrid` cells.
public struct PlanCarouselCard: View {
    public let event: EventDTO
    public let onTap: () -> Void

    private static let cardWidth: CGFloat = 220
    private static let imageHeight: CGFloat = 130
    private static let pad: CGFloat = 12
    private static let radius: CGFloat = 8
    private static let imageRadius: CGFloat = 6

    public init(event: EventDTO, onTap: @escaping () -> Void) {
        self.event = event
        self.onTap = onTap
    }

    public var body: some View {
        Button {
            onTap()
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                imageBlock
                eyebrow
                Text(event.title)
                    .font(.dsBody)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.dsTextPrimary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                if let venue = event.venueName?.nilIfEmpty {
                    Text(venue)
                        .font(.dsBodySm)
                        .foregroundStyle(Color.dsTextMuted)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            }
            .padding(Self.pad)
            .frame(width: Self.cardWidth, alignment: .leading)
            .background(Color.dsSurface)
            .clipShape(RoundedRectangle(cornerRadius: Self.radius))
            .overlay(
                RoundedRectangle(cornerRadius: Self.radius)
                    .stroke(Color.dsBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var imageBlock: some View {
        ZStack(alignment: .topTrailing) {
            if let url = event.images.first.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(Color.dsSurfaceRaised)
                }
                .frame(width: Self.cardWidth - 2 * Self.pad, height: Self.imageHeight)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: Self.imageRadius))
            } else {
                Rectangle()
                    .fill(Color.dsSurfaceRaised)
                    .frame(width: Self.cardWidth - 2 * Self.pad, height: Self.imageHeight)
                    .clipShape(RoundedRectangle(cornerRadius: Self.imageRadius))
            }
            if event.isFree {
                Text("Free")
                    .font(.dsCaptionXs)
                    .foregroundStyle(Color.dsAccentPrimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.dsAccentPrimarySoft)
                    .clipShape(Capsule())
                    .padding(8)
            }
        }
    }

    @ViewBuilder
    private var eyebrow: some View {
        Text(Self.eyebrowText(for: event))
            .font(.dsCaption2xs)
            .textCase(.uppercase)
            .tracking(1.0)
            .foregroundStyle(Color.dsAccentSecondary)
            .lineLimit(1)
    }

    static func eyebrowText(for event: EventDTO) -> String {
        DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime)
    }
}

private extension String {
    var nilIfEmpty: String? {
        trimmingCharacters(in: .whitespaces).isEmpty ? nil : self
    }
}
