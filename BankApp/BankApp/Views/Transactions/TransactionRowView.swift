import SwiftUI

struct TransactionRowView<Content: View>: View {
    let accountId: Int?
    let transaction: Transaction
    let isEditing: Bool
    let isRenaming: Bool
    let renameError: String?
    let onCancel: () -> Void
    let onSubmit: (String) -> Void
    let rowContent: () -> Content

    @State private var draft: String

    init(
        accountId: Int?,
        transaction: Transaction,
        isEditing: Bool,
        isRenaming: Bool,
        renameError: String?,
        onCancel: @escaping () -> Void,
        onSubmit: @escaping (String) -> Void,
        @ViewBuilder rowContent: @escaping () -> Content
    ) {
        self.accountId = accountId
        self.transaction = transaction
        self.isEditing = isEditing
        self.isRenaming = isRenaming
        self.renameError = renameError
        self.onCancel = onCancel
        self.onSubmit = onSubmit
        self.rowContent = rowContent
        _draft = State(initialValue: transaction.descriptionDisplay ?? transaction.displayTitle ?? "")
    }

    var body: some View {
        Group {
            if isEditing {
                editingView
            } else {
                NavigationLink {
                    TransactionDetailView(transaction: transaction, accountId: accountId)
                } label: {
                    rowContent()
                }
                .disabled(isRenaming)
            }
        }
        .onChange(of: isEditing) { _, nowEditing in
            if nowEditing {
                draft = transaction.descriptionDisplay ?? transaction.displayTitle ?? ""
            }
        }
    }

    private var editingView: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Button {
                    onCancel()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.borderless)

                TextField("New description", text: $draft)
                    .textFieldStyle(.plain)
                    .font(.headline.weight(.semibold))

                Button {
                    onSubmit(draft)
                } label: {
                    if isRenaming {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.title3)
                    }
                }
                .foregroundColor(.accentColor)
                .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isRenaming)
                .buttonStyle(.borderless)
            }

            if let renameError {
                Text(renameError)
                    .font(.footnote)
                    .foregroundColor(.red)
            }
        }
        .padding(.vertical, 6)
    }
}

