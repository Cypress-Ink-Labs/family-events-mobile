import XCTest
@testable import FECore

@MainActor
final class ScenePhaseRefreshControllerTests: XCTestCase {
    private final class Spy: Refreshable {
        var calls = 0
        func refresh() async {
            calls += 1
        }
    }

    func test_coldStart_active_doesNotTriggerRefresh() async {
        let controller = ScenePhaseRefreshController()
        let spy = Spy()
        controller.bind(spy)
        controller.scenePhaseChanged(.active)
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(spy.calls, 0, "cold-start .active should defer to .task")
    }

    func test_backgroundThenActive_triggersRefresh() async {
        let controller = ScenePhaseRefreshController()
        let spy = Spy()
        controller.bind(spy)
        controller.scenePhaseChanged(.background)
        controller.scenePhaseChanged(.active)
        try? await Task.sleep(nanoseconds: 100_000_000)
        XCTAssertEqual(spy.calls, 1)
    }

    func test_inactive_isIgnored() async {
        let controller = ScenePhaseRefreshController()
        let spy = Spy()
        controller.bind(spy)
        controller.scenePhaseChanged(.inactive)
        controller.scenePhaseChanged(.active)
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(spy.calls, 0)
    }

    func test_bindNil_swallowsRefresh() async {
        let controller = ScenePhaseRefreshController()
        let spy = Spy()
        controller.bind(spy)
        controller.bind(nil)
        controller.scenePhaseChanged(.background)
        controller.scenePhaseChanged(.active)
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(spy.calls, 0)
    }

    func test_triggerRefresh_callsBound() async {
        let controller = ScenePhaseRefreshController()
        let spy = Spy()
        controller.bind(spy)
        controller.triggerRefresh()
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(spy.calls, 1)
    }

    func test_rebind_replacesPrevious() async {
        let controller = ScenePhaseRefreshController()
        let first = Spy()
        let second = Spy()
        controller.bind(first)
        controller.bind(second)
        controller.scenePhaseChanged(.background)
        controller.scenePhaseChanged(.active)
        try? await Task.sleep(nanoseconds: 50_000_000)
        XCTAssertEqual(first.calls, 0)
        XCTAssertEqual(second.calls, 1)
    }
}
