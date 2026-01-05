import Foundation

extension Transaction {
    /// Returns the most reliable identifier to use for backend endpoints.
    var resolvedBackendId: String? {
        transactionId ?? apiId ?? (Int(id) != nil ? id : nil)
    }

    var currencySymbol: String {
        currency == "EUR" ? "€" : (currency ?? "")
    }

    func updatingDescriptionDisplay(_ description: String) -> Transaction {
        Transaction(
            transactionId: transactionId,
            apiId: apiId,
            amount: amount,
            currency: currency,
            bookingDate: bookingDate,
            descriptionDisplay: description,
            descriptionRaw: descriptionRaw,
            description: legacyDescription,
            direction: direction,
            merchantId: merchantId,
            merchant: merchant
        )
    }
}

