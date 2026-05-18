import AppIntents
import Foundation

public enum AppIntentsRegistry {
    public static let registered: [String] = [
        "OpenDestinationIntent",
        "SearchEventsIntent",
        "OpenEventIntent",
        "SaveEventIntent",
        "AddEventToCalendarIntent",
        "RateEventIntent",
        "AddCommentIntent",
        "OpenAdminSectionIntent",
        "AdminUpdateEventIntent",
        "AdminModerateCommentIntent",
        "AdminCreateInviteIntent",
        "AdminRevokeInviteIntent",
        "AdminRunSourceIntent",
        "AdminRunCronIntent",
    ]
}

public enum FamilyEventsDestination: String, CaseIterable, AppEnum {
    case plan
    case explore
    case saved
    case calendar
    case profile

    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Destination")
    public static var caseDisplayRepresentations: [FamilyEventsDestination: DisplayRepresentation] = [
        .plan: "Plan",
        .explore: "Explore",
        .saved: "Saved",
        .calendar: "Calendar",
        .profile: "Profile",
    ]
}

public enum FamilyEventsAdminSection: String, CaseIterable, AppEnum {
    case dashboard
    case sources
    case events
    case cities
    case comments
    case ratings
    case access
    case invites
    case logs
    case crons

    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Admin Section")
    public static var caseDisplayRepresentations: [FamilyEventsAdminSection: DisplayRepresentation] = [
        .dashboard: "Dashboard",
        .sources: "Sources",
        .events: "Events",
        .cities: "Cities",
        .comments: "Comments",
        .ratings: "Ratings",
        .access: "Access",
        .invites: "Invites",
        .logs: "Logs",
        .crons: "Crons",
    ]
}

public struct FamilyEventsEventEntity: AppEntity {
    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Event")
    public static var defaultQuery = FamilyEventsEventQuery()

    public let id: String
    public let title: String

    public init(id: String, title: String? = nil) {
        self.id = id
        self.title = title ?? id
    }

    public var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)")
    }
}

public struct FamilyEventsEventQuery: EntityQuery {
    public init() {}

    public func entities(for identifiers: [String]) async throws -> [FamilyEventsEventEntity] {
        identifiers.map { FamilyEventsEventEntity(id: $0) }
    }

    public func suggestedEntities() async throws -> [FamilyEventsEventEntity] {
        []
    }
}

public struct FamilyEventsSourceEntity: AppEntity {
    public static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Source")
    public static var defaultQuery = FamilyEventsSourceQuery()

    public let id: String
    public let name: String

    public init(id: String, name: String? = nil) {
        self.id = id
        self.name = name ?? id
    }

    public var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

public struct FamilyEventsSourceQuery: EntityQuery {
    public init() {}

    public func entities(for identifiers: [String]) async throws -> [FamilyEventsSourceEntity] {
        identifiers.map { FamilyEventsSourceEntity(id: $0) }
    }

    public func suggestedEntities() async throws -> [FamilyEventsSourceEntity] {
        []
    }
}

public struct OpenDestinationIntent: AppIntent {
    public static var title: LocalizedStringResource = "Open Family Events"
    public static var description = IntentDescription("Open a main Family Events destination.")
    public static var openAppWhenRun = true

    @Parameter(title: "Destination")
    public var destination: FamilyEventsDestination

    public init() {
        destination = .plan
    }

    public init(destination: FamilyEventsDestination) {
        self.destination = destination
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening \(destination.rawValue).")
    }
}

public struct SearchEventsIntent: AppIntent {
    public static var title: LocalizedStringResource = "Search Family Events"
    public static var description = IntentDescription("Open event search with a query.")
    public static var openAppWhenRun = true

    @Parameter(title: "Search")
    public var query: String

    public init() {
        query = ""
    }

    public init(query: String) {
        self.query = query
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: query.isEmpty ? "Opening event search." : "Searching for \(query).")
    }
}

public struct OpenEventIntent: AppIntent {
    public static var title: LocalizedStringResource = "Open Event"
    public static var description = IntentDescription("Open a Family Events event.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    public init() {
        event = FamilyEventsEventEntity(id: "")
    }

    public init(event: FamilyEventsEventEntity) {
        self.event = event
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening \(event.title).")
    }
}

public struct SaveEventIntent: AppIntent {
    public static var title: LocalizedStringResource = "Save Event"
    public static var description = IntentDescription("Save or unsave an event.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    @Parameter(title: "Saved")
    public var saved: Bool

    public init() {
        event = FamilyEventsEventEntity(id: "")
        saved = true
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: saved ? "Saving \(event.title)." : "Removing \(event.title) from saved events.")
    }
}

