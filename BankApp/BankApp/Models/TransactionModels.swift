import Foundation

struct Transaction: Codable, Identifiable {
    let id: String
    let transactionId: String?
    let apiId: String?
    let amount: Double?
    let currency: String?
    let bookingDate: String?
    let descriptionDisplay: String?
    let descriptionRaw: String?
    let legacyDescription: String?
    let direction: String?
    let merchantId: String?
    let merchant: Merchant?

    enum CodingKeys: String, CodingKey {
        case transactionId
        case apiId = "id"
        case amount
        case currency
        case bookingDate
        case descriptionDisplay
        case descriptionRaw
        case legacyDescription = "description"
        case descriptionDisplaySnake = "description_display"
        case descriptionRawSnake = "description_raw"
        case direction
        case merchantId
        case merchantIdSnake = "merchant_id"
        case merchant
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let transactionId = Transaction.decodeId(from: c, key: .transactionId)
        let apiId = Transaction.decodeId(from: c, key: .apiId)
        let amount = try c.decodeIfPresent(Double.self, forKey: .amount)
        let currency = try c.decodeIfPresent(String.self, forKey: .currency)
        let bookingDate = try c.decodeIfPresent(String.self, forKey: .bookingDate)
        let descriptionDisplay = try Self.decodeDescription(in: c, camel: .descriptionDisplay, snake: .descriptionDisplaySnake)
        let descriptionRaw = try Self.decodeDescription(in: c, camel: .descriptionRaw, snake: .descriptionRawSnake)
        let legacyDescription = try c.decodeIfPresent(String.self, forKey: .legacyDescription)
        let direction = try c.decodeIfPresent(String.self, forKey: .direction)
        let merchantId = Transaction.decodeId(from: c, key: .merchantId) ?? Transaction.decodeId(from: c, key: .merchantIdSnake)
        let merchant = try c.decodeIfPresent(Merchant.self, forKey: .merchant)

        self.transactionId = transactionId
        self.apiId = apiId
        self.amount = amount
        self.currency = currency
        self.bookingDate = bookingDate
        self.descriptionDisplay = descriptionDisplay
        self.descriptionRaw = descriptionRaw
        self.legacyDescription = legacyDescription
        self.direction = direction
        self.merchantId = merchantId
        self.merchant = merchant
        self.id = transactionId ?? apiId ?? UUID().uuidString
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(transactionId, forKey: .transactionId)
        try container.encodeIfPresent(apiId, forKey: .apiId)
        try container.encodeIfPresent(amount, forKey: .amount)
        try container.encodeIfPresent(currency, forKey: .currency)
        try container.encodeIfPresent(bookingDate, forKey: .bookingDate)
        try container.encodeIfPresent(descriptionDisplay, forKey: .descriptionDisplay)
        try container.encodeIfPresent(descriptionRaw, forKey: .descriptionRaw)
        try container.encodeIfPresent(legacyDescription, forKey: .legacyDescription)
        try container.encodeIfPresent(direction, forKey: .direction)
        try container.encodeIfPresent(merchantId, forKey: .merchantId)
        try container.encodeIfPresent(merchant, forKey: .merchant)
    }

    var bookingDateAsDate: Date? {
        guard let bookingDate else { return nil }
        return Self.bookingDateFormatter.date(from: bookingDate)
    }

    init(
        transactionId: String? = nil,
        apiId: String? = nil,
        amount: Double? = nil,
        currency: String? = nil,
        bookingDate: String? = nil,
        descriptionDisplay: String? = nil,
        descriptionRaw: String? = nil,
        description: String? = nil,
        direction: String? = nil,
        merchantId: String? = nil,
        merchant: Merchant? = nil
    ) {
        self.transactionId = transactionId
        self.apiId = apiId
        self.amount = amount
        self.currency = currency
        self.bookingDate = bookingDate
        self.descriptionDisplay = descriptionDisplay
        self.descriptionRaw = descriptionRaw
        self.legacyDescription = description
        self.direction = direction
        self.merchantId = merchantId
        self.merchant = merchant
        self.id = transactionId ?? apiId ?? UUID().uuidString
    }

    var displayDescription: String? {
        descriptionDisplay ?? legacyDescription ?? descriptionRaw
    }

    var displayTitle: String? {
        if let merchantName = merchant?.name {
            return merchantName
        }
        return descriptionDisplay ?? legacyDescription ?? descriptionRaw
    }

    var description: String? {
        displayDescription
    }

    private static func decodeId(from container: KeyedDecodingContainer<CodingKeys>, key: CodingKeys) -> String? {
        if let str = try? container.decodeIfPresent(String.self, forKey: key) {
            return str
        }
        if let intVal = try? container.decodeIfPresent(Int.self, forKey: key) {
            return String(intVal)
        }
        return nil
    }

    private static func decodeDescription(
        in container: KeyedDecodingContainer<CodingKeys>,
        camel: CodingKeys,
        snake: CodingKeys
    ) throws -> String? {
        if let camelValue = try container.decodeIfPresent(String.self, forKey: camel) {
            return camelValue
        }
        if let snakeValue = try container.decodeIfPresent(String.self, forKey: snake) {
            return snakeValue
        }
        return nil
    }

    private static let bookingDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()
}

struct TransactionCategory: Codable, Identifiable {
    let id: Int
    let transactionId: Int?
    let categoryId: Int?
    let categoryKey: String?
    let categoryName: String?
    let parentCategoryId: Int?
    let confidence: Double?
    let source: String?
    let primary: Bool?
}

struct RenameTransactionRequest: Codable {
    let descriptionDisplay: String
}

struct BulkRenameDescriptionRequest: Codable {
    let descriptionRaw: String
    let descriptionDisplay: String
}
