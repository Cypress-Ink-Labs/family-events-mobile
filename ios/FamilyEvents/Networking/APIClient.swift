import Foundation

struct APIClient {
    let baseURL: URL

    init(baseURL: URL) {
        self.baseURL = baseURL
    }

    func url(for path: ConsumerAPIPath) -> URL {
        baseURL.appending(path: path.value)
    }
}
