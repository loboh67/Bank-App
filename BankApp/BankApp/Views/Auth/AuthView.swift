import SwiftUI

struct AuthView: View {
    @EnvironmentObject var appState: AppState
    
    @State private var email = ""
    @State private var password = ""
    @State private var isRegister = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Picker("", selection: $isRegister) {
                    Text("Login").tag(false)
                    Text("Register").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.bottom, 24)
                
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textInputAutocapitalization(.never)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(8)
                
                SecureField("Password", text: $password)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(8)
                
                if let error = appState.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.footnote)
                }
                
                Button {
                    Task {
                        if isRegister {
                            await appState.register(email: email, password: password)
                        } else {
                            await appState.login(email: email, password: password)
                        }
                    }
                } label: {
                    if appState.isLoading {
                        ProgressView()
                            .padding()
                    } else {
                        Text(isRegister ? "Create account" : "Login")
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(email.isEmpty || password.isEmpty || appState.isLoading)
                
                Spacer()
            }
            .padding()
            .navigationTitle(isRegister ? "Register" : "Login")
        }
    }
}
