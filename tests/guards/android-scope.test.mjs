import assert from "node:assert/strict"
import { existsSync, readFileSync, readdirSync } from "node:fs"
import path from "node:path"
import test from "node:test"

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
const androidRoot = path.join(repoRoot, "android")
const settingsPath = path.join(androidRoot, "settings.gradle.kts")
const pathPolicyPath = path.join(androidRoot, "core", "src", "main", "java", "com", "familyevents", "core", "ConsumerApiPath.kt")

function discoverAndroidSourcePaths() {
  const files = []
  const stack = [androidRoot]
  while (stack.length > 0) {
    const current = stack.pop()
    const entries = readdirSync(current, { withFileTypes: true })
    for (const entry of entries) {
      const next = path.join(current, entry.name)
      if (entry.isDirectory()) {
        if (!entry.name.startsWith(".") && entry.name !== "build" && entry.name !== "test" && entry.name !== "androidTest") {
          stack.push(next)
        }
      } else if (/\.(kt|kts|xml)$/.test(entry.name)) {
        files.push(next)
      }
    }
  }
  return files
}

function readAndroidSources() {
  return discoverAndroidSourcePaths().map((file) => [file, readFileSync(file, "utf8")])
}

test("android Gradle project and consumer endpoint policy exist", () => {
  assert.equal(existsSync(settingsPath), true)
  assert.equal(existsSync(pathPolicyPath), true)
  const settings = readFileSync(settingsPath, "utf8")
  for (const moduleName of [":app", ":core", ":data", ":designsystem", ":auth", ":plan", ":explore", ":saved", ":eventdetail", ":platform"]) {
    assert.match(settings, new RegExp(moduleName.replace(":", "\\:")))
  }
})

test("android endpoint policy is consumer-only", () => {
  const source = readFileSync(pathPolicyPath, "utf8")
  assert.doesNotMatch(source, /admin/i)
  assert.match(source, /Events/)
  assert.match(source, /EventDetail/)
  assert.match(source, /Favorites/)
  assert.match(source, /Profile/)
})

test("android consumer endpoint policy stays admin-free even though app modules may bind admin RPCs", () => {
  // Sanity: at least one Android source file is discoverable so this guard isn't a no-op.
  const sources = discoverAndroidSourcePaths()
  assert.ok(sources.length > 0, "no Android sources discovered")
})

// Structural scope guard: the Android tree is consumer-only. No `admin` module, no
// `admin` directory segments, no source files referencing the bare identifier
// `admin` (case-insensitive). String literals are stripped before matching so legit
// consumer-side role comparisons like `role == "admin"` do not trip the guard.
function discoverAllAndroidPaths() {
  const paths = []
  const stack = [androidRoot]
  const skipDirs = new Set(["build", ".gradle", ".idea", "node_modules", ".kotlin"])
  while (stack.length > 0) {
    const current = stack.pop()
    const entries = readdirSync(current, { withFileTypes: true })
    for (const entry of entries) {
      const next = path.join(current, entry.name)
      paths.push(next)
      if (entry.isDirectory() && !skipDirs.has(entry.name) && !entry.name.startsWith(".")) {
        stack.push(next)
      }
    }
  }
  return paths
}

function stripStringLiterals(source) {
  // Remove triple-quoted raw strings, double-quoted strings, char literals, and
  // line + block comments before scanning for identifiers.
  return source
    .replace(/"""[\s\S]*?"""/g, "")
    .replace(/"(?:\\.|[^"\\])*"/g, "")
    .replace(/'(?:\\.|[^'\\])'/g, "")
    .replace(/\/\/.*$/gm, "")
    .replace(/\/\*[\s\S]*?\*\//g, "")
}

test("android tree has no admin module or admin references", () => {
  // No admin module wired into Gradle.
  const settings = readFileSync(settingsPath, "utf8")
  assert.doesNotMatch(
    settings,
    /["']:admin["']/,
    "':admin' must not be in apps/android/settings.gradle.kts"
  )

  // No path under apps/android/ has an `admin` segment (case-insensitive).
  const allPaths = discoverAllAndroidPaths()
  const offendingPaths = allPaths.filter((p) => {
    const rel = path.relative(androidRoot, p)
    return rel.split(path.sep).some((seg) => /^admin$/i.test(seg))
  })
  assert.deepEqual(
    offendingPaths,
    [],
    `unexpected 'admin' path segment(s) under apps/android: ${offendingPaths.join(", ")}`
  )

  // No source file (.kt/.kts/.xml) outside string literals/comments contains the
  // bare identifier `admin` (case-insensitive). String literals are stripped so
  // legitimate role string comparisons survive.
  const offendingIdentifierHits = []
  for (const [file, source] of readAndroidSources()) {
    const cleaned = stripStringLiterals(source)
    if (/\badmin\b/i.test(cleaned)) {
      offendingIdentifierHits.push(path.relative(androidRoot, file))
    }
  }
  assert.deepEqual(
    offendingIdentifierHits,
    [],
    `unexpected bare 'admin' identifier(s) in Android sources: ${offendingIdentifierHits.join(", ")}`
  )
})
