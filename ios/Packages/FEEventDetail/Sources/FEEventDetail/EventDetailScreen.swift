import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct EventDetailScreen: View {
    @State private var viewModel: EventDetailViewModel
    private let eventID: EventID

    public init(
        eventID: EventID,
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        userID: UserID
    ) {
        self.eventID = eventID
        _viewModel = State(initialValue: EventDetailViewModel(
            eventRepo: eventRepo,
            favoriteRepo: favoriteRepo,
            userID: userID,
            eventID: eventID
        ))
    }

    public var body: some View {
        Group {
            if viewModel.isLoading && viewModel.event == nil {
                loadingState
            } else if let err = viewModel.errorMessage, viewModel.event == nil {
                errorState(err)
            } else if let event = viewModel.event {
                content(for: event)
            }
        }
        .task { await viewModel.load() }
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        #endif
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { viewModel.toggleFavorite() }) {
                    Image(systemName: viewModel.isFavorited ? "heart.fill" : "heart")
                        .foregroundStyle(viewModel.isFavorited ? Color.pink : Color.primary)
                }
                .accessibilityLabel(viewModel.isFavorited ? "Unfavorite" : "Favorite")
            }
        }
    }

    @ViewBuilder
    private var loadingState: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func errorState(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(.orange)
            Text("Couldn't load this event")
                .font(.title3.weight(.semibold))
            Text(message)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Button("Retry") { Task { await viewModel.load() } }
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func content(for event: EventDTO) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                heroImage(event: event)

                VStack(alignment: .leading, spacing: 24) {
                    titleBlock(event: event)
                    if !event.tags.isEmpty { tagRow(event: event) }
                    infoGrid(event: event)
                    if let description = event.description, !description.isEmpty {
                        aboutSection(description: description)
                    }
                    locationSection(event: event)
                    if let source = event.sourceURL, let url = URL(string: source) {
                        sourceLink(url: url, name: event.sourceName)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.top, 20)
                .padding(.bottom, 32)
            }
        }
    }

    @ViewBuilder
    private func heroImage(event: EventDTO) -> some View {
        // Fixed-frame Rectangle + overlay pattern: the Rectangle owns the size
        // (260pt tall, full width). AsyncImage renders into the overlay and is
        // clipped by the Rectangle's frame, so post-load layout cannot resize
        // the container — fixes the iOS 26 "snap to broken layout" symptom.
        Rectangle()
            .fill(Color.appSecondaryBackground)
            .frame(maxWidth: .infinity)
            .frame(height: 260)
            .overlay {
                if let urlString = event.images.first, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.clear
                    }
                } else {
                    Image(systemName: "calendar")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary)
                }
            }
            .clipped()
    }

    @ViewBuilder
    private func titleBlock(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(event.title)
                .font(.title2.weight(.bold))
                .fixedSize(horizontal: false, vertical: true)
                .lineLimit(4)

            HStack(spacing: 8) {
                if event.isFree {
                    badge(text: "Free", tint: .green)
                } else if let price = event.price, price > 0 {
                    badge(text: String(format: "$%.2f", price), tint: .blue)
                }
                if let ageMin = event.ageMin, let ageMax = event.ageMax {
                    badge(text: "Ages \(ageMin)–\(ageMax)", tint: .secondary)
                }
            }
        }
    }

    @ViewBuilder
    private func badge(text: String, tint: Color) -> some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(tint)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(tint.opacity(0.12))
            .clipShape(Capsule())
    }

    @ViewBuilder
    private func tagRow(event: EventDTO) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(event.tags, id: \.id) { tag in
                    Text(tag.name)
                        .font(.footnote)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.appSecondaryBackground)
                        .clipShape(Capsule())
                }
            }
        }
    }

    @ViewBuilder
    private func infoGrid(event: EventDTO) -> some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            infoCell(systemImage: "calendar", label: "Date", value: Self.dateFormatter.string(from: event.startDatetime))
            infoCell(systemImage: "clock", label: "Time", value: Self.timeFormatter.string(from: event.startDatetime))
            infoCell(systemImage: "figure.2.and.child.holdinghands", label: "Ages", value: ageText(event: event))
            infoCell(systemImage: event.isFree ? "checkmark.seal" : "dollarsign.circle", label: "Price",
                     value: event.isFree ? "Free" : (event.price.map { String(format: "$%.2f", $0) } ?? "—"))
        }
    }

    private func ageText(event: EventDTO) -> String {
        if let lo = event.ageMin, let hi = event.ageMax { return "\(lo)–\(hi)" }
        if let lo = event.ageMin { return "\(lo)+" }
        return "All ages"
    }

    @ViewBuilder
    private func infoCell(systemImage: String, label: String, value: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.body)
                .foregroundStyle(.tint)
                .frame(width: 22)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSecondaryBackground)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private func aboutSection(description: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionHeader("About")
            Text(description)
                .font(.body)
                .foregroundStyle(.primary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder
    private func locationSection(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Location")
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.body)
                    .foregroundStyle(.tint)
                    .frame(width: 22)
                VStack(alignment: .leading, spacing: 2) {
                    if let venue = event.venueName, !venue.isEmpty {
                        Text(venue).font(.subheadline.weight(.medium))
                    }
                    if let address = event.address, !address.isEmpty {
                        Text(address).font(.caption).foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 0)
            }
            if let url = appleMapsURL(for: event) {
                Link(destination: url) {
                    HStack(spacing: 6) {
                        Image(systemName: "map")
                        Text("Open in Maps")
                    }
                    .font(.subheadline.weight(.medium))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity)
                    .background(Color.accentColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func sourceLink(url: URL, name: String?) -> some View {
        Link(destination: url) {
            HStack(spacing: 6) {
                Image(systemName: "link")
                Text("View on \(name ?? "source")")
            }
            .font(.subheadline)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .background(Color.appSecondaryBackground)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.headline)
            .foregroundStyle(.primary)
    }

    private func appleMapsURL(for event: EventDTO) -> URL? {
        if let lat = event.latitude, let lng = event.longitude {
            let q = event.venueName?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "Event"
            return URL(string: "http://maps.apple.com/?ll=\(lat),\(lng)&q=\(q)")
        }
        if let address = event.address, !address.isEmpty,
           let encoded = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) {
            return URL(string: "http://maps.apple.com/?q=\(encoded)")
        }
        return nil
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE, MMM d"
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()
}
