import SwiftUI
import FEDesignSystem

public struct PlanContextBar: View {
    public let cityName: String?
    public let kidAge: Int?

    public init(cityName: String?, kidAge: Int?) {
        self.cityName = cityName
        self.kidAge = kidAge
    }

    public var body: some View {
        HStack(spacing: 8) {
            chip(systemImage: "mappin.and.ellipse", text: cityName?.nilIfEmpty ?? "Nearby")
            chip(systemImage: "calendar", text: "Today + 7 days")
            chip(systemImage: "sparkles", text: kidAge.map { "Age \($0) fit" } ?? "Weather-aware")
        }
    }

    @ViewBuilder
    private func chip(systemImage: String, text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: systemImage).appTypography(.caption)
            Text(text).appTypography(.caption)
        }
        .padding(.horizontal, 10).padding(.vertical, 6)
        .background(Color.appSecondaryBackground)
        .clipShape(Capsule())
    }
}

private extension String {
    var nilIfEmpty: String? {
        trimmingCharacters(in: .whitespaces).isEmpty ? nil : self
    }
}