public struct AddEventToCalendarIntent: AppIntent {
    public static var title: LocalizedStringResource = "Add Event to Calendar"
    public static var description = IntentDescription("Add a Family Events event to Calendar.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    public init() {
        event = FamilyEventsEventEntity(id: "")
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Adding \(event.title) to Calendar.")
    }
}

public struct RateEventIntent: AppIntent {
    public static var title: LocalizedStringResource = "Rate Event"
    public static var description = IntentDescription("Rate a Family Events event.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    @Parameter(title: "Score")
    public var score: Int

    public init() {
        event = FamilyEventsEventEntity(id: "")
        score = 5
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Rating \(event.title) \(max(1, min(score, 5))).")
    }
}

public struct AddCommentIntent: AppIntent {
    public static var title: LocalizedStringResource = "Add Event Comment"
    public static var description = IntentDescription("Add a comment to a Family Events event.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    @Parameter(title: "Comment")
    public var body: String

    public init() {
        event = FamilyEventsEventEntity(id: "")
        body = ""
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Adding a comment to \(event.title).")
    }
}

public struct OpenAdminSectionIntent: AppIntent {
    public static var title: LocalizedStringResource = "Open Admin"
    public static var description = IntentDescription("Open a Family Events admin section.")
    public static var openAppWhenRun = true

    @Parameter(title: "Section")
    public var section: FamilyEventsAdminSection

    public init() {
        section = .dashboard
    }

    public init(section: FamilyEventsAdminSection) {
        self.section = section
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening admin \(section.rawValue).")
    }
}

public struct AdminUpdateEventIntent: AppIntent {
    public static var title: LocalizedStringResource = "Update Admin Event"
    public static var description = IntentDescription("Open an authorized admin event update.")
    public static var openAppWhenRun = true

    @Parameter(title: "Event")
    public var event: FamilyEventsEventEntity

    public init() {
        event = FamilyEventsEventEntity(id: "")
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening admin edit for \(event.title).")
    }
}

public struct AdminModerateCommentIntent: AppIntent {
    public static var title: LocalizedStringResource = "Moderate Comment"
    public static var description = IntentDescription("Open an authorized admin comment moderation action.")
    public static var openAppWhenRun = true

    @Parameter(title: "Comment ID")
    public var commentID: String

    @Parameter(title: "Approved")
    public var approved: Bool

    @Parameter(title: "Flagged")
    public var flagged: Bool

    public init() {
        commentID = ""
        approved = true
        flagged = false
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening comment moderation.")
    }
}

public struct AdminCreateInviteIntent: AppIntent {
    public static var title: LocalizedStringResource = "Create Invite"
    public static var description = IntentDescription("Create an authorized admin invite code.")
    public static var openAppWhenRun = true

    @Parameter(title: "Note")
    public var note: String

    public init() {
        note = ""
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening invite creation.")
    }
}

public struct AdminRevokeInviteIntent: AppIntent {
    public static var title: LocalizedStringResource = "Revoke Invite"
    public static var description = IntentDescription("Revoke an authorized admin invite code.")
    public static var openAppWhenRun = true

    @Parameter(title: "Invite ID")
    public var inviteID: String

    public init() {
        inviteID = ""
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening invite revocation.")
    }
}

public struct AdminRunSourceIntent: AppIntent {
    public static var title: LocalizedStringResource = "Run Source"
    public static var description = IntentDescription("Run an authorized admin source scrape.")
    public static var openAppWhenRun = true

    @Parameter(title: "Source")
    public var source: FamilyEventsSourceEntity

    public init() {
        source = FamilyEventsSourceEntity(id: "")
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening source run for \(source.name).")
    }
}

public struct AdminRunCronIntent: AppIntent {
    public static var title: LocalizedStringResource = "Run Cron"
    public static var description = IntentDescription("Run an authorized admin cron action.")
    public static var openAppWhenRun = true

    @Parameter(title: "Job")
    public var jobName: String

    public init() {
        jobName = "due-scrapes"
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Opening cron run for \(jobName).")
    }
}

public struct FamilyEventsShortcuts: AppShortcutsProvider {
    public static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenDestinationIntent(destination: .plan),
            phrases: ["Open \(.applicationName) plan"],
            shortTitle: "Open Plan",
            systemImageName: "calendar.badge.clock"
        )
        AppShortcut(
            intent: OpenDestinationIntent(destination: .explore),
            phrases: ["Search \(.applicationName)"],
            shortTitle: "Explore",
            systemImageName: "sparkle.magnifyingglass"
        )
        AppShortcut(
            intent: OpenAdminSectionIntent(section: .dashboard),
            phrases: ["Open \(.applicationName) admin"],
            shortTitle: "Admin",
            systemImageName: "person.badge.key"
        )
    }
}
