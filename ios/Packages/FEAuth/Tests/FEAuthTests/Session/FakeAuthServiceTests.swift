import XCTest
import FECore
@testable import FEAuth
import FEAuthTesting

final class FakeAuthServiceTests: XCTestCase {
    func testFakeServiceReturnsCannedSession() async throws {
        let fake = FakeAuthService()
        let session = AuthSession(
            userID: UserID("u_1"),
            accessToken: "access",
            refreshToken: "refresh",
            email: "alice@example.com",
            identityProvider: .password
        )
        fake.signInResult = .success(session)
        let got = try await fake.signIn(email: "alice@example.com", password: "pw")
        XCTAssertEqual(got, session)
    }

    func testFakeServiceThrowsConfiguredError() async {
        let fake = FakeAuthService()
        fake.signInResult = .failure(AppError.invalidCredentials)
        do {
            _ = try await fake.signIn(email: "x@y.z", password: "wrong")
            XCTFail("expected throw")
        } catch let error as AppError {
            XCTAssertEqual(error.userMessage, AppError.invalidCredentials.userMessage)
        } catch {
            XCTFail("wrong error type: \(error)")
        }
    }
}
