import SwiftUI

public struct EventCard: View {
    public let title: String
    public let subtitle: String
    public let imageURL: URL?
    public let badge: String?
    public let onTap: (() -> Void)?

    public init(
        title: String,
        subtitle: String,
        imageURL: URL? = nil,
        badge: String? = nil,
        onTap: (() -> Void)? = nil
    ) {
        self.title = title
        self.subtitle = subtitle
        self.imageURL = imageURL
        self.badge = badge
        self.onTap = onTap
    }

    public var body: some View {
        Button { onTap?() } label: {
            VStack(alignment: .leading, spacing: DesignTokens.Space.s2) {
                if let url = imageURL {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            Rectangle().fill(Color.dsSurfaceRaised)
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        case .failure:
                            // AsyncImage swallows errors silently by default.
                            // Surface a token-coloured fallback so cards
                            // still read as cards (vs. an empty rectangle
                            // that looks like a layout bug).
                            ZStack {
                                Rectangle().fill(Color.dsSurfaceRaised)
                                Image(systemName: "photo")
                                    .font(.system(size: 28))
                                    .foregroundStyle(Color.dsTextMuted)
                            }
                        @unknown default:
                            Rectangle().fill(Color.dsSurfaceRaised)
                        }
                    }
                    // maxWidth: .infinity locks the image to the parent
                    // VStack's available width. Without it,
                    // .aspectRatio(.fill) reports the image's intrinsic
                    // pixel width as the preferred size and overflows
                    // the LazyVGrid cell — causing the secondary thumb
                    // cards to overlap each other on iPhone.
                    .frame(maxWidth: .infinity)
                    .frame(height: 160)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.md))
                }
                HStack(alignment: .top) {
                    Text(title)
                        .font(.dsTitleLg)
                        .foregroundStyle(Color.dsTextPrimary)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                    if let badge {
                        Text(badge)
                            .font(.dsCaptionXs)
                            .foregroundStyle(Color.dsAccentPrimary)
                            .padding(.horizontal, DesignTokens.Space.s2)
                            .padding(.vertical, DesignTokens.Space.s1)
                            .background(Color.dsAccentPrimarySoft)
                            .clipShape(Capsule())
                            .layoutPriority(1)
                    }
                }
                Text(subtitle)
                    .font(.dsBodySm)
                    .foregroundStyle(Color.dsTextMuted)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DesignTokens.Space.s4)
            .background(Color.dsSurface)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.md))
            .overlay(
                RoundedRectangle(cornerRadius: DesignTokens.Radius.md)
                    .stroke(Color.dsBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        // SwiftUI Buttons inherit their label's intrinsic size. Without this
        // outer frame, AsyncImage.aspectRatio(.fill) reports the source
        // image's native pixel width upward through the VStack and the
        // Button, and LazyVGrid(.flexible()) cells accept that oversized
        // child instead of clipping — producing the overlapping-cards bug.
        // maxWidth: .infinity locks the Button to the grid cell it's
        // placed into.
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
