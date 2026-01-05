import Foundation

final class APIClient {
    static let shared = APIClient()
    
    private init() {}
    
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.waitsForConnectivity = false
        config.timeoutIntervalForRequest = 120
        config.timeoutIntervalForResource = 120
        return URLSession(configuration: config)
    }()
    private let baseURL = URL(string: "http://192.168.1.183:8085")!
    private let isoDayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    struct OfflineError: LocalizedError {
        var errorDescription: String? { "No internet connection" }
    }
    
    var token: String? {
        get { UserDefaults.standard.string(forKey: "authToken") }
        set { UserDefaults.standard.setValue(newValue, forKey: "authToken") }
    }
    
    // MARK: - Auth
    
    func login(email: String, password: String) async throws {
        let body = AuthRequest(email: email, password: password)
        let response: AuthResponse = try await request(
            path: "/api/auth/login",
            method: "POST",
            body: body,
            authorized: false
        )
        token = response.accessToken
    }
    
    func register(email: String, password: String) async throws {
        let body = AuthRequest(email: email, password: password)
        let response: AuthResponse = try await request(
            path: "/api/auth/register",
            method: "POST",
            body: body,
            authorized: false
        )
        token = response.accessToken
    }
    
    // MARK: - Accounts
    
    func fetchAccounts() async throws -> [BankAccount] {
        if !NetworkMonitor.shared.isOnline {
            if let cached = LocalCache.shared.loadAccounts() { return cached }
            throw OfflineError()
        }
        do {
            let accounts: [BankAccount] = try await request(path: "/api/accounts", method: "GET")
            LocalCache.shared.saveAccounts(accounts)
            return accounts
        } catch {
            if let cached = LocalCache.shared.loadAccounts() {
                print("💾 Returning cached accounts after network error: \(error)")
                return cached
            }
            throw error
        }
    }

    // MARK: - Bank sessions

    func fetchBankSessionStatus() async throws -> BankSessionStatus {
        if !NetworkMonitor.shared.isOnline {
            if let cached = LocalCache.shared.loadBankSessionStatus() { return cached }
            throw OfflineError()
        }
        do {
            let status: BankSessionStatus = try await request(path: "/api/bank-session/status", method: "GET")
            LocalCache.shared.saveBankSessionStatus(status)
            return status
        } catch {
            if let cached = LocalCache.shared.loadBankSessionStatus() {
                print("💾 Returning cached bank session status after network error: \(error)")
                return cached
            }
            throw error
        }
    }

    func startBankAuthentication(country: String, name: String) async throws -> BankAuthResponse {
        let body = BankAuthRequest(country: country, name: name)
        return try await request(path: "/api/auth", method: "POST", body: body)
    }
    
    func fetchTransactions(accountId: Int) async throws -> [Transaction] {
        if !NetworkMonitor.shared.isOnline {
            if let cached = LocalCache.shared.loadTransactions(forAccount: accountId) { return cached }
            throw OfflineError()
        }
        do {
            let transactions: [Transaction] = try await request(path: "/api/accounts/\(accountId)/transactions", method: "GET")
            LocalCache.shared.saveTransactions(transactions, forAccount: accountId)
            return transactions
        } catch {
            if let cached = LocalCache.shared.loadTransactions(forAccount: accountId) {
                print("💾 Returning cached transactions for account \(accountId) after network error: \(error)")
                return cached
            }
            throw error
        }
    }

    /// Clears backend-stored transactions for an account before re-syncing.
    func resetTransactionsSync(accountId: Int) async throws {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        var urlRequest = URLRequest(url: baseURL.appendingPathComponent("/api/accounts/\(accountId)/reset-sync"))
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.timeoutInterval = 120

        if let token {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (_, response) = try await session.data(for: urlRequest)

        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200..<300).contains(http.statusCode) else {
            if http.statusCode == 401 {
                throw NSError(domain: "API", code: 401, userInfo: [NSLocalizedDescriptionKey: "Unauthorized"])
            }
            throw NSError(domain: "API", code: http.statusCode, userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode)"])
        }
    }

    /// Triggers a full sync of transactions for an account.
    /// The backend will fetch fresh data into the DB; the UI should then read via the normal transactions endpoint.
    func fullSyncTransactions(accountId: Int) async throws {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        var urlRequest = URLRequest(url: baseURL.appendingPathComponent("/api/accounts/\(accountId)/transactions/full-sync"))
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.timeoutInterval = 120

        if let token {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (_, response) = try await session.data(for: urlRequest)

        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200..<300).contains(http.statusCode) else {
            if http.statusCode == 401 {
                throw NSError(domain: "API", code: 401, userInfo: [NSLocalizedDescriptionKey: "Unauthorized"])
            }
            throw NSError(domain: "API", code: http.statusCode, userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode)"])
        }
    }

    func fetchTransactionCategories(transactionId: String) async throws -> [TransactionCategory] {
        if !NetworkMonitor.shared.isOnline {
            if let cached = LocalCache.shared.loadCategories(forTransactionId: transactionId) { return cached }
            throw OfflineError()
        }
        do {
            let categories: [TransactionCategory] = try await request(
                path: "/api/transactions/\(transactionId)/categories",
                method: "GET"
            )
            LocalCache.shared.saveCategories(categories, forTransactionId: transactionId)
            return categories
        } catch {
            if let cached = LocalCache.shared.loadCategories(forTransactionId: transactionId) {
                print("💾 Returning cached categories for tx \(transactionId) after network error: \(error)")
                return cached
            }
            throw error
        }
    }

    func fetchCategories() async throws -> [Category] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(path: "/api/categories", method: "GET")
    }

    func fetchCategoryTransactions(categoryId: Int, from: Date? = nil, to: Date? = nil) async throws -> [Transaction] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/categories/\(categoryId)/transactions",
            method: "GET",
            queryItems: dateRangeQueryItems(from: from, to: to)
        )
    }

    func fetchCategoryTotal(categoryId: Int) async throws -> CategoryTotal {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/categories/\(categoryId)/transactions/total-spent",
            method: "GET"
        )
    }

    func fetchMerchantTransactions(merchantId: String, from: Date? = nil, to: Date? = nil) async throws -> [Transaction] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/merchants/\(merchantId)/transactions",
            method: "GET",
            queryItems: dateRangeQueryItems(from: from, to: to)
        )
    }

    func fetchMerchantTotal(merchantId: String) async throws -> CategoryTotal {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/merchants/\(merchantId)/transactions/total-spent",
            method: "GET"
        )
    }

    func fetchDescriptionTransactions(
        query: String,
        from: Date? = nil,
        to: Date? = nil
    ) async throws -> [Transaction] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/transactions/description",
            method: "GET",
            queryItems: dateRangeQueryItems(from: from, to: to) + [URLQueryItem(name: "q", value: query)]
        )
    }

    func fetchDescriptionTotal(query: String) async throws -> CategoryTotal {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        return try await request(
            path: "/api/transactions/description/total-spent",
            method: "GET",
            queryItems: [URLQueryItem(name: "q", value: query)]
        )
    }

    func updateTransactionCategories(
        transactionId: String,
        selections: [UpdateTransactionCategoriesRequest.CategorySelection]
    ) async throws -> [TransactionCategory] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        let body = UpdateTransactionCategoriesRequest(categories: selections)
        let updated: [TransactionCategory] = try await request(
            path: "/api/transactions/\(transactionId)/categories",
            method: "PUT",
            body: body
        )
        LocalCache.shared.saveCategories(updated, forTransactionId: transactionId)
        return updated
    }

    func renameTransactionDescription(
        accountId: Int,
        transactionId: String,
        descriptionDisplay: String
    ) async throws -> Transaction {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        let body = RenameTransactionRequest(descriptionDisplay: descriptionDisplay)
        return try await request(
            path: "/api/accounts/\(accountId)/transactions/\(transactionId)/description",
            method: "PATCH",
            body: body
        )
    }

    func bulkRenameDescription(descriptionRaw: String, descriptionDisplay: String) async throws -> [Transaction] {
        if !NetworkMonitor.shared.isOnline {
            throw OfflineError()
        }
        let body = BulkRenameDescriptionRequest(descriptionRaw: descriptionRaw, descriptionDisplay: descriptionDisplay)
        return try await request(
            path: "/api/transactions/description",
            method: "PATCH",
            body: body
        )
    }
    
    // MARK: - Generic request
    
    private func request<T: Decodable, B: Encodable>(
        path: String,
        method: String,
        body: B? = nil,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = []
    ) async throws -> T {
        #if DEBUG
        print("➡️ \(method) \(path)")
        #endif
        var urlRequest = URLRequest(url: try buildURL(path: path, queryItems: queryItems))
        urlRequest.httpMethod = method
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.timeoutInterval = 10
        
        if authorized, let token {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body {
            urlRequest.httpBody = try JSONEncoder().encode(body)
        }
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        
        guard (200..<300).contains(http.statusCode) else {
            #if DEBUG
            print("❌ HTTP \(http.statusCode) for \(method) \(path)")
            #endif
            // Aqui podes mapear erros específicos
            if http.statusCode == 401 {
                throw NSError(domain: "API", code: 401, userInfo: [NSLocalizedDescriptionKey: "Unauthorized"])
            }
            throw NSError(domain: "API", code: http.statusCode, userInfo: [NSLocalizedDescriptionKey: "HTTP \(http.statusCode)"])
        }

#if DEBUG
        if let jsonString = String(data: data, encoding: .utf8) {
            print("📥 Response for \(path): \(jsonString)")
        }
#endif

        return try JSONDecoder().decode(T.self, from: data)
    }
    
    // Overload para requests sem body
    private func request<T: Decodable>(
        path: String,
        method: String,
        authorized: Bool = true,
        queryItems: [URLQueryItem] = []
    ) async throws -> T {
        try await request(
            path: path,
            method: method,
            body: Optional<String>.none,
            authorized: authorized,
            queryItems: queryItems
        )
    }

    private func buildURL(path: String, queryItems: [URLQueryItem]) throws -> URL {
        let basePathURL = baseURL.appendingPathComponent(path)
        guard var components = URLComponents(url: basePathURL, resolvingAgainstBaseURL: false) else {
            throw URLError(.badURL)
        }
        components.queryItems = queryItems.isEmpty ? nil : queryItems
        guard let url = components.url else {
            throw URLError(.badURL)
        }
        return url
    }

    private func dateRangeQueryItems(from: Date?, to: Date?) -> [URLQueryItem] {
        var items: [URLQueryItem] = []
        if let from {
            items.append(URLQueryItem(name: "from", value: isoDayFormatter.string(from: from)))
        }
        if let to {
            items.append(URLQueryItem(name: "to", value: isoDayFormatter.string(from: to)))
        }
        return items
    }
}
