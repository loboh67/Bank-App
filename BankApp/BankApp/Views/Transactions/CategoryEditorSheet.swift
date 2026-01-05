import SwiftUI

struct CategoryEditorSheet: View {
    let categories: [Category]
    let isLoading: Bool
    @Binding var selection: Set<Int>
    @Binding var primaryCategoryId: Int?
    let isSubmitting: Bool
    let errorMessage: String?
    let onCancel: () -> Void
    let onSave: () -> Void

    var body: some View {
        List {
            if isLoading {
                HStack {
                    ProgressView()
                    Text("Loading categories…")
                }
            } else {
                ForEach(categories) { cat in
                    let isSelected = selection.contains(cat.id)
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(cat.name ?? "Unnamed category")
                                .font(.body)
                        }
                        Spacer()
                        if isSelected {
                            Button {
                                primaryCategoryId = cat.id
                            } label: {
                                Image(systemName: primaryCategoryId == cat.id ? "star.fill" : "star")
                                    .foregroundColor(.yellow)
                            }
                            .buttonStyle(.borderless)
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.accentColor)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        toggleSelection(for: cat.id)
                    }
                }
            }

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundColor(.red)
            }
        }
        .navigationTitle("Edit categories")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { onCancel() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { onSave() }
                    .disabled(selection.isEmpty || isSubmitting)
            }
        }
    }

    private func toggleSelection(for id: Int) {
        if selection.contains(id) {
            selection.remove(id)
            if primaryCategoryId == id {
                primaryCategoryId = selection.first
            }
        } else {
            selection.insert(id)
            if primaryCategoryId == nil {
                primaryCategoryId = id
            }
        }
    }
}

