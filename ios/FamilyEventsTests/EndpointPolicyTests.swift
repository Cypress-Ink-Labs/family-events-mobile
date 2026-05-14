import XCTest

final class EndpointPolicyTests: XCTestCase {
    func testNoAdminPathReferencesInIOSSources() throws {
        let fileManager = FileManager.default
        let repoRoot = try repoRootURL()
        let iosRoot = repoRoot.appending(path: "apps/ios")
        let searchRoots = [
            iosRoot.appending(path: "FamilyEvents"),
            iosRoot.appending(path: "Packages"),
        ]

        var offenders: [String] = []
        for root in searchRoots {
            let enumerator = fileManager.enumerator(at: root, includingPropertiesForKeys: nil)
            while let url = enumerator?.nextObject() as? URL {
                guard url.pathExtension == "swift" else { continue }
                // Skip SPM build artifacts
                if url.pathComponents.contains(".build") { continue }
                let contents = try String(contentsOf: url, encoding: .utf8)
                if contents.contains("/api/v1/admin") || contents.contains("\"/admin/") {
                    offenders.append(url.path)
                }
            }
        }
        XCTAssertTrue(offenders.isEmpty, "admin-path references found in: \(offenders.joined(separator: ", "))")
    }

    private func repoRootURL() throws -> URL {
        // Walk up from this source file until we find a `pnpm-workspace.yaml`.
        var url = URL(fileURLWithPath: #filePath)
        while url.pathComponents.count > 1 {
            url.deleteLastPathComponent()
            let marker = url.appending(path: "pnpm-workspace.yaml")
            if FileManager.default.fileExists(atPath: marker.path) {
                return url
            }
        }
        throw NSError(domain: "EndpointPolicyTests", code: 1, userInfo: [NSLocalizedDescriptionKey: "repo root not found"])
    }
}
