import XCTest

final class EndpointPolicyTests: XCTestCase {
    func testNoAdminPathReferencesInIOSSources() throws {
        let fileManager = FileManager.default
        // This file lives at ios/FamilyEventsTests/EndpointPolicyTests.swift, so the
        // iOS root is two directories up. (Standalone repo: iOS sources are at ios/,
        // not the monorepo-era apps/ios/, and there is no pnpm-workspace.yaml to anchor on.)
        let iosRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent() // FamilyEventsTests/
            .deletingLastPathComponent() // ios/
        let searchRoots = [
            iosRoot.appending(path: "FamilyEvents"),
            iosRoot.appending(path: "Packages"),
        ]

        var offenders: [String] = []
        for root in searchRoots {
            guard let enumerator = fileManager.enumerator(at: root, includingPropertiesForKeys: nil) else {
                continue
            }
            while let url = enumerator.nextObject() as? URL {
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
}
