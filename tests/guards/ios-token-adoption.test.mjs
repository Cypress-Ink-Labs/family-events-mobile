import assert from "node:assert/strict"
import { readdirSync, readFileSync, statSync } from "node:fs"
import path from "node:path"
import test from "node:test"

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
const packagesRoot = path.join(repoRoot, "ios", "Packages")

// Consumer packages whose Sources/ must use FEDesignSystem tokens instead of
// SwiftUI default Colors / hex literals / Color.accentColor. The token bridge
// (FEDesignSystem.Color.dsToken / Color.dsTextMuted / Color.dsAccentPrimary
// etc.) keeps iOS aligned with the web client's design tokens.
const CONSUMER_PACKAGES = [
  "FEAuth",
  "FECalendar",
  "FECityPicker",
  "FEEventDetail",
  "FEExplore",
  "FEMap",
  "FEPlan",
  "FESaved",
]

// FEDesignSystem itself owns the raw color literals (generated Tokens.swift),
// so it's intentionally exempt. FECore has no UI surface.
const EXEMPT_FILES = new Set([
  "Tokens.swift",
])

const FORBIDDEN_PATTERNS = [
  {
    pattern: /\.foregroundStyle\(\s*\.secondary\s*\)/g,
    fix: "Use .foregroundStyle(Color.dsTextMuted) — matches web text-muted token",
  },
  {
    pattern: /\.foregroundStyle\(\s*\.primary\s*\)/g,
    fix: "Use .foregroundStyle(Color.dsTextPrimary) — matches web text-primary token",
  },
  {
    pattern: /\.foregroundStyle\(\s*Color\.secondary\s*\)/g,
    fix: "Use Color.dsTextMuted",
  },
  {
    pattern: /\.foregroundStyle\(\s*Color\.primary\s*\)/g,
    fix: "Use Color.dsTextPrimary",
  },
  {
    pattern: /Color\.accentColor/g,
    fix: "Use Color.dsAccentPrimary (or .dsAccentSecondary for action color)",
  },
]

function walkSwiftFiles(root) {
  const out = []
  const stack = [root]
  while (stack.length > 0) {
    const current = stack.pop()
    let entries
    try {
      entries = readdirSync(current, { withFileTypes: true })
    } catch {
      continue
    }
    for (const entry of entries) {
      const next = path.join(current, entry.name)
      if (entry.isDirectory()) {
        if (entry.name === ".build" || entry.name === ".swiftpm" || entry.name === "Generated") continue
        stack.push(next)
      } else if (entry.name.endsWith(".swift")) {
        out.push(next)
      }
    }
  }
  return out
}

function findFirstMatch(pattern, content) {
  const re = new RegExp(pattern.source, pattern.flags)
  return re.test(content) ? content.match(re) : null
}

test("iOS consumer packages do not use SwiftUI default Color tokens", () => {
  const violations = []
  for (const pkg of CONSUMER_PACKAGES) {
    const sourcesDir = path.join(packagesRoot, pkg, "Sources")
    let exists = false
    try {
      exists = statSync(sourcesDir).isDirectory()
    } catch {
      continue
    }
    if (!exists) continue
    const files = walkSwiftFiles(sourcesDir)
    for (const file of files) {
      const base = path.basename(file)
      if (EXEMPT_FILES.has(base)) continue
      const content = readFileSync(file, "utf8")
      for (const { pattern, fix } of FORBIDDEN_PATTERNS) {
        const match = findFirstMatch(pattern, content)
        if (match) {
          const idx = content.indexOf(match[0])
          const lineNumber = content.slice(0, idx).split("\n").length
          violations.push(`${path.relative(repoRoot, file)}:${lineNumber}: '${match[0]}'. ${fix}.`)
        }
      }
    }
  }
  assert.equal(
    violations.length,
    0,
    `Found ${violations.length} non-token color usage(s) in consumer iOS packages. ` +
      `Replace with Color.dsToken-prefixed helpers from FEDesignSystem.\n\n  ` +
      violations.join("\n  "),
  )
})
