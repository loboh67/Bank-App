extension BankAccount {
    static let preview: BankAccount = BankAccount(
        id: 1,
        name: "Main Account",
        iban: "PT50 0000 0000 0000 0000 0000 1"
    )
}

extension Transaction {
    static let previewList: [Transaction] = [
        Transaction(
            transactionId: "tx_1",
            amount: 12.34,
            currency: "EUR",
            bookingDate: "2025-12-10",
            description: "Coffee shop",
            direction: "DEBIT",
            merchantId: "merchant_1",
            merchant: Merchant(
                id: "merchant_1",
                name: "Coffee Spot",
                logoUrl: "https://via.placeholder.com/64"
            )
        ),
        Transaction(
            transactionId: "tx_2",
            amount: 123.45,
            currency: "EUR",
            bookingDate: "2025-12-10",
            description: "Salary",
            direction: "CREDIT"
        ),
        Transaction(
            transactionId: "tx_3",
            amount: 50.00,
            currency: "EUR",
            bookingDate: "2025-12-09",
            description: "Groceries",
            direction: "DEBIT"
        )
    ]
}

extension TransactionCategory {
    static let previewList: [TransactionCategory] = [
        TransactionCategory(
            id: 1,
            transactionId: 1,
            categoryId: 100,
            categoryKey: "food",
            categoryName: "Food & Dining",
            parentCategoryId: nil,
            confidence: 0.92,
            source: "Model",
            primary: true
        ),
        TransactionCategory(
            id: 2,
            transactionId: 1,
            categoryId: 110,
            categoryKey: "coffee",
            categoryName: "Coffee",
            parentCategoryId: 100,
            confidence: 0.73,
            source: "Model",
            primary: false
        )
    ]
}
