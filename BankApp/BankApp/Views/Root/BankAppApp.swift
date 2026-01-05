import SwiftUI

@main
struct BankAppApp: App {
    @StateObject private var appState = AppState()
    
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .onOpenURL { url in
                    appState.handleDeepLink(url)
                }
        }
    }
}

struct RootView: View {
    @EnvironmentObject var appState: AppState
    
    var body: some View {
        Group {
            if appState.isAuthenticated {
                BankSessionGateView()
            } else {
                AuthView()
            }
        }
    }
}

struct BankSessionGateView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        Group {
            if appState.isCheckingBankSession && appState.bankSessionStatus == nil {
                VStack(spacing: 12) {
                    ProgressView("Checking bank session...")
                    Text("Please wait while we verify your active sessions.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = appState.bankSessionError {
                VStack(spacing: 12) {
                    Text("Could not verify bank sessions")
                        .font(.headline)
                    Text(error)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    HStack {
                        Button("Retry") {
                            Task { await appState.refreshBankSessionStatus() }
                        }
                        Button("Logout") {
                            appState.logout()
                        }
                        .tint(.red)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let status = appState.bankSessionStatus {
                if status.hasActiveSessions {
                    AccountsView()
                } else {
                    BankSessionSetupView()
                }
            } else {
                ProgressView("Loading...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task {
            await appState.loadBankSessionStatusIfNeeded()
        }
    }
}
