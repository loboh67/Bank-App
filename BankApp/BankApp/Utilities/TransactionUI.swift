import SwiftUI

enum TransactionUI {
    static func amountColor(direction: String?) -> Color {
        switch direction?.uppercased() {
        case "CREDIT", "IN":
            return .green
        case "DEBIT", "OUT":
            return .red
        default:
            return .primary
        }
    }

    static func signedAmountText(_ amount: Double, direction: String?) -> String {
        switch direction?.uppercased() {
        case "CREDIT", "IN":
            return "+\(String(format: "%.2f", amount))"
        case "DEBIT", "OUT":
            return "-\(String(format: "%.2f", amount))"
        default:
            return String(format: "%.2f", amount)
        }
    }
}

