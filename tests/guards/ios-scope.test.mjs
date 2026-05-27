import assert from "node:assert/strict"
import { existsSync, readFileSync, readdirSync } from "node:fs"
import path from "node:path"
import test from "node:test"

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
const projectYmlPath = path.join(repoRoot, "apps", "ios", "project.yml")
const pathPolicyPath = path.join(repoRoot, "apps", "ios", "Packages", "FECore", "Sources", "FECore", "ConsumerAPIPath.swift")
const scopeTestPath = path.join(repoRoot, "apps", "ios", "Packages", "FECore", "Tests", "FECoreTests", "ConsumerAPIPathTests.swift")
const iosPackagesRoot = path.join(repoRoot, "apps", "ios", "Packages")

test("iOS workspace includes XcodeGen project spec", () => {
  assert.equal(existsSync(projectYmlPath), true)
  const projectYml = readFileSync(projectYmlPath, "utf8")
  assert.match(projectYml, /^name: FamilyEvents$/m)
  assert.match(projectYml, /^targets:$/m)
})

test("iOS endpoint policy is consumer-only and excludes admin", () => {
  assert.equal(existsSync(pathPolicyPath), true)
  const source = readFileSync(pathPolicyPath, "utf8")
  assert.doesNotMatch(source, /admin/i)
  assert.match(source, /case events/)
  assert.match(source, /case eventDetail/)
  assert.match(source, /case favorites/)
  assert.match(source, /case profile/)
})

test("iOS tests explicitly enforce admin out-of-scope policy", () => {
  assert.equal(existsSync(scopeTestPath), true)
  const testSource = readFileSync(scopeTestPath, "utf8")
  assert.match(testSource, /testNoAdminPathsExposed/)
})

// Structural scope guard for iOS — mirrors the Android tree guard. Catches a
// re-introduction of an admin package (e.g. FEAdmin/, Sources/Admin*/, etc.)
// at Node-test time before Swift compilation ever runs, so non-macOS CI sees
// the violation immediately.
function discoverAllIosPackagePaths() {
  if (!existsSync(iosPackagesRoot)) return []
  const paths = []
  const stack = [iosPackagesRoot]
  const skipDirs = new Set([".build", "build", ".swiftpm", "DerivedData", "node_modules"])
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

test("iOS Packages tree has no admin package or admin path segments", () => {
  const allPaths = discoverAllIosPackagePaths()
  const offendingPaths = allPaths.filter((p) => {
    const rel = path.relative(iosPackagesRoot, p)
    return rel
      .split(path.sep)
      .some((seg) => /admin/i.test(seg))
  })
  assert.deepEqual(
    offendingPaths,
    [],
    `unexpected 'admin' path segment(s) under apps/ios/Packages: ${offendingPaths.join(", ")}`
  )
})
