import SwiftUI
import FECore
import FEData
import FEDesignSystem

@MainActor
public struct EventDetailScreen: View {
    @State private var viewModel: EventDetailViewModel
    @State private var calendarToast: CalendarToast?
    @State private var draftComment: String = ""
    @State private var draftRating: Int = 0
    private let eventID: EventID

    private struct CalendarToast: Identifiable {
        let id = UUID()
        let title: String
        let message: String
        let isError: Bool
    }

    public init(
        eventID: EventID,
        eventRepo: any EventRepository,
        favoriteRepo: any FavoriteRepo,
        ratingRepo: (any RatingRepo)? = nil,
        commentRepo: (any CommentRepo)? = nil,
        userID: UserID
    ) {
        self.eventID = eventID
        _viewModel = State(initialValue: EventDetailViewModel(
            eventRepo: eventRepo,
            favoriteRepo: favoriteRepo,
            ratingRepo: ratingRepo,
            commentRepo: commentRepo,
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
            if let event = viewModel.event {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button {
                            Task { await addToCalendar(event: event) }
                        } label: {
                            Label("Add to Calendar", systemImage: "calendar.badge.plus")
                        }
                        if let url = EventShareURL.build(eventID: event.id) {
                            ShareLink(item: url, subject: Text(event.title), message: Text("Family plan: \(event.title)")) {
                                Label("Share", systemImage: "square.and.arrow.up")
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityLabel("More actions")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(action: { viewModel.toggleFavorite() }) {
                    Image(systemName: viewModel.isFavorited ? "heart.fill" : "heart")
                        .foregroundStyle(viewModel.isFavorited ? Color.pink : Color.primary)
                }
                .accessibilityLabel(viewModel.isFavorited ? "Unfavorite" : "Favorite")
            }
        }
        .alert(item: $calendarToast) { toast in
            Alert(
                title: Text(toast.title),
                message: Text(toast.message),
                dismissButton: .default(Text("OK"))
            )
        }
    }

    #if canImport(EventKit)
    private func addToCalendar(event: EventDTO) async {
        let writer = EventKitWriter()
        do {
            try await writer.addToCalendar(event: event)
            calendarToast = CalendarToast(
                title: "Added to Calendar",
                message: "\(event.title) is on your calendar.",
                isError: false
            )
        } catch EventKitWriter.WriteError.accessDenied {
            calendarToast = CalendarToast(
                title: "Calendar Access Denied",
                message: "Allow Calendar access in Settings to add events.",
                isError: true
            )
        } catch EventKitWriter.WriteError.noDefaultCalendar {
            calendarToast = CalendarToast(
                title: "No Calendar",
                message: "Add a default calendar in the Calendar app, then try again.",
                isError: true
            )
        } catch {
            calendarToast = CalendarToast(
                title: "Couldn't Add Event",
                message: error.localizedDescription,
                isError: true
            )
        }
    }
    #else
    private func addToCalendar(event: EventDTO) async {}
    #endif

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
                    if let description = DescriptionSanitizer.clean(event.description) {
                        aboutSection(description: description)
                    }
                    locationSection(event: event)
                    if let source = event.sourceURL, let url = URL(string: source) {
                        sourceLink(url: url, name: event.sourceName)
                    }
                    ratingSection(event: event)
                    commentSection(event: event)
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
            .fill(Color.dsSurfaceRaised)
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
                        .background(Color.dsSurfaceRaised)
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
        .background(Color.dsSurfaceRaised)
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
            .background(Color.dsSurfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func ratingSection(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader("Your rating")
            StarRatingView(
                score: $draftRating,
                isDisabled: viewModel.isRatingInFlight,
                onChange: { score in
                    Task { await viewModel.setRating(score) }
                }
            )
            .onAppear { draftRating = viewModel.userRating?.score ?? 0 }
            .onChange(of: viewModel.userRating?.score) { _, new in
                draftRating = new ?? 0
            }
            if event.ratingCount > 0 {
                Text(communityRatingText(event: event))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func communityRatingText(event: EventDTO) -> String {
        let avg = String(format: "%.1f", event.avgRating)
        return "Community average: \(avg) (\(event.ratingCount) rating\(event.ratingCount == 1 ? "" : "s"))"
    }

    @ViewBuilder
    private func commentSection(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Comments")
            HStack(alignment: .top, spacing: 8) {
                TextField("Share what you're hoping for…", text: $draftComment, axis: .vertical)
                    .lineLimit(1...4)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color.dsSurfaceRaised)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                Button {
                    Task {
                        await viewModel.addComment(draftComment)
                        if viewModel.commentError == nil { draftComment = "" }
                    }
                } label: {
                    Image(systemName: "paperplane.fill")
                        .padding(10)
                        .background(Color.accentColor)
                        .foregroundStyle(.white)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .disabled(viewModel.isCommentInFlight || draftComment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .accessibilityLabel("Post comment")
            }
            if let err = viewModel.commentError {
                Text(err)
                    .font(.caption)
                    .foregroundStyle(Color.red)
            }
            if viewModel.comments.isEmpty {
                Text("No comments yet. Be the first.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(viewModel.comments) { comment in
                        commentRow(comment)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func commentRow(_ comment: CommentDTO) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Image(systemName: "person.crop.circle.fill")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                Text(comment.authorDisplayName ?? "Anonymous")
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Text(Self.commentDateFormatter.string(from: comment.createdAt))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Text(comment.body)
                .font(.body)
                .foregroundStyle(.primary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.dsSurfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private static let commentDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()

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
