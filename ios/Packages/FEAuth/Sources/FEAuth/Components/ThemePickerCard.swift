import SwiftUI
import FEDesignSystem

/// Three-way Light / Dark / System picker bound to
/// `@AppStorage("family-events-theme")`. Drives `.preferredColorScheme`
/// at the app root. Mirrors web's ProfileThemeCard.
public struct ThemePickerCard: View {
    @Binding public var appearanceRawValue: String

    public init(appearanceRawValue: Binding<String>) {
        _appearanceRawValue = appearanceRawValue
    }

    public var body: some View {
        Picker("Theme", selection: $appearanceRawValue) {
            ForEach(AppAppearancePreference.allCases) { option in
                Text(option.title).tag(option.rawValue)
            }
        }
        .pickerStyle(.segmented)
    }
}
