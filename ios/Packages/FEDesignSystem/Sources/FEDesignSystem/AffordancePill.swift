import SwiftUI

/// Visual grammar for kid-context affordance signals.
/// Always uses `dsAccentKid` / `dsAccentKidSoft` — never a raw color.
///
/// Usage:
/// ```swift
/// AffordancePill(variant: .free)
/// AffordancePill(variant: .age, label: "3–8y")
/// AffordancePill(variant: .stroller, style: .compact)
/// ```
public struct AffordancePill: View {
    public enum Variant {
        case age
        case free
        case stroller
        case indoorBackup
        case walkable
        case bathroomOnSite

        var defaultLabel: String {
            switch self {
            case .age: return "All ages"
            case .free: return "Free"
            case .stroller: return "Stroller OK"
            case .indoorBackup: return "Indoor backup"
            case .walkable: return "Walkable"
            case .bathroomOnSite: return "Restrooms"
            }
        }

        var systemImage: String {
            switch self {
            case .age: return "figure.and.child.holdinghands"
            case .free: return "face.smiling"
            case .stroller: return "stroller"
            case .indoorBackup: return "house"
            case .walkable: return "mappin"
            case .bathroomOnSite: return "door.right.hand.open"
            }
        }

        // age + free use solid kid; others use soft
        var isSolid: Bool {
            switch self {
            case .age, .free: return true
            default: return false
            }
        }
    }

    public enum Style {
        case standard  // 44pt min touch target
        case compact   // dense — use inside a parent that is already tappable
    }

    private let variant: Variant
    private let label: String?
    private let style: Style

    public init(variant: Variant, label: String? = nil, style: Style = .standard) {
        self.variant = variant
        self.label = label
        self.style = style
    }

    public var body: some View {
        HStack(spacing: 4) {
            Image(systemName: variant.systemImage)
                .font(style == .compact ? .system(size: 9) : .system(size: 11))
                .accessibilityHidden(true)
            Text(label ?? variant.defaultLabel)
                .font(style == .compact ? .system(size: 10, weight: .medium) : .system(size: 12, weight: .medium))
        }
        .padding(.horizontal, style == .compact ? 6 : 10)
        .padding(.vertical, style == .compact ? 2 : 6)
        .frame(minWidth: style == .compact ? 0 : 44, minHeight: style == .compact ? 0 : 44)
        .background(variant.isSolid ? Color.dsAccentKid : Color.dsAccentKidSoft)
        .foregroundStyle(Color.dsTextPrimary)
        .clipShape(Capsule())
        .accessibilityLabel(label ?? variant.defaultLabel)
    }
}

#if DEBUG
#Preview("AffordancePill — all variants") {
    VStack(spacing: 16) {
        ForEach(
            [
                AffordancePill.Variant.age,
                .free,
                .stroller,
                .indoorBackup,
                .walkable,
                .bathroomOnSite,
            ],
            id: \.defaultLabel
        ) { variant in
            HStack(spacing: 8) {
                AffordancePill(variant: variant)
                AffordancePill(variant: variant, style: .compact)
            }
        }
    }
    .padding()
    .background(Color.dsBackground)
}
#endif
