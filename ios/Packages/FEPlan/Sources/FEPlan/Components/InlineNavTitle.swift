import SwiftUI
import FEDesignSystem

struct InlineNavTitle: ViewModifier {
    func body(content: Content) -> some View {
        #if os(iOS)
        content
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Color.dsBackground, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        #else
        content
        #endif
    }
}
