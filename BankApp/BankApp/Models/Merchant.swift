import Foundation

struct Merchant: Codable, Identifiable {
    let id: String
    let name: String?
    let logoUrl: String?

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case logoUrl
        case logoUrlSnake = "logo_url"
    }

    init(
        id: String,
        name: String? = nil,
        logoUrl: String? = nil
    ) {
        self.id = id
        self.name = name
        self.logoUrl = logoUrl
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let rawIdString = try? c.decodeIfPresent(String.self, forKey: .id)
        let rawIdInt = try? c.decodeIfPresent(Int.self, forKey: .id)
        let resolvedId = rawIdString ?? (rawIdInt.map { String($0) }) ?? UUID().uuidString

        let name = try c.decodeIfPresent(String.self, forKey: .name)

        let logoUrl: String?
        if let v = try c.decodeIfPresent(String.self, forKey: .logoUrl) {
            logoUrl = v
        } else {
            logoUrl = try c.decodeIfPresent(String.self, forKey: .logoUrlSnake)
        }

        self.id = resolvedId
        self.name = name
        self.logoUrl = logoUrl
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encodeIfPresent(name, forKey: .name)
        try container.encodeIfPresent(logoUrl, forKey: .logoUrl)
    }
}

