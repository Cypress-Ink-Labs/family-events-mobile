import Foundation

/// Cleans event descriptions ingested from WordPress / Divi-built sources.
/// These often contain raw `[et_pb_*]` shortcodes and HTML markup that
/// users would otherwise see verbatim. Conservative: strips known noise
/// but preserves prose, line breaks, and links rendered as plain text.
public enum DescriptionSanitizer {
    /// Returns nil for nil input or whitespace-only output.
    public static func clean(_ raw: String?) -> String? {
        guard let raw, !raw.isEmpty else { return nil }
        var out = raw

        out = stripDiviShortcodes(out)
        out = stripGenericShortcodes(out)
        out = stripHTMLTags(out)
        out = decodeCommonEntities(out)
        out = collapseWhitespace(out)

        let trimmed = out.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    // MARK: - Pieces

    /// Match `[et_pb_*]` opening tags with arbitrary attributes and
    /// `[/et_pb_*]` closing tags. Divi shortcodes have unbounded attribute
    /// strings and may span multiple lines after rich text editing.
    static func stripDiviShortcodes(_ input: String) -> String {
        regexReplace(input, pattern: #"\[\/?et_pb_[a-z0-9_]*[^\]]*\]"#, with: "")
    }

    /// Generic WordPress shortcode fallback: `[caption …]`, `[gallery]`,
    /// `[/caption]`. Conservative — only matches `[name …]` where name is
    /// alphanumeric+underscore so we don't eat user-written `[notes]` prose.
    static func stripGenericShortcodes(_ input: String) -> String {
        regexReplace(input, pattern: #"\[\/?[a-z][a-z0-9_]*(?:\s[^\]]*)?\]"#, with: "")
    }

    /// Replaces `<br>` / `<br/>` / `<p>` with newlines, then strips all
    /// remaining tags. Preserves paragraph breaks in the visible text.
    static func stripHTMLTags(_ input: String) -> String {
        var s = input
        s = regexReplace(s, pattern: #"<br\s*\/?>"#, with: "\n", caseInsensitive: true)
        s = regexReplace(s, pattern: #"<\/p>"#, with: "\n\n", caseInsensitive: true)
        s = regexReplace(s, pattern: #"<[^>]+>"#, with: "")
        return s
    }

    static func decodeCommonEntities(_ input: String) -> String {
        var s = input
        let map: [(String, String)] = [
            ("&nbsp;", " "),
            ("&amp;", "&"),
            ("&lt;", "<"),
            ("&gt;", ">"),
            ("&quot;", "\""),
            ("&#039;", "'"),
            ("&apos;", "'"),
            ("&hellip;", "…"),
            ("&ndash;", "–"),
            ("&mdash;", "—"),
            ("&rsquo;", "'"),
            ("&lsquo;", "'"),
            ("&rdquo;", "\""),
            ("&ldquo;", "\""),
        ]
        for (entity, replacement) in map {
            s = s.replacingOccurrences(of: entity, with: replacement)
        }
        return s
    }

    /// Collapse runs of blank lines (3+) into a single blank line and
    /// trim trailing whitespace on each line.
    static func collapseWhitespace(_ input: String) -> String {
        var s = regexReplace(input, pattern: #"[ \t]+\n"#, with: "\n")
        s = regexReplace(s, pattern: #"\n{3,}"#, with: "\n\n")
        s = regexReplace(s, pattern: #"[ \t]{2,}"#, with: " ")
        return s
    }

    // MARK: - Helper

    private static func regexReplace(_ input: String, pattern: String, with replacement: String, caseInsensitive: Bool = true) -> String {
        var options: NSRegularExpression.Options = [.dotMatchesLineSeparators]
        if caseInsensitive { options.insert(.caseInsensitive) }
        guard let re = try? NSRegularExpression(pattern: pattern, options: options) else {
            return input
        }
        let range = NSRange(input.startIndex..<input.endIndex, in: input)
        return re.stringByReplacingMatches(in: input, options: [], range: range, withTemplate: replacement)
    }
}
