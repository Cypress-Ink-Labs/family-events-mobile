import Foundation
import FECore

public enum AppRoute: Hashable, Sendable {
    case event(EventID)
    case city(CityID)
    case profile
    case settings
}
