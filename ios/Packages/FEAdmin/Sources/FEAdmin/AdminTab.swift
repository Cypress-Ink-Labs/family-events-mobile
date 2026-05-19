import SwiftUI

public struct AdminTab: View {
    public init() {}

    public var body: some View {
        NavigationStack {
            List {
                Section("Admin") {
                    NavigationLink("Dashboard") { AdminDashboardSection() }
                    NavigationLink("Events") { AdminEventsSection() }
                    NavigationLink("Sources") { AdminSourcesSection() }
                    NavigationLink("Invites") { AdminInvitesSection() }
                    NavigationLink("Comments") { AdminCommentsSection() }
                    NavigationLink("Cities") { AdminCitiesSection() }
                    NavigationLink("Ratings") { AdminRatingsSection() }
                    NavigationLink("Access") { AdminAccessSection() }
                    NavigationLink("Logs") { AdminLogsSection() }
                    NavigationLink("Crons") { AdminCronsSection() }
                }
            }
            .navigationTitle("Admin")
        }
    }
}
