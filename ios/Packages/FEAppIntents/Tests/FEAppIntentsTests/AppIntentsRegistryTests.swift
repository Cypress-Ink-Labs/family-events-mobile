import XCTest
@testable import FEAppIntents

final class AppIntentsRegistryTests: XCTestCase {
    func testRegistryExposesConsumerAndAdminIntents() {
        XCTAssertTrue(AppIntentsRegistry.registered.contains("OpenDestinationIntent"))
        XCTAssertTrue(AppIntentsRegistry.registered.contains("OpenEventIntent"))
        XCTAssertTrue(AppIntentsRegistry.registered.contains("OpenAdminSectionIntent"))
        XCTAssertTrue(AppIntentsRegistry.registered.contains("AdminRunCronIntent"))
    }

    func testDestinationCasesMatchNativeTabsAndRoutes() {
        XCTAssertEqual(FamilyEventsDestination.allCases.map(\.rawValue), [
            "plan",
            "explore",
            "saved",
            "calendar",
            "profile",
        ])
    }

    func testAdminSectionsMatchWebBackOffice() {
        XCTAssertEqual(FamilyEventsAdminSection.allCases.map(\.rawValue), [
            "dashboard",
            "sources",
            "events",
            "cities",
            "comments",
            "ratings",
            "access",
            "invites",
            "logs",
            "crons",
        ])
    }
}
