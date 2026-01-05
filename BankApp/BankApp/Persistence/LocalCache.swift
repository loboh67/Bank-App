import Foundation

/// Lightweight JSON cache for persisting API data locally.
final class LocalCache {
    static let shared = LocalCache()
    
    private let fileManager = FileManager.default
    private let baseURL: URL
    
    private init() {
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        baseURL = documents.appendingPathComponent("BankAppCache", isDirectory: true)
        try? fileManager.createDirectory(at: baseURL, withIntermediateDirectories: true)
    }
    
    // MARK: - Public API
    
    func saveAccounts(_ accounts: [BankAccount]) {
        save(accounts, to: "accounts.json")
    }
    
    func loadAccounts() -> [BankAccount]? {
        load([BankAccount].self, from: "accounts.json")
    }
    
    func saveBankSessionStatus(_ status: BankSessionStatus) {
        save(status, to: "bankSessionStatus.json")
    }
    
    func loadBankSessionStatus() -> BankSessionStatus? {
        load(BankSessionStatus.self, from: "bankSessionStatus.json")
    }
    
    func saveTransactions(_ transactions: [Transaction], forAccount accountId: Int) {
        save(transactions, to: "transactions/account-\(accountId).json")
    }
    
    func loadTransactions(forAccount accountId: Int) -> [Transaction]? {
        load([Transaction].self, from: "transactions/account-\(accountId).json")
    }
    
    func saveCategories(_ categories: [TransactionCategory], forTransactionId transactionId: String) {
        save(categories, to: "categories/tx-\(transactionId).json")
    }
    
    func loadCategories(forTransactionId transactionId: String) -> [TransactionCategory]? {
        load([TransactionCategory].self, from: "categories/tx-\(transactionId).json")
    }

    func updateTransaction(_ updated: Transaction, forAccount accountId: Int) {
        var cached = loadTransactions(forAccount: accountId) ?? []
        var didUpdate = false

        cached = cached.map { tx in
            if tx.id == updated.id {
                didUpdate = true
                return updated
            }
            if let txId = tx.transactionId,
               let updatedTxId = updated.transactionId,
               txId == updatedTxId {
                didUpdate = true
                return updated
            }
            if let txApiId = tx.apiId,
               let updatedApiId = updated.apiId,
               txApiId == updatedApiId {
                didUpdate = true
                return updated
            }
            return tx
        }

        if didUpdate {
            saveTransactions(cached, forAccount: accountId)
        }
    }
    
    func clearAll() {
        try? fileManager.removeItem(at: baseURL)
        try? fileManager.createDirectory(at: baseURL, withIntermediateDirectories: true)
    }
    
    // MARK: - Helpers
    
    private func save<T: Encodable>(_ value: T, to relativePath: String) {
        let url = path(for: relativePath)
        createParentDirIfNeeded(for: url)
        do {
            let data = try JSONEncoder().encode(value)
            try data.write(to: url, options: .atomic)
        } catch {
            print("⚠️ Cache save failed for \(relativePath): \(error)")
        }
    }
    
    private func load<T: Decodable>(_ type: T.Type, from relativePath: String) -> T? {
        let url = path(for: relativePath)
        guard fileManager.fileExists(atPath: url.path) else { return nil }
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(type, from: data)
        } catch {
            print("⚠️ Cache load failed for \(relativePath): \(error)")
            return nil
        }
    }
    
    private func path(for relativePath: String) -> URL {
        baseURL.appendingPathComponent(relativePath, isDirectory: false)
    }
    
    private func createParentDirIfNeeded(for url: URL) {
        let dir = url.deletingLastPathComponent()
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
    }
}
