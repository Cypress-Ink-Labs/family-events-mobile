import SwiftUI
import FECore
import FEDesignSystem

struct PlanEmptyStateView: View {
    let hasCitySet: Bool
    let lastEmptyRefresh: Bool
    let onSetCity: () -> Void

    var body: some View {
        if !hasCitySet && lastEmptyRefresh {
            noCityState
        } else {
            noEventsState
        }
    }

    private var noCityState: some View {
        VStack(spacing: 16) {
            Image(systemName: "mappin.slash")
                .font(.system(size: 48))
                .foregroundStyle(Color.dsTextMuted)
            Text("No location set")
                .font(.dsTitleLg)
            Text("Pick a city to see family events nearby.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
                .font(.dsBody)
            Button("Set your city", action: onSetCity)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }

    private var noEventsState: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 48))
                .foregroundStyle(Color.dsTextMuted)
            Text("No family plans found nearby in the next 7 days.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.dsTextMuted)
                .font(.dsBody)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 48)
    }
}
