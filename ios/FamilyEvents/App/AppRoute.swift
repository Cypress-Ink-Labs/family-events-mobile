import Foundation
import FECore

enum AppRoute: Hashable, Sendable {
    case event(EventID)
    case city(CityID)
    case profile
    case settings
    case resetPassword(token: String)
}
