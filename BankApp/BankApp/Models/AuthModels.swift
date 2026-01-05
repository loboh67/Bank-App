import Foundation

struct AuthRequest: Codable {
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let accessToken: String
    let tokenType: String
}

