import SwiftUI

struct HomeView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Text("Family Events")
                    .font(.title2)
                    .fontWeight(.semibold)
                Text("Consumer experience only")
                    .foregroundStyle(.secondary)
            }
            .padding()
            .navigationTitle("Home")
        }
    }
}
