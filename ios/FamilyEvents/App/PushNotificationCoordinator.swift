import Foundation
import UIKit
import UserNotifications
import FECore
import FEData

@MainActor
final class PushNotificationCoordinator: NSObject, UNUserNotificationCenterDelegate {
    static let shared = PushNotificationCoordinator()

    private var userID: UserID?
    private var registrationRepo: (any MobilePushRegistrationRepo)?
    private var lastDeviceToken: String?
    private var hasRequestedAuthorization = false

    private override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func configure(userID: UserID, registrationRepo: any MobilePushRegistrationRepo) async {
        self.userID = userID
        self.registrationRepo = registrationRepo

        if let lastDeviceToken {
            await register(token: lastDeviceToken)
            return
        }

        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            UIApplication.shared.registerForRemoteNotifications()
        case .notDetermined:
            guard !hasRequestedAuthorization else { return }
            hasRequestedAuthorization = true
            do {
                let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
                if granted {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            } catch {
                return
            }
        case .denied:
            return
        @unknown default:
            return
        }
    }

    func didRegisterForRemoteNotifications(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        lastDeviceToken = token
        Task { await register(token: token) }
    }

    private func register(token: String) async {
        guard let userID, let registrationRepo else { return }
        try? await registrationRepo.registerMobilePushToken(token, platform: .ios, for: userID)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound, .badge]
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Task { @MainActor in
            PushNotificationCoordinator.shared.didRegisterForRemoteNotifications(deviceToken: deviceToken)
        }
    }
}
