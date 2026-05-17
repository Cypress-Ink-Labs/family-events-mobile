import CoreText
import Foundation
#if canImport(UIKit)
import UIKit
#endif

public enum FEDesignSystem {
    /// Registers every `.ttf` and `.otf` file bundled under
    /// `Sources/FEDesignSystem/Resources/Fonts/` with the system's font
    /// manager so SwiftUI `Font.custom("Fraunces", ...)` resolves them.
    ///
    /// Call once from the app entry point (e.g. inside `init()` on your
    /// `@main App` struct) before any view that uses the design fonts is
    /// rendered. Idempotent — re-registering an already-registered font
    /// silently no-ops.
    ///
    /// Returns the PostScript names of fonts that registered successfully.
    /// Names that failed are logged via `print(_:)` and surfaced through
    /// the returned `failures` array. Failures are non-fatal — the design
    /// system gracefully falls back to system serif/sans equivalents.
    @discardableResult
    public static func registerFonts() -> (registered: [String], failures: [String]) {
        guard let fontsURL = Bundle.module.url(forResource: "Fonts", withExtension: nil) else {
            // No bundled fonts — every Font.custom() call falls back to
            // the closest system family. App still renders.
            return (registered: [], failures: [])
        }

        var registered: [String] = []
        var failures: [String] = []

        let allowedExtensions: Set<String> = ["ttf", "otf", "ttc"]
        let urls = (try? FileManager.default.contentsOfDirectory(
            at: fontsURL,
            includingPropertiesForKeys: nil
        )) ?? []

        for url in urls where allowedExtensions.contains(url.pathExtension.lowercased()) {
            var error: Unmanaged<CFError>?
            if CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error) {
                registered.append(url.lastPathComponent)
            } else {
                let message = error?.takeRetainedValue().localizedDescription ?? "unknown error"
                failures.append("\(url.lastPathComponent): \(message)")
            }
        }

        return (registered: registered, failures: failures)
    }

    /// The exact PostScript names the design system expects to resolve.
    /// Use these in tests / asserts when validating an environment has the
    /// right fonts available.
    public static let expectedPostScriptNames: [String] = [
        "Fraunces-Regular",
        "Fraunces-Medium",
        "Fraunces-SemiBold",
        "DMSans-Regular",
        "DMSans-Medium",
        "Newsreader-Regular",
        "Newsreader-Italic",
        "GeistMono-Regular",
    ]
}
