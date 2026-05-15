import SwiftUI
import FEData
import FEDesignSystem

public struct WeatherStrip: View {
    public let snapshot: WeatherSnapshot

    public init(snapshot: WeatherSnapshot) { self.snapshot = snapshot }

    public var body: some View {
        HStack(spacing: 10) {
            Image(systemName: iconName)
                .font(.title3)
                .foregroundStyle(.tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(temperatureText).appTypography(.titleMedium)
                Text(fitText).appTypography(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(precipitationText).appTypography(.caption).foregroundStyle(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.appSecondaryBackground)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var iconName: String {
        switch snapshot.weatherFit {
        case "outdoor": return "sun.max.fill"
        case "indoor": return snapshot.precipitationChance >= 0.4 ? "cloud.rain.fill" : "thermometer"
        default: return "cloud.fill"
        }
    }

    private var temperatureText: String {
        let f = MeasurementFormatter()
        f.unitOptions = .providedUnit
        f.numberFormatter.maximumFractionDigits = 0
        let measurement = Measurement(value: snapshot.temperatureCelsius, unit: UnitTemperature.celsius)
        let local = measurement.converted(to: Locale.current.measurementSystem == .metric ? .celsius : .fahrenheit)
        return f.string(from: local)
    }

    private var fitText: String {
        switch snapshot.weatherFit {
        case "outdoor": return "Good for outdoor plans"
        case "indoor": return "Better stay indoor"
        default: return "Weather-aware"
        }
    }

    private var precipitationText: String {
        let pct = Int((snapshot.precipitationChance * 100).rounded())
        return "\(pct)% precip"
    }
}
