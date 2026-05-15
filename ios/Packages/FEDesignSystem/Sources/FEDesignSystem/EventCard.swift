import SwiftUI

public struct EventCard: View {
    public let title: String
    public let subtitle: String
    public let imageURL: URL?
    public let badge: String?
    public let onTap: (() -> Void)?

    public init(title: String, subtitle: String, imageURL: URL? = nil, badge: String? = nil, onTap: (() -> Void)? = nil) {
        self.title = title
        self.subtitle = subtitle
        self.imageURL = imageURL
        self.badge = badge
        self.onTap = onTap
    }

    public var body: some View {
        Button { onTap?() } label: {
            VStack(alignment: .leading, spacing: 8) {
                if let url = imageURL {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle().fill(Color.appSecondaryBackground)
                    }
                    .frame(height: 160)
                    .clipped()
                    .cornerRadius(8)
                }
                HStack(alignment: .top) {
                    Text(title).appTypography(.titleMedium).foregroundStyle(.primary)
                    Spacer()
                    if let badge {
                        Text(badge)
                            .appTypography(.caption)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(Color.appAccent.opacity(0.15))
                            .clipShape(Capsule())
                    }
                }
                Text(subtitle).appTypography(.body).foregroundStyle(.secondary)
            }
            .padding()
            .background(Color.appBackground)
            .cornerRadius(12)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.appSecondaryBackground, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
