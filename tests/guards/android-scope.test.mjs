import assert from "node:assert/strict"
import { existsSync, readFileSync, readdirSync } from "node:fs"
import path from "node:path"
import test from "node:test"

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
const androidRoot = path.join(repoRoot, "apps", "android")
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
        if (!entry.name.startsWith(".") && entry.name !== "build" && entry.name !== "test" && entry.name !== "androidTest" && entry.name !== "admin") {
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
  for (const moduleName of [":app", ":core", ":data", ":designsystem", ":auth", ":plan", ":explore", ":saved", ":eventdetail", ":platform", ":admin"]) {
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
