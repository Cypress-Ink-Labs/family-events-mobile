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
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(Color.dsSurfaceRaised)
                    }
                    .frame(height: 160)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.md))
                }
                HStack(alignment: .top) {
                    Text(title)
                        .font(.dsTitleLg)
                        .foregroundStyle(Color.dsTextPrimary)
                    Spacer()
                    if let badge {
                        Text(badge)
                            .font(.dsCaptionXs)
                            .foregroundStyle(Color.dsAccentPrimary)
                            .padding(.horizontal, DesignTokens.Space.s2)
                            .padding(.vertical, DesignTokens.Space.s1)
                            .background(Color.dsAccentPrimarySoft)
                            .clipShape(Capsule())
                    }
                }
                Text(subtitle)
                    .font(.dsBodySm)
                    .foregroundStyle(Color.dsTextMuted)
            }
            .padding(DesignTokens.Space.s4)
            .background(Color.dsSurface)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.md))
            .overlay(
                RoundedRectangle(cornerRadius: DesignTokens.Radius.md)
                    .stroke(Color.dsBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
