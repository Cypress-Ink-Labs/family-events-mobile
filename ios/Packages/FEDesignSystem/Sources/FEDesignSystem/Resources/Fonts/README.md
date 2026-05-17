# FEDesignSystem fonts

This directory holds the font files (`.ttf` / `.otf`) the design system
expects to register at runtime. Any file dropped here is auto-bundled
into the Swift package and registered via `FEDesignSystem.registerFonts()`
without code changes.

## Expected fonts

| Family | Weight | Filename | Source |
|---|---|---|---|
| Fraunces | 400 Regular | `Fraunces-Regular.ttf` | https://fonts.google.com/specimen/Fraunces |
| Fraunces | 500 Medium | `Fraunces-Medium.ttf` | same |
| Fraunces | 600 SemiBold | `Fraunces-SemiBold.ttf` | same |
| DM Sans | 400 Regular | `DMSans-Regular.ttf` | https://fonts.google.com/specimen/DM+Sans |
| DM Sans | 500 Medium | `DMSans-Medium.ttf` | same |
| Newsreader | 400 Regular | `Newsreader-Regular.ttf` | https://fonts.google.com/specimen/Newsreader |
| Newsreader | 400 Italic | `Newsreader-Italic.ttf` | same |
| Geist Mono | 400 Regular | `GeistMono-Regular.ttf` | https://fonts.google.com/specimen/Geist+Mono |

PostScript names are listed in
`FEDesignSystem.expectedPostScriptNames` — the snapshot tests assert
each one resolves at runtime.

## Adding new fonts

1. Download the font file from its official source (Google Fonts / Vercel for Geist).
2. Drop the `.ttf` (or `.otf`) into this directory using the exact filename
   from the table above.
3. If the PostScript name differs from the filename stem, update
   `expectedPostScriptNames` in `FontRegistration.swift`.
4. No `Package.swift` edit needed — the `.process("Resources/Fonts")`
   declaration globs every file.

## Verifying

Run from the package root:

```bash
swift test --filter FontRegistrationTests
```

If the fonts aren't yet bundled, the app still runs — every
`Font.custom("Fraunces", ...)` call falls back to the closest system
family (serif → ui-serif → Times, sans → ui-sans-serif → Helvetica).
The fallback path is intentional so the UI degrades gracefully when
this directory is empty.
