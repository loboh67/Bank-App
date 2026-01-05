import SwiftUI

struct BankSessionSetupView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.openURL) private var openURL
    @State private var selectedBankId: UUID?
    
    private let banks: [Bank] = [
        Bank(country: "PT", name: "Mock ASPSP", isSupported: true),
        Bank(country: "PT", name: "Caixa Geral de Depósitos", isSupported: false),
        Bank(country: "PT", name: "Santander", isSupported: false),
        Bank(country: "PT", name: "Revolut", isSupported: false),
        Bank(country: "PT", name: "Crédito Agrícola", isSupported: false)
    ]
    
    var body: some View {
        NavigationStack {
            List {
                VStack(alignment: .leading, spacing: 8) {
                    Text("No active sessions found.")
                        .font(.headline)
                    Text("Select a bank to start a new session. For now, only Mock ASPSP (PT) is available.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                
                ForEach(banks) { bank in
                    Button {
                        Task { await select(bank) }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(bank.name)
                                    .font(.headline)
                                Text(bank.country)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            if !bank.isSupported {
                                Text("Unavailable")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            } else if appState.isStartingBankAuth && selectedBankId == bank.id {
                                ProgressView()
                            } else {
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .disabled(!bank.isSupported || appState.isStartingBankAuth)
                }
                
                if let error = appState.bankAuthError {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.footnote)
                        .listRowSeparator(.hidden)
                }
                
                if let callbackMsg = appState.bankAuthCallbackMessage {
                    Text(callbackMsg)
                        .foregroundColor(.secondary)
                        .font(.footnote)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Color(.systemBackground))
            .navigationTitle("Create session")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Logout") {
                        appState.logout()
                    }
                }
            }
        }
    }
    
    private func select(_ bank: Bank) async {
        guard bank.isSupported else { return }
        selectedBankId = bank.id
        let response = await appState.startBankAuthentication(for: bank)
        if let urlString = response?.redirectUrl,
           let url = URL(string: urlString) {
            print("🌐 Opening redirect URL: \(url)")
            await MainActor.run {
                openURL(url)
            }
        } else {
            let message = response?.message ?? "No redirectUrl returned. Response: \(String(describing: response))"
            await MainActor.run {
                appState.bankAuthError = message
            }
            print("⚠️ No redirectUrl found in bank auth response: \(message)")
        }
        selectedBankId = nil
    }
}

struct BankSessionSetupView_Previews: PreviewProvider {
    static var previews: some View {
        BankSessionSetupView()
            .environmentObject(AppState())
    }
}
