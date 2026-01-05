import Foundation

struct Category: Codable, Identifiable, Equatable {
    let id: Int
    let key: String?
    let name: String?
    let parentId: Int?
}

struct UpdateTransactionCategoriesRequest: Codable {
    struct CategorySelection: Codable {
        let categoryId: Int
        let primary: Bool?
    }
    let categories: [CategorySelection]
}

struct CategoryTotal: Codable {
    let amount: Double

    init(amount: Double) {
        self.amount = amount
    }

    /// Tries to decode either a bare number or common keyed formats.
    init(from decoder: Decoder) throws {
        let single = try decoder.singleValueContainer()
        if let raw = try? single.decode(Double.self) {
            amount = raw
            return
        }

        let keyed = try decoder.container(keyedBy: CodingKeys.self)
        if let totalSpent = try keyed.decodeIfPresent(Double.self, forKey: .totalSpent) {
            amount = totalSpent
            return
        }
        if let totalEarned = try keyed.decodeIfPresent(Double.self, forKey: .totalEarned) {
            amount = totalEarned
            return
        }
        if let total = try keyed.decodeIfPresent(Double.self, forKey: .total) {
            amount = total
            return
        }
        if let value = try keyed.decodeIfPresent(Double.self, forKey: .amount) {
            amount = value
            return
        }

        throw DecodingError.dataCorrupted(.init(codingPath: keyed.codingPath, debugDescription: "Missing total value"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(amount, forKey: .amount)
    }

    private enum CodingKeys: String, CodingKey {
        case totalSpent
        case totalEarned
        case total
        case amount
    }
}
