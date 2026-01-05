import SwiftUI

struct TransactionsView: View {
    let account: BankAccount

    private let loadingAnimationName = "loading"
    private let minimumLoadingDurationSeconds: TimeInterval = 2

    @State private var transactions: [Transaction] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var loadingBeganAt: Date?
    @State private var activeLoadID: UUID?
    @State private var loadTask: Task<Void, Never>?
    @State private var showLoadedContent = false
    @State private var isOffline: Bool = !NetworkMonitor.shared.isOnline
    @State private var hasLoadedTransactions = false
    @State private var displayMode: DisplayMode = .transactions
    @State private var editingTransactionId: String?
    @State private var renameError: String?
    @State private var isRenaming = false
    @State private var showRenameScopeDialog = false
    @State private var pendingRenameTransaction: Transaction?
    @State private var pendingRenameDraft: String = ""
    
    @StateObject private var nameStore = AccountNameStore.shared

    private enum DisplayMode {
        case transactions
        case monthly
    }
    
    var body: some View {
        ZStack {
            backgroundView

            contentView
                .opacity(showLoadedContent ? 1 : 0)
                .offset(y: showLoadedContent ? 0 : 40)
                .allowsHitTesting(showLoadedContent)
                .accessibilityHidden(!showLoadedContent)

            if isLoading {
                loadingView
                    .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.45), value: showLoadedContent)
        .navigationTitle(showLoadedContent ? nameStore.displayName(for: account, includeIBAN: false) : "")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    startLoad(forceFullSync: true)
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(isOffline || isLoading)
            }
            ToolbarItem(placement: .topBarLeading) {
                OfflineBadge(isOffline: isOffline)
            }
        }
        .task {
            if !hasLoadedTransactions {
                startLoad()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .reachabilityChanged)) { notification in
            if let isOn = notification.object as? Bool {
                let wasOffline = isOffline
                isOffline = !isOn
                if isOn && wasOffline {
                    startLoad()
                }
            }
        }
        .alert(
            "Apply to matching transactions?",
            isPresented: $showRenameScopeDialog
        ) {
            Button("Just this transaction") {
                showRenameScopeDialog = false
                Task { await savePendingRename(applyToAll: false) }
            }
            if let tx = pendingRenameTransaction, canBulkRename(tx) {
                Button("All with the same description") {
                    showRenameScopeDialog = false
                    Task { await savePendingRename(applyToAll: true) }
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
    private var contentView: some View {
        switch displayMode {
        case .transactions:
            transactionsList
        case .monthly:
            monthlySummaryView
        }
    }

    private var transactionsList: some View {
        List {
            if let errorMessage {
                VStack(spacing: 8) {
                    Text("Error loading transactions")
                        .font(.headline)
                    Text(errorMessage)
                        .font(.subheadline)
                }
            } else if transactions.isEmpty {
                Text("No transactions on this account")
                    .foregroundColor(.secondary)
            } else {
                ForEach(groupedTransactions, id: \.key) { dateString, items in
                    Section(header: Text(dateString)) {
                        ForEach(items) { tx in
                            transactionRow(for: tx)
                        }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
    }

    private var loadingView: some View {
        VStack {
            Spacer()
            VStack(spacing: 10) {
                LottieView(name: loadingAnimationName)
                    .frame(width: 240, height: 240)
            }
            .offset(y: -36)
            Spacer()
        }
    }

    private var backgroundView: some View {
        Group {
            if isLoading {
                Color(.systemBackground)
            } else {
                Color(.systemGroupedBackground)
            }
        }
        .ignoresSafeArea()
    }

    private var monthlySummaryView: some View {
        let summaries = monthlySummaries()
        return List {
            if summaries.isEmpty {
                Text("No data available")
                    .foregroundColor(.secondary)
            } else {
                ForEach(summaries) { summary in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(summary.title)
                                .font(.headline)
                            Text(summary.subtitle)
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("Earned \(formattedCurrency(summary.earned))")
                                .foregroundColor(.green)
                                .font(.subheadline.weight(.semibold))
                            Text("Spent \(formattedCurrency(summary.spent))")
                                .foregroundColor(.red)
                                .font(.subheadline.weight(.semibold))
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .scrollContentBackground(.hidden)
    }

    // MARK: - Loading inicial
    @MainActor
    private func startLoad(forceFullSync: Bool = false) {
        loadTask?.cancel()
        showLoadedContent = false
        isLoading = true
        loadingBeganAt = Date()
        loadTask = Task {
            await loadTransactions(forceFullSync: forceFullSync)
        }
    }
    
    @MainActor
    private func loadTransactions(forceFullSync: Bool = false) async {
        let loadID = UUID()
        activeLoadID = loadID
        isLoading = true
        loadingBeganAt = loadingBeganAt ?? Date()

        errorMessage = nil

        // Show cached data quickly if available.
        if !forceFullSync,
           let cached = LocalCache.shared.loadTransactions(forAccount: account.id),
           !cached.isEmpty {
            transactions = cached
            hasLoadedTransactions = true
        }

        guard NetworkMonitor.shared.isOnline else {
            isOffline = true
            await finishLoadingIfNeeded(loadID: loadID)
            return
        }

        isOffline = false
        
        do {
            if forceFullSync && NetworkMonitor.shared.isOnline {
                // Reset backend-synced data before triggering a fresh sync.
                try await APIClient.shared.resetTransactionsSync(accountId: account.id)
                transactions = []
                LocalCache.shared.saveTransactions([], forAccount: account.id)

                try await APIClient.shared.fullSyncTransactions(accountId: account.id)
            }

            if !NetworkMonitor.shared.isOnline {
                transactions = LocalCache.shared.loadTransactions(forAccount: account.id) ?? []
            } else {
                transactions = try await APIClient.shared.fetchTransactions(accountId: account.id)
            }
            guard activeLoadID == loadID else { return }
            LocalCache.shared.saveTransactions(transactions, forAccount: account.id)
            hasLoadedTransactions = true
        } catch {
            guard activeLoadID == loadID else { return }
            errorMessage = error.localizedDescription
            if error is APIClient.OfflineError || !NetworkMonitor.shared.isOnline {
                isOffline = true
            }
        }

        await finishLoadingIfNeeded(loadID: loadID)
    }

    @MainActor
    private func finishLoadingIfNeeded(loadID: UUID) async {
        guard activeLoadID == loadID else { return }

        if let start = loadingBeganAt {
            let elapsed = Date().timeIntervalSince(start)
            let remaining = minimumLoadingDurationSeconds - elapsed
            if remaining > 0 {
                let nanos = UInt64(remaining * 1_000_000_000)
                do {
                    try await Task.sleep(nanoseconds: nanos)
                } catch {
                    return
                }
            }
        }

        guard activeLoadID == loadID else { return }
        withAnimation(.easeOut(duration: 0.45)) {
            isLoading = false
            showLoadedContent = true
        }
        loadingBeganAt = nil
        activeLoadID = nil
    }
    
    // MARK: - Helpers
    @ViewBuilder
    private func transactionRow(for tx: Transaction) -> some View {
        TransactionRowView(
            accountId: account.id,
            transaction: tx,
            isEditing: editingTransactionId == tx.id,
            isRenaming: isRenaming && editingTransactionId == tx.id,
            renameError: editingTransactionId == tx.id ? renameError : nil,
            onCancel: { cancelEditing() },
            onSubmit: { draft in
                pendingRenameTransaction = tx
                pendingRenameDraft = draft
                showRenameScopeDialog = true
            },
            rowContent: { transactionContent(for: tx) }
        )
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            Button {
                startEditing(tx)
            } label: {
                Label("Rename", systemImage: "pencil")
                    .labelStyle(.iconOnly)
                    .font(.title2)
            }
            .tint(.blue)
            .disabled(isRenaming && editingTransactionId != tx.id)
        }
    }

    private func transactionContent(for tx: Transaction) -> some View {
        HStack(spacing: 12) {
            let iconSize: CGFloat = 36
            if let logoUrl = tx.merchant?.logoUrl,
               let url = URLHelpers.sanitizedURL(from: logoUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure:
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    case .empty:
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    @unknown default:
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(.secondarySystemFill))
                    }
                }
                .frame(width: iconSize, height: iconSize)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color(.separator), lineWidth: 0.5)
                )
            } else {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(.secondarySystemFill))
                    .frame(width: iconSize, height: iconSize)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(tx.displayTitle ?? "No description")
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                if let desc = tx.descriptionDisplay, tx.merchant?.name != nil {
                    Text(desc)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
            Spacer()
            if let amount = tx.amount {
                Text("\(TransactionUI.signedAmountText(amount, direction: tx.direction)) \(tx.currencySymbol)")
                    .font(.headline)
                    .foregroundColor(TransactionUI.amountColor(direction: tx.direction))
            }
        }
        .padding(.vertical, 4)
    }

    private func startEditing(_ tx: Transaction) {
        editingTransactionId = tx.id
        renameError = nil
    }

    private func canBulkRename(_ tx: Transaction) -> Bool {
        guard let raw = tx.descriptionRaw else { return false }
        return !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    @MainActor
    private func savePendingRename(applyToAll: Bool) async {
        guard let tx = pendingRenameTransaction else { return }
        let draft = pendingRenameDraft
        pendingRenameTransaction = nil
        pendingRenameDraft = ""
        await saveEditing(for: tx, newDescription: draft, applyToAll: applyToAll)
    }

    @MainActor
    private func saveEditing(for tx: Transaction, newDescription: String, applyToAll: Bool) async {
        guard let resolvedId = tx.resolvedBackendId else {
            renameError = "Transaction has no ID"
            return
        }
        let trimmed = newDescription.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            renameError = "Description cannot be empty"
            return
        }
        if applyToAll && !canBulkRename(tx) {
            renameError = "Cannot bulk rename without the original description"
            return
        }
        let previousState = transactions
        if applyToAll, let raw = tx.descriptionRaw {
            transactions = transactions.map { candidate in
                if candidate.descriptionRaw == raw {
                    return candidate.updatingDescriptionDisplay(trimmed)
                }
                return candidate
            }
        } else {
            let optimistic = tx.updatingDescriptionDisplay(trimmed)
            applyUpdatedTransaction(optimistic)
        }
        isRenaming = true
        renameError = nil
        do {
            if applyToAll, let raw = tx.descriptionRaw {
                let updatedList = try await APIClient.shared.bulkRenameDescription(
                    descriptionRaw: raw,
                    descriptionDisplay: trimmed
                )
                updatedList.forEach { applyUpdatedTransaction($0) }
            } else {
                let updated = try await APIClient.shared.renameTransactionDescription(
                    accountId: account.id,
                    transactionId: resolvedId,
                    descriptionDisplay: trimmed
                )
                applyUpdatedTransaction(updated)
            }
        } catch {
            renameError = error.localizedDescription
            transactions = previousState
            LocalCache.shared.saveTransactions(transactions, forAccount: account.id)
        }
        isRenaming = false
        if renameError == nil {
            cancelEditing()
        }
    }

    private func cancelEditing() {
        editingTransactionId = nil
        renameError = nil
    }

    @MainActor
    private func applyUpdatedTransaction(_ updated: Transaction) {
        transactions = transactions.map { tx in
            if tx.id == updated.id {
                return updated
            }
            if let updatedTxId = updated.transactionId, let txId = tx.transactionId, txId == updatedTxId {
                return updated
            }
            if let updatedApiId = updated.apiId, let txApiId = tx.apiId, txApiId == updatedApiId {
                return updated
            }
            return tx
        }
        LocalCache.shared.saveTransactions(transactions, forAccount: account.id)
    }

    private func monthlySummaries() -> [MonthlySummary] {
        var buckets: [String: MonthlySummary] = [:]

        for tx in transactions {
            guard let amount = tx.amount, let date = tx.bookingDateAsDate else { continue }
            let key = AppFormatters.monthKey.string(from: date)
            var summary = buckets[key] ?? MonthlySummary(id: key, title: AppFormatters.monthTitle.string(from: date), subtitle: "", earned: 0, spent: 0)
            let direction = (tx.direction ?? "").uppercased()
            if direction == "IN" || direction == "CREDIT" {
                summary.earned += amount
            } else {
                summary.spent += abs(amount)
            }
            buckets[key] = summary
        }

        return buckets.values.sorted { lhs, rhs in lhs.id > rhs.id }
    }

    private func formattedCurrency(_ value: Double) -> String {
        String(format: "%.2f €", value)
    }

    private var groupedTransactions: [(key: String, value: [Transaction])] {
        let grouped = Dictionary(grouping: transactions) { tx -> String in
            if let date = tx.bookingDateAsDate {
                return AppFormatters.transactionSectionDate.string(from: date)
            }
            return "Unknown date"
        }

        return grouped.sorted { lhs, rhs in
            if let d1 = AppFormatters.transactionSectionDate.date(from: lhs.key),
               let d2 = AppFormatters.transactionSectionDate.date(from: rhs.key) {
                return d1 > d2
            }
            return lhs.key > rhs.key
        }
    }
}

private struct MonthlySummary: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    var earned: Double
    var spent: Double
}

struct TransactionsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            TransactionsView(account: .preview)
        }
    }
}
