import XCTest
import FECore
@testable import FamilyEvents

final class DeepLinkRouterTests: XCTestCase {
    func testParsesEventURL() throws {
        let url = URL(string: "familyevents://event/evt_42")!
        let result = DeepLinkRouter.route(from: url)
        XCTAssertEqual(result?.tab, .plan)
        XCTAssertEqual(result?.routes, [.event(EventID("evt_42"))])
    }

    func testParsesSavedTabURL() throws {
        let url = URL(string: "familyevents://saved")!
        let result = DeepLinkRouter.route(from: url)
        XCTAssertEqual(result?.tab, .saved)
        XCTAssertEqual(result?.routes, [])
    }

    func testReturnsNilForUnknownScheme() {
        let url = URL(string: "https://example.com/event/x")!
        XCTAssertNil(DeepLinkRouter.route(from: url))
    }

    func testReturnsNilForUnknownHost() {
        let url = URL(string: "familyevents://nope/x")!
        XCTAssertNil(DeepLinkRouter.route(from: url))
    }

    func testReturnsNilWhenEventURLHasTrailingSegment() {
        let url = URL(string: "familyevents://event/evt_42/extra")!
        XCTAssertNil(DeepLinkRouter.route(from: url))
    }

    func testReturnsNilWhenEventURLHasNoID() {
        let url = URL(string: "familyevents://event")!
        XCTAssertNil(DeepLinkRouter.route(from: url))
    }

    func testReturnsNilWhenSavedURLHasTrailingSegment() {
        let url = URL(string: "familyevents://saved/extra")!
        XCTAssertNil(DeepLinkRouter.route(from: url))
    }

    func testParsesPasswordResetURL() throws {
        let url = URL(string: "familyevents://reset-password?token=tok_xyz")!
        let result = DeepLinkRouter.route(from: url)
        XCTAssertEqual(result?.tab, .saved)
        XCTAssertEqual(result?.routes.count, 1)
        if case .resetPassword(let token) = result?.routes.first {
            XCTAssertEqual(token, "tok_xyz")
        } else {
            XCTFail("expected .resetPassword route")
        }
    }
}
