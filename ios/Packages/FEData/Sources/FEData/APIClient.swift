import Foundation
import FECore

public struct APIClient: Sendable {
    public let baseURL: URL

    public init(baseURL: URL) {
        self.baseURL = baseURL
    }

    public func url(for path: ConsumerAPIPath) -> URL {
        baseURL.appending(path: path.value)
    }
}
