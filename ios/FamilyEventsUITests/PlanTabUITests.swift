import XCTest

/// D10 critical-path UI coverage. The two cases enumerated in the eng-review
/// decision require harness pieces that ship in M3.5:
///   - a sign-in fixture (the real flow is Sign in with Apple, which can't
///     run unattended in CI),
///   - a simulator-location pre-set step (`simctl location set ...`),
///   - an env-var injection toggle in `FamilyEventsApp.bootstrap` to swap a
///     fake PlanRepository in when `FE_PLAN_FAKE_ERROR=1`.
///
/// Until that harness lands the cases are surfaced as `XCTSkip`. The
/// sanity check (`testAppLaunchesPlanTab`) still runs so the bundle exercises
/// the launch path end-to-end on every build.
final class PlanTabUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAppLaunchesPlanTab() throws {
        let app = XCUIApplication()
        app.launch()
        // The Plan tab item should be visible regardless of whether the
        // user is signed in (auth root in signed-out, TabView in signed-in).
        // We assert only the launch — deeper UI hierarchy depends on
        // fixtures the M3.5 harness will introduce.
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
    }

    func testHeroCardRendersAfterSignIn() throws {
        throw XCTSkip("Requires SIWA sign-in fixture + simctl location set; tracked in M3.5 alongside the test-harness env-var.")
    }

    func testInjectedRPCErrorShowsRetryThenRecovers() throws {
        throw XCTSkip("Requires FE_PLAN_FAKE_ERROR env-var hook in FamilyEventsApp.bootstrap; tracked in M3.5.")
    }
}
