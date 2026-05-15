import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct EventDetailScreen: View {
    @State private var viewModel: EventDetailViewModel
    private let eventID: EventID

    public init(eventID: EventID, eventRepo: any EventRepository, userID: UserID) {
        self.eventID = eventID
        _viewModel = State(initialValue: EventDetailViewModel(eventRepo: eventRepo, userID: userID, eventID: eventID))
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
        #endif
    }

    @ViewBuilder
    private var loadingState: some View {
        ProgressView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding()
    }

    @ViewBuilder
    private func errorState(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(.orange)
            Text("Couldn't load this event")
                .appTypography(.titleMedium)
            Text(message)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry") { Task { await viewModel.load() } }
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func content(for event: EventDTO) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                heroImage(event: event)
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
                Spacer(minLength: 24)
            }
            .padding(.horizontal)
            .padding(.bottom, 24)
        }
    }

    @ViewBuilder
    private func heroImage(event: EventDTO) -> some View {
        if let urlString = event.images.first, let url = URL(string: urlString) {
            AsyncImage(url: url) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle().fill(Color.appSecondaryBackground)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 240)
            .clipped()
            .cornerRadius(12)
        }
    }

    @ViewBuilder
    private func titleBlock(event: EventDTO) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(event.title).appTypography(.titleLarge)
                if event.isFree {
                    Text("Free").appTypography(.caption)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Color.appAccent.opacity(0.15))
                        .clipShape(Capsule())
                } else if let price = event.price, price > 0 {
                    Text("$\(price, specifier: "%.2f")").appTypography(.caption)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Color.appSecondaryBackground)
                        .clipShape(Capsule())
                }
            }
            Spacer()
            FavoriteButton(
                isFavorited: Binding(get: { viewModel.isFavorited }, set: { _ in }),
                onToggle: { viewModel.toggleFavorite() }
            )
        }
    }

    @ViewBuilder
    private func tagRow(event: EventDTO) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(event.tags, id: \.id) { tag in
                    Text(tag.name)
                        .appTypography(.caption)
                        .padding(.horizontal, 10).padding(.vertical, 5)
                        .background(Color.appSecondaryBackground)
                        .clipShape(Capsule())
                }
            }
        }
    }

    @ViewBuilder
    private func infoGrid(event: EventDTO) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            infoCell(systemImage: "calendar", label: "Date", value: Self.dateFormatter.string(from: event.startDatetime))
            infoCell(systemImage: "clock", label: "Time", value: Self.timeFormatter.string(from: event.startDatetime))
            if let ageMin = event.ageMin, let ageMax = event.ageMax {
                infoCell(systemImage: "figure.2.and.child.holdinghands", label: "Ages", value: "\(ageMin)-\(ageMax)")
            } else if let ageMin = event.ageMin {
                infoCell(systemImage: "figure.2.and.child.holdinghands", label: "Ages", value: "\(ageMin)+")
            } else {
                infoCell(systemImage: "figure.2.and.child.holdinghands", label: "Ages", value: "All ages")
            }
            infoCell(
                systemImage: event.isFree ? "checkmark.seal" : "dollarsign.circle",
                label: "Price",
                value: event.isFree ? "Free" : (event.price.map { String(format: "$%.2f", $0) } ?? "—")
            )
        }
    }

    @ViewBuilder
    private func infoCell(systemImage: String, label: String, value: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(.tint)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).appTypography(.caption).foregroundStyle(.secondary)
                Text(value).appTypography(.body)
            }
            Spacer()
        }
        .padding(12)
        .background(Color.appSecondaryBackground)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private func aboutSection(description: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("About").appTypography(.titleMedium)
            Text(description).appTypography(.body).foregroundStyle(.primary)
        }
    }

    @ViewBuilder
    private func locationSection(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Location").appTypography(.titleMedium)
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.title3).foregroundStyle(.tint)
                VStack(alignment: .leading, spacing: 4) {
                    if let venue = event.venueName, !venue.isEmpty {
                        Text(venue).appTypography(.body)
                    }
                    if let address = event.address, !address.isEmpty {
                        Text(address).appTypography(.caption).foregroundStyle(.secondary)
                    }
                }
                Spacer()
            }
            if let url = appleMapsURL(for: event) {
                Link(destination: url) {
                    HStack {
                        Image(systemName: "map")
                        Text("Open in Maps")
                    }
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(Color.appAccent.opacity(0.15))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func sourceLink(url: URL, name: String?) -> some View {
        Link(destination: url) {
            HStack {
                Image(systemName: "link")
                Text("View on \(name ?? "source")")
            }
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(Color.appSecondaryBackground)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func appleMapsURL(for event: EventDTO) -> URL? {
        if let lat = event.latitude, let lng = event.longitude {
            return URL(string: "http://maps.apple.com/?ll=\(lat),\(lng)&q=\(event.venueName?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "Event")")
        }
        if let address = event.address, !address.isEmpty,
           let encoded = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) {
            return URL(string: "http://maps.apple.com/?q=\(encoded)")
        }
        return nil
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE, MMM d, yyyy"
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()
}
