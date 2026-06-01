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

    private struct CalendarToast {
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
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let err = viewModel.errorMessage, viewModel.event == nil {
                errorState(err)
            } else if let event = viewModel.event {
                content(for: event)
            }
        }
        .task {
            await viewModel.load()
            guard !Task.isCancelled else { return }
            viewModel.startObservingComments()
        }
        .onDisappear { viewModel.stopObservingComments() }
        .refreshable { await viewModel.refresh() }
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
                        Label("More actions", systemImage: "ellipsis.circle")
                    }
                    .labelStyle(.iconOnly)
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(viewModel.isFavorited ? "Unfavorite" : "Favorite", systemImage: viewModel.isFavorited ? "heart.fill" : "heart", action: viewModel.toggleFavorite)
                    .foregroundStyle(viewModel.isFavorited ? Color.dsAccentSecondary : Color.dsTextPrimary)
                    .labelStyle(.iconOnly)
            }
        }
        .alert(
            calendarToast?.title ?? "",
            isPresented: Binding(
                get: { calendarToast != nil },
                set: { if !$0 { calendarToast = nil } }
            )
        ) {
            Button("OK") { calendarToast = nil }
        } message: {
            if let toast = calendarToast {
                Text(toast.message)
            }
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
    private func errorState(_ message: String) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 48))
                    .foregroundStyle(.orange)
                Text("Couldn't load this event")
                    .font(.title3.weight(.semibold))
                Text(message)
                    .foregroundStyle(Color.dsTextMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Button("Retry") { Task { await viewModel.load() } }
                    .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 64)
        }
        .scrollBounceBehavior(.always)
    }

    @ViewBuilder
    private func content(for event: EventDTO) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                EventDetailHeroImage(event: event)

                VStack(alignment: .leading, spacing: 24) {
                    titleBlock(event: event)
                    if !event.tags.isEmpty { tagRow(event: event) }
                    EventDetailInfoGrid(event: event)
                    if let description = DescriptionSanitizer.clean(event.description) {
                        aboutSection(description: description)
                    }
                    locationSection(event: event)
                    if let source = event.sourceURL, let url = URL(string: source) {
                        sourceLink(url: url, name: event.sourceName)
                    }
                    ratingSection(event: event)
                    EventDetailCommentSection(
                        comments: viewModel.comments,
                        commentError: viewModel.commentError,
                        isInFlight: viewModel.isCommentInFlight,
                        draftComment: $draftComment,
                        onSubmit: { text in
                            await viewModel.addComment(text)
                            if viewModel.commentError == nil { draftComment = "" }
                        }
                    )
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.top, 20)
                .padding(.bottom, 32)
            }
        }
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
        ScrollView(.horizontal) {
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
        .scrollIndicators(.hidden)
    }

    @ViewBuilder
    private func aboutSection(description: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("About")
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
            Text(description)
                .font(.body)
                .foregroundStyle(Color.dsTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder
    private func locationSection(event: EventDTO) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Location")
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
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
                        Text(address).font(.caption).foregroundStyle(Color.dsTextMuted)
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
                    .background(Color.dsAccentPrimarySoft)
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
            Text("Your rating")
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
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
                    .foregroundStyle(Color.dsTextMuted)
            }
        }
    }

    private func communityRatingText(event: EventDTO) -> String {
        let avg = String(format: "%.1f", event.avgRating)
        return "Community average: \(avg) (\(event.ratingCount) rating\(event.ratingCount == 1 ? "" : "s"))"
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
}
