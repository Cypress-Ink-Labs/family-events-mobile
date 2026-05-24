import SwiftUI
import MapKit
import FECore
import FEData
import FEDesignSystem

@MainActor
public struct MapScreen: View {
    @Bindable var viewModel: MapViewModel
    public let onSelectEvent: (EventID) -> Void

    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var selectedEvent: EventDTO?

    public init(viewModel: MapViewModel, onSelectEvent: @escaping (EventID) -> Void) {
        self.viewModel = viewModel
        self.onSelectEvent = onSelectEvent
    }

    public var body: some View {
        ZStack {
            map
            overlay
        }
        .navigationTitle("Map")
        .refreshable { await viewModel.refresh() }
        .task { await viewModel.loadIfNeeded() }
        .sheet(item: $selectedEvent) { event in
            popup(for: event)
        }
        .onChange(of: viewModel.events) { _, _ in
            adjustCameraForEvents()
        }
    }

    private var map: some View {
        Map(position: $cameraPosition, selection: $selectedEvent.eventIDBinding(in: viewModel.events)) {
            ForEach(viewModel.events) { event in
                if let lat = event.latitude, let lng = event.longitude {
                    Annotation(
                        event.title,
                        coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lng)
                    ) {
                        Button { selectedEvent = event } label: {
                            EventPin(event: event, isSelected: selectedEvent?.id == event.id)
                        }
                        .buttonStyle(.plain)
                    }
                    .tag(event.id.rawValue)
                }
            }
        }
        .mapStyle(.standard(elevation: .realistic))
        .ignoresSafeArea(edges: .bottom)
    }

    @ViewBuilder
    private var overlay: some View {
        if viewModel.isLoading && viewModel.events.isEmpty {
            ProgressView()
                .controlSize(.large)
                .padding()
                .background(Color.dsSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
        } else if let message = viewModel.errorMessage, viewModel.events.isEmpty {
            errorOverlay(message: message)
        } else if viewModel.events.isEmpty {
            emptyOverlay
        }
    }

    @ViewBuilder
    private func popup(for event: EventDTO) -> some View {
        VStack {
            Capsule()
                .frame(width: 36, height: 5)
                .foregroundStyle(Color.dsBorder.opacity(0.6))
                .padding(.top, 8)
            EventPopupCard(event: event) {
                selectedEvent = nil
                onSelectEvent(event.id)
            }
            .padding(.horizontal)
            Spacer(minLength: 12)
        }
        .presentationDetents([.fraction(0.3), .medium])
        .presentationDragIndicator(.hidden)
    }

    private func errorOverlay(message: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .foregroundStyle(Color.dsWarning)
            Text(message)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextPrimary)
            Button("Retry") {
                Task { await viewModel.refresh() }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.dsAccentPrimary)
        }
        .padding()
        .background(Color.dsSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
        .padding()
    }

    private var emptyOverlay: some View {
        VStack(spacing: 8) {
            Image(systemName: "map")
                .font(.system(size: 32))
                .foregroundStyle(Color.dsTextMuted)
            Text("No mapped events for this city.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
        }
        .padding()
        .background(Color.dsSurfaceRaised, in: RoundedRectangle(cornerRadius: 12))
        .padding()
    }

    private func adjustCameraForEvents() {
        let coords: [CLLocationCoordinate2D] = viewModel.events.compactMap { event in
            guard let lat = event.latitude, let lng = event.longitude else { return nil }
            return CLLocationCoordinate2D(latitude: lat, longitude: lng)
        }
        guard !coords.isEmpty else { return }
        let lats = coords.map(\.latitude)
        let lngs = coords.map(\.longitude)
        let center = CLLocationCoordinate2D(
            latitude: ((lats.min() ?? 0) + (lats.max() ?? 0)) / 2,
            longitude: ((lngs.min() ?? 0) + (lngs.max() ?? 0)) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max(((lats.max() ?? 0) - (lats.min() ?? 0)) * 1.2, 0.02),
            longitudeDelta: max(((lngs.max() ?? 0) - (lngs.min() ?? 0)) * 1.2, 0.02)
        )
        cameraPosition = .region(MKCoordinateRegion(center: center, span: span))
    }
}

private extension Binding where Value == EventDTO? {
    /// MapKit `selection` binds an `Identifiable`'s id; this adapts our
    /// optional EventDTO state into the String-ID flavor MapKit prefers.
    func eventIDBinding(in events: [EventDTO]) -> Binding<String?> {
        Binding<String?>(
            get: { wrappedValue?.id.rawValue },
            set: { newValue in
                wrappedValue = events.first { $0.id.rawValue == newValue }
            }
        )
    }
}
