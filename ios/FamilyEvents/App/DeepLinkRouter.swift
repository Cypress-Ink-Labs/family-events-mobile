import Foundation
import FECore

public enum DeepLinkRouter {
    public struct Result: Equatable {
        public let tab: AppTab
        public let routes: [AppRoute]
    }

    public static func route(from url: URL) -> Result? {
        guard url.scheme == "familyevents" else { return nil }
        let host = url.host ?? ""
        switch host {
        case "event":
            let id = url.lastPathComponent
            guard !id.isEmpty, id != "event" else { return nil }
            return Result(tab: .plan, routes: [.event(EventID(id))])
        case "saved":
            return Result(tab: .saved, routes: [])
        default:
            return nil
        }
    }
}
