import SwiftUI

struct CategoryTransactionsView: View {
    let categoryId: Int
    let categoryName: String?
    private let shouldLoadData: Bool

    @State private var transactions: [Transaction]
    @State private var total: CategoryTotal?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var hasLoaded: Bool

    init(
        categoryId: Int,
        categoryName: String?,
        mockTransactions: [Transaction]? = nil,
        mockTotal: CategoryTotal? = nil
    ) {
        self.categoryId = categoryId
        self.categoryName = categoryName
        _transactions = State(initialValue: mockTransactions ?? [])
        _total = State(initialValue: mockTotal)
        _hasLoaded = State(initialValue: mockTransactions != nil || mockTotal != nil)
        shouldLoadData = mockTransactions == nil && mockTotal == nil
    }

    var body: some View {
        List {
            summaryContent
            transactionsContent
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle(categoryName ?? "Category")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task { await loadData(forceReload: true) }
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(isLoading || !shouldLoadData)
            }
        }
        .task {
            if shouldLoadData && !hasLoaded {
                await loadData()
            }
        }
        .refreshable {
            if shouldLoadData {
                await loadData(forceReload: true)
            }
        }
        .overlay {
            if isLoading && transactions.isEmpty && total == nil && errorMessage == nil {
                ProgressView("Loading category")
            } else {
                EmptyView()
            }
        }
    }

    @ViewBuilder
    private var summaryContent: some View {
        if isLoading && total == nil {
            HStack(spacing: 8) {
                ProgressView()
                Text("Loading total...")
            }
        } else if let total {
            totalSummaryView(total)
        } else {
            Text("No total available")
                .foregroundColor(.secondary)
        }
    }

    @ViewBuilder
    private var transactionsContent: some View {
        if let errorMessage {
            Section("Transactions") {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Could not load transactions")
                        .font(.headline)
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        Task { await loadData(forceReload: true) }
                    }
                    .buttonStyle(.borderless)
                }
            }
        } else if isLoading && transactions.isEmpty {
            Section("Transactions") {
                HStack(spacing: 8) {
                    ProgressView()
                    Text("Loading transactions...")
                }
            }
        } else if transactions.isEmpty {
            Section("Transactions") {
                Text("No transactions for this category")
                    .foregroundColor(.secondary)
            }
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

    private func totalSummaryView(_ total: CategoryTotal) -> some View {
        let direction: String? = total.amount == 0 ? nil : (total.amount > 0 ? "IN" : "OUT")
        let label = total.amount > 0 ? "Total earned" : (total.amount < 0 ? "Total spent" : "Total")

        return VStack(spacing: 6) {
            Text(label)
                .font(.callout.weight(.bold))
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, alignment: .center)
            Text("\(TransactionUI.signedAmountText(abs(total.amount), direction: direction)) \(currencySymbol)")
                .font(.system(size: 38, weight: .black))
                .foregroundColor(TransactionUI.amountColor(direction: direction))
                .frame(maxWidth: .infinity, alignment: .center)
        }
        .frame(maxWidth: .infinity)
        .listRowBackground(Color.clear)
        .padding(.vertical, 8)
    }

    private var currencySymbol: String {
        transactions.first?.currencySymbol ?? "€"
    }

    private func transactionRow(for tx: Transaction) -> some View {
        TransactionRowView(
            accountId: nil,
            transaction: tx,
            isEditing: false,
            isRenaming: false,
            renameError: nil,
            onCancel: {},
            onSubmit: { _ in },
            rowContent: { transactionContent(for: tx) }
        )
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

    @MainActor
    private func loadData(forceReload: Bool = false) async {
        if !shouldLoadData { return }
        if isLoading && !forceReload { return }
        isLoading = true
        errorMessage = nil
        do {
            async let txs = APIClient.shared.fetchCategoryTransactions(categoryId: categoryId)
            async let totalValue = APIClient.shared.fetchCategoryTotal(categoryId: categoryId)
            let (fetchedTransactions, fetchedTotal) = try await (txs, totalValue)
            transactions = fetchedTransactions
            total = fetchedTotal
            hasLoaded = true
        } catch {
            errorMessage = error.localizedDescription
            hasLoaded = true
        }
        isLoading = false
    }
}

#Preview {
    NavigationStack {
        CategoryTransactionsView(
            categoryId: 100,
            categoryName: "Food & Dining",
            mockTransactions: Transaction.previewList,
            mockTotal: CategoryTotal(amount: -245.50)
        )
    }
}
