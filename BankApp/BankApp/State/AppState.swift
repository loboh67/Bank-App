import Combine
import Foundation

@MainActor
final class AppState: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var bankSessionStatus: BankSessionStatus?
    @Published var bankSessionError: String?
    @Published var isCheckingBankSession = false
    @Published var isStartingBankAuth = false
    @Published var bankAuthError: String?
    @Published var bankAuthCallbackMessage: String?
    
    init() {
        // se tiver token guardado, considera logged in
        self.isAuthenticated = APIClient.shared.token != nil
    }
    
    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            try await APIClient.shared.login(email: email, password: password)
            isAuthenticated = true
            bankSessionStatus = nil
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
    
    func register(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        do {
            try await APIClient.shared.register(email: email, password: password)
            isAuthenticated = true
            bankSessionStatus = nil
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
    
    func logout() {
        APIClient.shared.token = nil
        isAuthenticated = false
        bankSessionStatus = nil
        bankSessionError = nil
        bankAuthError = nil
        bankAuthCallbackMessage = nil
        LocalCache.shared.clearAll()
    }

    func handleDeepLink(_ url: URL) {
        // expecting bankapp://enablebanking/auth/callback?... from backend
        guard url.scheme == "bankapp" else { return }
        let path = url.path
        let host = url.host ?? ""
        if host == "enablebanking", path == "/auth/callback" {
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            let queryItems = components?.queryItems ?? []
            let state = queryItems.first { $0.name == "state" }?.value
            let code = queryItems.first { $0.name == "code" }?.value
            let error = queryItems.first { $0.name == "error" }?.value

            print("🔗 Received bank callback. state=\(state ?? "nil") code=\(code ?? "nil") error=\(error ?? "nil")")
            Task { await refreshBankSessionStatus() }
            if let error {
                bankAuthCallbackMessage = "Bank auth returned error: \(error)"
            } else {
                bankAuthCallbackMessage = "Bank authorization completed. Updating session..."
            }
        }
    }

    func loadBankSessionStatusIfNeeded() async {
        guard isAuthenticated else { return }
        guard bankSessionStatus == nil, !isCheckingBankSession else { return }
        await refreshBankSessionStatus()
    }

    func refreshBankSessionStatus() async {
        guard isAuthenticated else {
            bankSessionStatus = nil
            bankSessionError = nil
            return
        }

        isCheckingBankSession = true
        bankSessionError = nil
        defer { isCheckingBankSession = false }

        do {
            bankSessionStatus = try await APIClient.shared.fetchBankSessionStatus()
        } catch {
            bankSessionStatus = nil
            bankSessionError = error.localizedDescription
        }
    }

    func startBankAuthentication(for bank: Bank) async -> BankAuthResponse? {
        guard !isStartingBankAuth else {
            print("⏳ Ignoring startBankAuthentication because a request is already in progress")
            return nil
        }
        isStartingBankAuth = true
        bankAuthError = nil
        bankAuthCallbackMessage = nil

        do {
            let response = try await APIClient.shared.startBankAuthentication(country: bank.country, name: bank.name)
            print("✅ Bank auth started for \(bank.name) (\(bank.country)). Response: \(response)")
            await refreshBankSessionStatus()
            isStartingBankAuth = false
            return response
        } catch {
            print("❌ Bank auth failed for \(bank.name) (\(bank.country)): \(error)")
            bankAuthError = error.localizedDescription
            isStartingBankAuth = false
            return nil
        }
    }
}
