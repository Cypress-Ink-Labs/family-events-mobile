import SwiftUI
import FECore
import FEData
import FEDesignSystem

struct EventDetailCommentSection: View {
    let comments: [CommentDTO]
    let commentError: String?
    let isInFlight: Bool
    @Binding var draftComment: String
    let onSubmit: (String) async -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Comments")
                .font(.headline)
                .foregroundStyle(Color.dsTextPrimary)
            commentForm
            if let err = commentError {
                Text(err)
                    .font(.caption)
                    .foregroundStyle(Color.dsError)
            }
            if comments.isEmpty {
                Text("No comments yet. Be the first.")
                    .font(.subheadline)
                    .foregroundStyle(Color.dsTextMuted)
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(comments) { comment in
                        EventDetailCommentRow(comment: comment)
                    }
                }
            }
        }
    }

    private var commentForm: some View {
        HStack(alignment: .top, spacing: 8) {
            TextField("Share what you're hoping for…", text: $draftComment, axis: .vertical)
                .lineLimit(1...4)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color.dsSurfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            Button {
                Task { await onSubmit(draftComment) }
            } label: {
                Image(systemName: "paperplane.fill")
                    .padding(10)
                    .background(Color.dsAccentPrimary)
                    .foregroundStyle(Color.dsSurface)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .disabled(isInFlight || draftComment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .accessibilityLabel("Post comment")
        }
    }
}

struct EventDetailCommentRow: View {
    let comment: CommentDTO

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Image(systemName: "person.crop.circle.fill")
                    .font(.title3)
                    .foregroundStyle(Color.dsTextMuted)
                    .accessibilityHidden(true)
                Text(comment.authorDisplayName ?? "Anonymous")
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Text(Self.dateFormatter.string(from: comment.createdAt))
                    .font(.caption)
                    .foregroundStyle(Color.dsTextMuted)
            }
            Text(comment.body)
                .font(.body)
                .foregroundStyle(Color.dsTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.dsSurfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()
}
