import SwiftUI
import MapKit
import FECore
import FEData
import FEDesignSystem

// MARK: - ExploreMapView

public struct ExploreMapView: View {
    public let events: [EventDTO]
    public let onSelectEvent: (EventID) -> Void

    @State private var selectedEvent: EventDTO?

    public init(events: [EventDTO], onSelectEvent: @escaping (EventID) -> Void) {
        self.events = events
        self.onSelectEvent = onSelectEvent
    }

    private var mappableEvents: [(EventDTO, CLLocationCoordinate2D)] {
        events.compactMap { event in
            guard let lat = event.latitude, let lng = event.longitude else { return nil }
            return (event, CLLocationCoordinate2D(latitude: lat, longitude: lng))
        }
    }

    /// Bounding region with 20% padding around visible pins.
    /// Falls back to an Austin-centered view when no events have coords.
    private var region: MKCoordinateRegion {
        let coords = mappableEvents.map(\.1)
        if coords.isEmpty {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 30.27, longitude: -97.74),
                span: MKCoordinateSpan(latitudeDelta: 0.5, longitudeDelta: 0.5)
            )
        }
        let lats = coords.map(\.latitude)
        let lngs = coords.map(\.longitude)
        let minLat = lats.min() ?? 0
        let maxLat = lats.max() ?? 0
        let minLng = lngs.min() ?? 0
        let maxLng = lngs.max() ?? 0
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLng + maxLng) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max((maxLat - minLat) * 1.2, 0.02),
            longitudeDelta: max((maxLng - minLng) * 1.2, 0.02)
        )
        return MKCoordinateRegion(center: center, span: span)
    }

    public var body: some View {
        Map(initialPosition: .region(region)) {
            ForEach(mappableEvents, id: \.0.id) { event, coord in
                Annotation(event.title, coordinate: coord) {
                    Button {
                        selectedEvent = event
                    } label: {
                        Image(systemName: "mappin.circle.fill")
                            .font(.title)
                            .foregroundStyle(.tint)
                            .background(Circle().fill(.background))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .sheet(item: $selectedEvent) { event in
            mapEventSheet(for: event)
        }
    }

    @ViewBuilder
    private func mapEventSheet(for event: EventDTO) -> some View {
        VStack(spacing: 12) {
            Capsule()
                .frame(width: 36, height: 5)
                .foregroundStyle(.secondary.opacity(0.5))
                .padding(.top, 8)
            ExploreMapEventCard(event: event, onSelect: {
                selectedEvent = nil
                onSelectEvent(event.id)
            })
            .padding(.horizontal)
            Spacer(minLength: 12)
        }
        .presentationDetents([.fraction(0.3), .medium])
        .presentationDragIndicator(.hidden)
    }
}

// MARK: - ExploreMapEventCard

struct ExploreMapEventCard: View {
    let event: EventDTO
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            VStack(alignment: .leading, spacing: 8) {
                Text(event.title).font(.headline).foregroundStyle(.primary)
                if let venue = event.venueName, !venue.isEmpty {
                    Label(venue, systemImage: "mappin").font(.subheadline).foregroundStyle(.secondary)
                }
                HStack(spacing: 8) {
                    if event.isFree {
                        Text("Free").font(.caption.weight(.semibold))
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.green.opacity(0.15)).foregroundStyle(.green)
                            .clipShape(Capsule())
                    }
                    Text(DateFormatting.cardSubtitleFormatter.string(from: event.startDatetime))
                        .font(.caption).foregroundStyle(.secondary)
                }
                Text("View details →").font(.subheadline).foregroundStyle(.tint).padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color.dsSurfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}
