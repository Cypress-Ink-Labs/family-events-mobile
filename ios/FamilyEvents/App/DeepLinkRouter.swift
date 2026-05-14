import Foundation
import FECore

enum DeepLinkRouter {
    struct Result: Equatable {
        let tab: AppTab
        let routes: [AppRoute]
    }

    static func route(from url: URL) -> Result? {
        guard url.scheme == "familyevents" else { return nil }
        let host = url.host ?? ""
        let segments = url.pathComponents.filter { $0 != "/" }

        switch host {
        case "event":
            guard segments.count == 1, !segments[0].isEmpty else { return nil }
            return Result(tab: .plan, routes: [.event(EventID(segments[0]))])
        case "saved":
            guard segments.isEmpty else { return nil }
            return Result(tab: .saved, routes: [])
        default:
            return nil
        }
    }
}
