import SwiftUI

struct TransactionDetailView: View {
    let accountId: Int?
    @State private var transaction: Transaction
    @State private var categories: [TransactionCategory]
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var availableCategories: [Category] = []
    @State private var isEditingCategories = false
    @State private var isFetchingCategoryList = false
    @State private var isSubmittingCategories = false
    @State private var categorySelection: Set<Int> = []
    @State private var primaryCategoryId: Int?
    @State private var categoryError: String?
    
    @State private var isEditingDescription = false
    @State private var isRenaming = false
    @State private var renameError: String?
    @State private var descriptionDraft: String
    @State private var relatedTotal: CategoryTotal?
    @State private var isLoadingRelatedTotal = false
    @State private var relatedTotalError: String?
    @State private var showRenameScopeDialog = false
    @State private var pendingRenameDraft: String?
    
    private var isPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] != nil
    }

    private enum RelatedContext {
        case merchant(id: String, name: String?)
        case description(raw: String, display: String?)
    }

    private var relatedContext: RelatedContext? {
        if let merchantId = transaction.merchantId ?? transaction.merchant?.id {
            return .merchant(id: merchantId, name: transaction.merchant?.name ?? transaction.displayTitle)
        }
        if let raw = transaction.descriptionRaw ?? transaction.displayDescription {
            return .description(raw: raw, display: transaction.displayDescription ?? raw)
        }
        return nil
    }

    init(transaction: Transaction, accountId: Int? = nil, initialCategories: [TransactionCategory] = []) {
        self.accountId = accountId
        _transaction = State(initialValue: transaction)
        _categories = State(initialValue: initialCategories)
        _categorySelection = State(initialValue: Set(initialCategories.compactMap { $0.categoryId }))
        _primaryCategoryId = State(initialValue: initialCategories.first(where: { $0.primary == true })?.categoryId ?? initialCategories.first?.categoryId)
        _descriptionDraft = State(initialValue: transaction.descriptionDisplay ?? transaction.displayTitle ?? "")
    }

    var body: some View {
        List {
            Section {
                headerView
                    .listRowSeparator(.hidden)
            }
            .listRowBackground(Color(.systemGroupedBackground))

            relatedTotalSection

            Section("Description") {
                descriptionEditor
            }

            Section {
                if isLoading {
                    HStack {
                        ProgressView()
                        Text("Loading categories...")
                    }
                } else if let errorMessage {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Could not load categories")
                            .font(.headline)
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        Button("Retry") {
                            Task { await loadCategories() }
                        }
                    }
                } else if categories.isEmpty {
                    Text("No categories found")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(categories) { cat in
                        categoryNavigationLink(for: cat)
                    }
                }
            } header: {
                HStack {
                    Text("Categories")
                    Spacer()
                    Button("Edit") {
                        isEditingCategories = true
                        Task { await ensureCategoriesLoaded() }
                    }
                    .font(.subheadline.weight(.semibold))
                    .buttonStyle(.borderless)
                    .disabled(isLoading || isSubmittingCategories)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isEditingCategories) {
            NavigationStack {
                CategoryEditorSheet(
                    categories: availableCategories,
                    isLoading: isFetchingCategoryList,
                    selection: $categorySelection,
                    primaryCategoryId: $primaryCategoryId,
                    isSubmitting: isSubmittingCategories,
                    errorMessage: categoryError,
                    onCancel: { isEditingCategories = false },
                    onSave: { Task { await submitCategories() } }
                )
            }
            .presentationDetents([.medium, .large])
        }
        .task {
            guard !isPreview else { return }
            await loadCategories()
            await loadRelatedTotal()
        }
        .alert(
            "Apply to matching transactions?",
            isPresented: $showRenameScopeDialog
        ) {
            Button("Just this transaction") {
                showRenameScopeDialog = false
                Task { await saveDescription(applyToAll: false) }
            }
            if canBulkRename {
                Button("All with the same description") {
                    showRenameScopeDialog = false
                    Task { await saveDescription(applyToAll: true) }
                }
            }
            Button("Cancel", role: .cancel) {
                showRenameScopeDialog = false
            }
        } message: {
            Text("Choose whether to rename only this transaction or every transaction with the same original description.")
        }
    }

    @ViewBuilder
    private var descriptionEditor: some View {
        if isEditingDescription {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Button {
                        cancelEditing()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.borderless)
                    TextField("New description", text: $descriptionDraft)
                        .textFieldStyle(.plain)
                        .font(.headline.weight(.semibold))
                    Button {
                        pendingRenameDraft = descriptionDraft
                        showRenameScopeDialog = true
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
                    .disabled(descriptionDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isRenaming)
                    .buttonStyle(.borderless)
                }
                if let renameError {
                    Text(renameError)
                        .font(.footnote)
                        .foregroundColor(.red)
                }
            }
            .padding(.vertical, 6)
        } else {
            Group {
                if let destination = descriptionDestination {
                    NavigationLink {
                        destination
                    } label: {
                        descriptionRow(showSubtitle: true)
                    }
                    .buttonStyle(.plain)
                } else {
                    descriptionRow(showSubtitle: false)
                }
            }
            .contentShape(Rectangle())
            .swipeActions(edge: .leading, allowsFullSwipe: false) {
                Button {
                    descriptionDraft = transaction.descriptionDisplay ?? transaction.displayTitle ?? ""
                    isEditingDescription = true
                } label: {
                    Label("Rename", systemImage: "pencil")
                        .labelStyle(.iconOnly)
                        .font(.title2)
                }
                .tint(.blue)
                .disabled(isRenaming)
            }
            .padding(.vertical, 6)
        }
    }

    @ViewBuilder
    private func categoryNavigationLink(for category: TransactionCategory) -> some View {
        if let categoryId = category.categoryId {
            NavigationLink {
                CategoryTransactionsView(
                    categoryId: categoryId,
                    categoryName: category.categoryName ?? category.categoryKey
                )
            } label: {
                categoryRow(for: category)
            }
        } else {
            categoryRow(for: category)
        }
    }

    @ViewBuilder
    private var relatedTotalSection: some View {
        if let context = relatedContext {
            Section("Total spent/earned") {
                if isLoadingRelatedTotal && relatedTotal == nil {
                    HStack(spacing: 8) {
                        ProgressView()
                        Text("Loading total...")
                    }
                } else if let error = relatedTotalError, relatedTotal == nil {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Could not load total")
                            .font(.headline)
                        Text(error)
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        Button("Retry") {
                            Task { await loadRelatedTotal(forceReload: true) }
                        }
                        .buttonStyle(.borderless)
                    }
                } else {
                    NavigationLink {
                        relatedDestination(for: context)
                    } label: {
                        relatedTotalRow(context: context)
                    }
                    .disabled(isLoadingRelatedTotal)
                }
            }
        }
    }

    private func relatedTotalRow(context: RelatedContext) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(relatedTotalTitle(for: context))
                    .font(.headline)
                if let subtitle = relatedTotalSubtitle(for: context) {
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            if let total = relatedTotal {
                let direction = direction(for: total.amount)
                Text("\(TransactionUI.signedAmountText(abs(total.amount), direction: direction)) \(transaction.currencySymbol)")
                    .font(.headline)
                    .foregroundColor(TransactionUI.amountColor(direction: direction))
            } else if isLoadingRelatedTotal {
                ProgressView()
            } else if relatedTotalError != nil {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.orange)
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func relatedDestination(for context: RelatedContext) -> some View {
        switch context {
        case .merchant(let id, let name):
            MerchantTransactionsView(
                merchantId: id,
                merchantName: name
            )
        case .description(let raw, let display):
            DescriptionTransactionsView(
                descriptionQuery: raw,
                displayTitle: display
            )
        }
    }

    private var descriptionDestination: DescriptionTransactionsView? {
        guard let query = transaction.descriptionRaw ?? transaction.displayDescription else {
            return nil
        }
        return DescriptionTransactionsView(
            descriptionQuery: query,
            displayTitle: transaction.displayDescription ?? query
        )
    }

    private func categoryRow(for category: TransactionCategory) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(category.categoryName ?? "Unnamed category")
                .font(.headline)
        }
        .padding(.vertical, 4)
    }

    // MARK: - Loading

    @MainActor
    private func loadCategories() async {
        if availableCategories.isEmpty {
            await ensureCategoriesLoaded()
        }
        guard let txId = transaction.resolvedBackendId else {
            errorMessage = "Transaction has no ID"
            return
        }
        isLoading = true
        errorMessage = nil
        do {
            let fetched = try await APIClient.shared.fetchTransactionCategories(transactionId: txId)
            categories = enrichCategories(fetched)
            syncCategorySelectionFromCurrent()
            print("✅ Loaded \(categories.count) categories")
        } catch {
            errorMessage = error.localizedDescription
            print("❌ Error loading categories: \(error.localizedDescription)")
        }
        isLoading = false
    }

    @MainActor
    private var canBulkRename: Bool {
        guard let raw = transaction.descriptionRaw else { return false }
        return !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func loadRelatedTotal(forceReload: Bool = false) async {
        guard let context = relatedContext else { return }
        if isLoadingRelatedTotal && !forceReload { return }
        isLoadingRelatedTotal = true
        relatedTotalError = nil
        do {
            switch context {
            case .merchant(let id, _):
                relatedTotal = try await APIClient.shared.fetchMerchantTotal(merchantId: id)
            case .description(let raw, _):
                relatedTotal = try await APIClient.shared.fetchDescriptionTotal(query: raw)
            }
        } catch {
            relatedTotalError = error.localizedDescription
        }
        isLoadingRelatedTotal = false
    }

    // MARK: - Helpers

    private func syncCategorySelectionFromCurrent() {
        categorySelection = Set(categories.compactMap { $0.categoryId })
        primaryCategoryId = categories.first(where: { $0.primary == true })?.categoryId ?? categories.first?.categoryId
    }

    private func enrichCategories(_ cats: [TransactionCategory]) -> [TransactionCategory] {
        guard !availableCategories.isEmpty else { return cats }
        let lookup = Dictionary(uniqueKeysWithValues: availableCategories.map { ($0.id, $0) })
        return cats.map { cat in
            let meta = cat.categoryId.flatMap { lookup[$0] }
            return TransactionCategory(
                id: cat.id,
                transactionId: cat.transactionId,
                categoryId: cat.categoryId,
                categoryKey: cat.categoryKey ?? meta?.key,
                categoryName: cat.categoryName ?? meta?.name,
                parentCategoryId: cat.parentCategoryId,
                confidence: cat.confidence,
                source: cat.source,
                primary: cat.primary
            )
        }
    }

    private func relatedTotalTitle(for context: RelatedContext) -> String {
        switch context {
        case .merchant(_, let name):
            if let name {
                return "Total at \(name)"
            }
            return "Merchant total"
        case .description(_, let display):
            return "Total for \(display ?? "description")"
        }
    }

    private func relatedTotalSubtitle(for context: RelatedContext) -> String? {
        switch context {
        case .merchant:
            return "Across all transactions with this merchant"
        case .description:
            return "Across all transactions matching this description"
        }
    }

    @ViewBuilder
    private func descriptionRow(showSubtitle: Bool) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(transaction.displayDescription ?? "No description")
                    .font(.headline.weight(.semibold))
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if showSubtitle {
                    Text("View all matching transactions")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
        }
    }

    private func direction(for amount: Double) -> String? {
        if amount == 0 { return nil }
        return amount > 0 ? "IN" : "OUT"
    }

    private var headerView: some View {
        VStack(spacing: 8) {
            if let logoUrl = transaction.merchant?.logoUrl,
               let url = URLHelpers.sanitizedURL(from: logoUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure:
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    case .empty:
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    @unknown default:
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    }
                }
                .frame(width: 64, height: 64)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(Color(.separator), lineWidth: 0.5)
                )
                .padding(.bottom, 4)
            }
            if let title = transaction.displayTitle {
                Text(title)
                    .font(.title2.weight(.semibold))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            if let amount = transaction.amount {
                Text("\(TransactionUI.signedAmountText(amount, direction: transaction.direction)) \(transaction.currencySymbol)")
                    .font(.system(size: 38, weight: .black))
                    .foregroundColor(TransactionUI.amountColor(direction: transaction.direction))
                    .frame(maxWidth: .infinity)
            }
            if let date = transaction.bookingDateAsDate {
                Text(AppFormatters.transactionDetailDate.string(from: date))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 6)
            }
        }
        .padding(.vertical, 12)
    }

    @MainActor
    private func ensureCategoriesLoaded() async {
        guard availableCategories.isEmpty else { return }
        isFetchingCategoryList = true
        categoryError = nil
        do {
            availableCategories = try await APIClient.shared.fetchCategories()
                .sorted { ($0.name ?? "") < ($1.name ?? "") }
        } catch {
            categoryError = error.localizedDescription
        }
        isFetchingCategoryList = false
        syncCategorySelectionFromCurrent()
    }

    @MainActor
    private func submitCategories() async {
        if availableCategories.isEmpty {
            await ensureCategoriesLoaded()
        }
        guard let txId = transaction.resolvedBackendId else {
            categoryError = "Transaction has no ID"
            return
        }
        guard !categorySelection.isEmpty else {
            categoryError = "Select at least one category"
            return
        }
        var primary = primaryCategoryId
        if let existingPrimary = primary, !categorySelection.contains(existingPrimary) {
            primary = nil
        }
        if primary == nil {
            primary = categorySelection.first
        }
        let selections = categorySelection.sorted().map { id in
            UpdateTransactionCategoriesRequest.CategorySelection(
                categoryId: id,
                primary: id == primary ? true : nil
            )
        }

        isSubmittingCategories = true
        categoryError = nil
        do {
            let updated = try await APIClient.shared.updateTransactionCategories(
                transactionId: txId,
                selections: selections
            )
            let enriched = enrichCategories(updated)
            categories = enriched
            LocalCache.shared.saveCategories(enriched, forTransactionId: txId)
            // keep any cached transaction list aligned with category changes for this tx
            if let accountId {
                LocalCache.shared.updateTransaction(transaction, forAccount: accountId)
            }
            syncCategorySelectionFromCurrent()
            isEditingCategories = false
        } catch {
            categoryError = error.localizedDescription
        }
        isSubmittingCategories = false
    }

    @MainActor
    private func saveDescription(applyToAll: Bool = false) async {
        guard let accountId else {
            renameError = "Missing account id"
            return
        }
        guard let txId = transaction.resolvedBackendId else {
            renameError = "Transaction has no ID"
            return
        }

        let trimmed = (pendingRenameDraft ?? descriptionDraft).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            renameError = "Description cannot be empty"
            return
        }

        let optimistic = transaction.updatingDescriptionDisplay(trimmed)
        transaction = optimistic
        isRenaming = true
        renameError = nil
        do {
            if applyToAll {
                guard let raw = transaction.descriptionRaw, !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                    throw NSError(domain: "Rename", code: 0, userInfo: [NSLocalizedDescriptionKey: "Cannot bulk rename without an original description"])
                }
                let updatedList = try await APIClient.shared.bulkRenameDescription(
                    descriptionRaw: raw,
                    descriptionDisplay: trimmed
                )
                if let matching = updatedList.first(where: { $0.id == transaction.id || $0.resolvedBackendId == transaction.resolvedBackendId }) {
                    transaction = matching
                    descriptionDraft = matching.descriptionDisplay ?? matching.displayTitle ?? ""
                } else {
                    descriptionDraft = trimmed
                }
                updatedList.forEach { LocalCache.shared.updateTransaction($0, forAccount: accountId) }
            } else {
                let updated = try await APIClient.shared.renameTransactionDescription(
                    accountId: accountId,
                    transactionId: txId,
                    descriptionDisplay: trimmed
                )
                transaction = updated
                descriptionDraft = updated.descriptionDisplay ?? updated.displayTitle ?? ""
                LocalCache.shared.updateTransaction(updated, forAccount: accountId)
            }
            isEditingDescription = false
            pendingRenameDraft = nil
            relatedTotal = nil
            await loadRelatedTotal(forceReload: true)
        } catch {
            renameError = error.localizedDescription
        }
        isRenaming = false
    }

    private func cancelEditing() {
        isEditingDescription = false
        renameError = nil
        descriptionDraft = transaction.descriptionDisplay ?? transaction.displayTitle ?? ""
        pendingRenameDraft = nil
    }
}

#Preview {
    NavigationStack {
        TransactionDetailView(
            transaction: Transaction.previewList.first!,
            initialCategories: TransactionCategory.previewList
        )
    }
}
