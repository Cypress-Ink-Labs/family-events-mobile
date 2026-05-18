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
        case "tab":
            guard segments.count == 1, let tab = AppTab(rawValue: segments[0]) else { return nil }
            return Result(tab: tab, routes: [])
        case "admin":
            return Result(tab: .admin, routes: [.admin(section: segments.first)])
        case "saved":
            guard segments.isEmpty else { return nil }
            return Result(tab: .saved, routes: [])
        case "reset-password":
            let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
            guard let token = comps?.queryItems?.first(where: { $0.name == "token" })?.value,
                  !token.isEmpty else { return nil }
            return Result(tab: .saved, routes: [.resetPassword(token: token)])
        default:
            return nil
        }
    }
}
