import Foundation

struct BankSessionStatus: Codable {
    let hasActiveSessions: Bool
    let sessionIds: [Int]
}

struct BankAuthRequest: Codable {
    let country: String
    let name: String
}

struct BankAuthResponse: Decodable {
    let sessionId: Int?
    let redirectUrl: String?
    let message: String?

    enum CodingKeys: String, CodingKey {
        case sessionId
        case redirectUrl
        case redirect_url
        case url
        case message
    }

    init(sessionId: Int?, redirectUrl: String?, message: String?) {
        self.sessionId = sessionId
        self.redirectUrl = redirectUrl
        self.message = message
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let sessionId = try container.decodeIfPresent(Int.self, forKey: .sessionId)

        let redirectUrl: String?
        if let v = try container.decodeIfPresent(String.self, forKey: .redirectUrl) {
            redirectUrl = v
        } else if let v = try container.decodeIfPresent(String.self, forKey: .redirect_url) {
            redirectUrl = v
        } else {
            redirectUrl = try container.decodeIfPresent(String.self, forKey: .url)
        }

        let message = try container.decodeIfPresent(String.self, forKey: .message)
        self.init(sessionId: sessionId, redirectUrl: redirectUrl, message: message)
    }
}

struct Bank: Identifiable, Equatable {
    let id = UUID()
    let country: String
    let name: String
    let isSupported: Bool
}

struct BankAccount: Codable, Identifiable {
    let id: Int
    let name: String?
    let iban: String?
}

