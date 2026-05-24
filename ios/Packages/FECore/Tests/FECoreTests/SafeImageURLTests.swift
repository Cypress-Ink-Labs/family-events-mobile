import XCTest
@testable import FECore

final class SafeImageURLTests: XCTestCase {
    func test_resolve_returnsFirstHttpsURL() {
        let url = SafeImageURL.resolve(
            images: ["https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg"],
            seed: "e1"
        )
        XCTAssertEqual(url.absoluteString, "https://cdn.example.com/a.jpg")
    }

    func test_resolve_skipsUnsafeSchemes() {
        let url = SafeImageURL.resolve(
            images: ["javascript:alert(1)", "data:image/png;base64,...", "https://cdn.example.com/ok.jpg"],
            seed: "e1"
        )
        XCTAssertEqual(url.absoluteString, "https://cdn.example.com/ok.jpg")
    }

    func test_resolve_returnsPicsumPlaceholder_whenEmpty() {
        let url = SafeImageURL.resolve(images: [], seed: "abc")
        XCTAssertEqual(url.absoluteString, "https://picsum.photos/seed/abc/640/360")
    }

    func test_resolve_returnsPicsumPlaceholder_whenAllUnsafe() {
        let url = SafeImageURL.resolve(
            images: ["", "ftp://nope", "javascript:bad"],
            seed: "abc"
        )
        XCTAssertEqual(url.absoluteString, "https://picsum.photos/seed/abc/640/360")
    }

    func test_resolve_heroAspect_usesLargerDimensions() {
        let url = SafeImageURL.resolve(images: [], seed: "x", aspect: .hero)
        XCTAssertEqual(url.absoluteString, "https://picsum.photos/seed/x/1200/630")
    }

    func test_firstSafe_returnsNilWhenNoneUsable() {
        XCTAssertNil(SafeImageURL.firstSafe(images: []))
        XCTAssertNil(SafeImageURL.firstSafe(images: ["javascript:1"]))
    }

    func test_resolve_trimsWhitespace() {
        let url = SafeImageURL.resolve(images: ["   https://x.com/y.jpg   "], seed: "e")
        XCTAssertEqual(url.absoluteString, "https://x.com/y.jpg")
    }
}
