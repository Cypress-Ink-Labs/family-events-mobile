import SwiftUI
import FECore
import FEData
import FEDesignSystem

struct EventDetailInfoGrid: View {
    let event: EventDTO

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            infoCell(systemImage: "calendar", label: "Date", value: Self.dateFormatter.string(from: event.startDatetime))
            infoCell(systemImage: "clock", label: "Time", value: Self.timeFormatter.string(from: event.startDatetime))
            infoCell(systemImage: "figure.2.and.child.holdinghands", label: "Ages", value: ageText)
            infoCell(systemImage: event.isFree ? "checkmark.seal" : "dollarsign.circle", label: "Price",
                     value: event.isFree ? "Free" : (event.price.map { String(format: "$%.2f", $0) } ?? "—"))
        }
    }

    private var ageText: String {
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
                    .foregroundStyle(Color.dsTextMuted)
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

    static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE, MMM d"
        return f
    }()

    static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()
}
