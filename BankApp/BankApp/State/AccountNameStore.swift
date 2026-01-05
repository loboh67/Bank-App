import Foundation
import Combine

final class AccountNameStore: ObservableObject {
    static let shared = AccountNameStore()
    
    @Published private(set) var customNames: [Int: String] = [:]
    
    private let defaultsKey = "accountCustomNames"
    
    private init() {
        if let data = UserDefaults.standard.data(forKey: defaultsKey),
           let decoded = try? JSONDecoder().decode([Int: String].self, from: data) {
            self.customNames = decoded
        }
    }
    
    func displayName(for account: BankAccount, includeIBAN: Bool = true) -> String {
        if let custom = customNames[account.id], !custom.isEmpty {
            return custom
        }
        if let name = account.name, !name.isEmpty {
            return name
        }
        if includeIBAN, let iban = account.iban {
            return "Account \(iban)"
        }
        return "Account \(account.id)"
    }
    
    func setName(_ name: String, for accountId: Int) {
        customNames[accountId] = name
        persist()
    }
    
    private func persist() {
        if let data = try? JSONEncoder().encode(customNames) {
            UserDefaults.standard.set(data, forKey: defaultsKey)
        }
    }
}
