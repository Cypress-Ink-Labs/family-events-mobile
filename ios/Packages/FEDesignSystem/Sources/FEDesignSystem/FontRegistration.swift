import CoreText
import Foundation
import SwiftUI
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

    #if canImport(UIKit)
    /// Configures global UINavigationBar + UITabBar appearance to the
    /// warm-paper palette so translucent system chrome doesn't ghost
    /// scroll content behind it. Idempotent; safe to re-run.
    ///
    /// Call once at app start after `registerFonts()`. Without this, iOS
    /// renders nav/tab bars with a system-translucent material that lets
    /// the warm-paper background bleed through and reads as confused
    /// ghosting on phone screens.
    public static func configureChromeAppearance() {
        let bgColor = UIColor(Color.dsBackground)
        let textColor = UIColor(Color.dsTextPrimary)

        // Nav bar
        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithOpaqueBackground()
        navAppearance.backgroundColor = bgColor
        navAppearance.shadowColor = UIColor(Color.dsBorder)
        navAppearance.titleTextAttributes = [
            .foregroundColor: textColor,
            .font: UIFont(name: "Fraunces-Medium", size: 17) ?? UIFont.systemFont(ofSize: 17, weight: .medium),
        ]
        navAppearance.largeTitleTextAttributes = [
            .foregroundColor: textColor,
            .font: UIFont(name: "Fraunces-Medium", size: 32) ?? UIFont.systemFont(ofSize: 32, weight: .medium),
        ]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance
        UINavigationBar.appearance().compactAppearance = navAppearance

        // Tab bar
        let tabAppearance = UITabBarAppearance()
        tabAppearance.configureWithOpaqueBackground()
        tabAppearance.backgroundColor = bgColor
        tabAppearance.shadowColor = UIColor(Color.dsBorder)
        let selectedAttrs: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor(Color.dsAccentPrimary),
        ]
        let normalAttrs: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor(Color.dsTextMuted),
        ]
        for layout in [tabAppearance.stackedLayoutAppearance, tabAppearance.inlineLayoutAppearance, tabAppearance.compactInlineLayoutAppearance] {
            layout.selected.iconColor = UIColor(Color.dsAccentPrimary)
            layout.selected.titleTextAttributes = selectedAttrs
            layout.normal.iconColor = UIColor(Color.dsTextMuted)
            layout.normal.titleTextAttributes = normalAttrs
        }
        UITabBar.appearance().standardAppearance = tabAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabAppearance
    }
    #else
    public static func configureChromeAppearance() {}
    #endif
}

