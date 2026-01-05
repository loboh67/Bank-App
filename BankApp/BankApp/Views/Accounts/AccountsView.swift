import SwiftUI

struct AccountsView: View {
    @EnvironmentObject var appState: AppState
    @State private var accounts: [BankAccount] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    @StateObject private var nameStore = AccountNameStore.shared
    
    @State private var accountBeingRenamed: BankAccount?
    @State private var newAccountName: String = ""
    @State private var isShowingRenameSheet = false
    @State private var isOffline: Bool = !NetworkMonitor.shared.isOnline
    
    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("A carregar contas...")
                } else if let errorMessage {
                    VStack(spacing: 12) {
                        Text("Ocorreu um erro")
                            .font(.headline)
                        Text(errorMessage)
                            .font(.subheadline)
                        Button("Tentar novamente") {
                            Task { await loadAccounts() }
                        }
                    }
                } else if accounts.isEmpty {
                    Text("Não tens contas associadas.")
                        .foregroundColor(.secondary)
                } else {
                    List(accounts) { account in
                        let displayName = nameStore.displayName(for: account)
                        
                        NavigationLink {
                            // DESTINATION
                            TransactionsView(account: account)
                        } label: {
                            // LABEL
                            VStack(alignment: .leading, spacing: 4) {
                                Text(displayName)
                                    .font(.headline)
                            }
                        }
                        .swipeActions(edge: .trailing) {
                            Button("Rename") {
                                accountBeingRenamed = account
                                newAccountName = displayName
                                isShowingRenameSheet = true
                            }
                        }
                    }
                    .safeAreaInset(edge: .top) {
                        Color.clear.frame(height: 8)
                    }
                }
            }
            .navigationTitle("Accounts")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Logout") {
                        appState.logout()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await loadAccounts() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(isOffline)
                }
                ToolbarItem(placement: .topBarLeading) {
                    OfflineBadge(isOffline: isOffline)
                }
            }
            .sheet(isPresented: $isShowingRenameSheet) {
                NavigationStack {
                    Form {
                        Section("Account name") {
                            TextField("Account name", text: $newAccountName)
                        }
                        
                        Section {
                            Text("This name is saved on device only.")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                    }
                    .navigationTitle("Rename account")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                isShowingRenameSheet = false
                                accountBeingRenamed = nil
                            }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") {
                                if let account = accountBeingRenamed {
                                    nameStore.setName(newAccountName, for: account.id)
                                }
                                isShowingRenameSheet = false
                                accountBeingRenamed = nil
                            }
                        }
                    }
                }
            }

            .task {
                await loadAccounts()
            }
            .onReceive(NotificationCenter.default.publisher(for: .reachabilityChanged)) { notification in
                if let isOn = notification.object as? Bool {
                    isOffline = !isOn
                }
            }
        }
    }
    
    private func loadAccounts() async {
        isLoading = true
        errorMessage = nil
        do {
            accounts = try await APIClient.shared.fetchAccounts()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
