import SwiftUI

/// Token-driven typography. Maps the v2 type scale (mobile column) to
/// SwiftUI Fonts using `Font.custom` so Fraunces / DM Sans / Newsreader /
/// Geist Mono render when their `.ttf`/`.otf` files are bundled. Falls back
/// to closest system family until those assets land.
///
/// Bundling fonts: add font files to the app target's resources
/// (`Resources/Fonts/`) and list each filename in Info.plist under
/// `UIAppFonts` (or use `.copy()` from a Swift package bundle).
public enum AppFont {
    public static let displayFamily = DesignTokens.FontFamily.display
    public static let bodyFamily = DesignTokens.FontFamily.body
    public static let editorialFamily = DesignTokens.FontFamily.editorial
    public static let monoFamily = DesignTokens.FontFamily.mono

    public static func display(size: CGFloat, weight: Font.Weight = .medium) -> Font {
        Font.custom(displayFamily, size: size, relativeTo: .body).weight(weight)
    }

    public static func body(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        Font.custom(bodyFamily, size: size, relativeTo: .body).weight(weight)
    }

    public static func editorial(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        Font.custom(editorialFamily, size: size, relativeTo: .body).weight(weight)
    }

    public static func mono(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        Font.custom(monoFamily, size: size, relativeTo: .body).weight(weight)
    }
}

/// v2 type scale presets matching the mobile column from tokens.json. Use
/// these for app-wide consistency; one-offs reach for `AppFont.display(size:)`.
public extension Font {
    /// 11pt mono caption — eyebrows, micro-metadata.
    static let dsCaption2xs = AppFont.mono(size: 11)
    /// 12pt mono — price, distance, age range.
    static let dsCaptionXs = AppFont.mono(size: 12)
    /// 14pt body — helper text, pill labels.
    static let dsBodySm = AppFont.body(size: 14)
    /// 16pt body — default body. Never smaller on mobile (iOS HIG).
    static let dsBody = AppFont.body(size: 16)
    /// 18pt editorial italic — long-form curator notes.
    static let dsEditorial = AppFont.editorial(size: 18)
    /// 22pt display — secondary card title.
    static let dsTitleLg = AppFont.display(size: 22)
    /// 28pt display — hero card title.
    static let dsTitleXl = AppFont.display(size: 28)
    /// 36pt display — page title.
    static let dsTitle2xl = AppFont.display(size: 36)
}
