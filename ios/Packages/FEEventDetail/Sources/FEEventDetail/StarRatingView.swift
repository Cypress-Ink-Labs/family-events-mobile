import SwiftUI

/// Tap-to-rate 5-star control. Stateless — caller owns `score` binding.
public struct StarRatingView: View {
    @Binding public var score: Int
    public let isDisabled: Bool
    public let onChange: ((Int) -> Void)?

    public init(score: Binding<Int>, isDisabled: Bool = false, onChange: ((Int) -> Void)? = nil) {
        self._score = score
        self.isDisabled = isDisabled
        self.onChange = onChange
    }

    public var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { value in
                Button {
                    score = value
                    onChange?(value)
                } label: {
                    Image(systemName: value <= score ? "star.fill" : "star")
                        .font(.title3)
                        .foregroundStyle(value <= score ? Color.yellow : Color.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(value) star\(value == 1 ? "" : "s")")
            }
        }
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.5 : 1.0)
    }
}
