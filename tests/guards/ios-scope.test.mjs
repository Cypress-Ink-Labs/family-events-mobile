import assert from "node:assert/strict"
import { existsSync, readFileSync } from "node:fs"
import path from "node:path"
import test from "node:test"

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
const projectYmlPath = path.join(repoRoot, "apps", "ios", "project.yml")
const pathPolicyPath = path.join(repoRoot, "apps", "ios", "FamilyEvents", "Networking", "ConsumerAPIPath.swift")
const scopeTestPath = path.join(repoRoot, "apps", "ios", "FamilyEventsTests", "ConsumerAPIPathTests.swift")

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
  assert.match(testSource, /testAdminPathIsOutOfScope/)
})
