import XCTest
@testable import FEData

final class RepositoryTests: XCTestCase {
    func testFakeRepoConformsToRepository() async throws {
        let repo = FakeRepository()
        try await repo.refresh()
        XCTAssertEqual(repo.refreshCount, 1)
    }

    func testInMemoryModelContainerBuilds() throws {
        let container = try AppModelContainer.makeInMemory()
        XCTAssertNotNil(container)
    }
}

private final class FakeRepository: Repository, @unchecked Sendable {
    var refreshCount = 0
    func refresh() async throws {
        refreshCount += 1
    }
}
