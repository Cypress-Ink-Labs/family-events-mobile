import SwiftUI
import FECore
import FEData
import FEDesignSystem

struct EventDetailHeroImage: View {
    let event: EventDTO

    var body: some View {
        Rectangle()
            .fill(Color.dsSurfaceRaised)
            .frame(maxWidth: .infinity)
            .frame(height: 260)
            .overlay {
                let resolvedURL = SafeImageURL.resolve(
                    images: event.images,
                    seed: event.id.rawValue,
                    aspect: .hero
                )
                AsyncImage(url: resolvedURL) { phase in
                    switch phase {
                    case .empty:
                        Color.clear
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "photo")
                            .font(.system(size: 48))
                            .foregroundStyle(Color.dsTextMuted)
                            .accessibilityHidden(true)
                    @unknown default:
                        Color.clear
                    }
                }
            }
            .clipped()
    }
}
