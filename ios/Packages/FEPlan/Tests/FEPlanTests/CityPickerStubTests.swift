import XCTest
@testable import FEPlan

@MainActor
final class CityPickerStubTests: XCTestCase {
    func testOnDismissCallbackIsRetained() {
        var dismissed = false
        let stub = CityPickerStub(onDismiss: { dismissed = true })
        stub.onDismiss()
        XCTAssertTrue(dismissed)
    }
}
