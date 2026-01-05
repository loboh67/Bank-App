import Foundation

enum URLHelpers {
    static func sanitizedURL(from raw: String) -> URL? {
        if let direct = URL(string: raw) {
            return direct
        }
        if let encoded = raw.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) {
            return URL(string: encoded)
        }
        return nil
    }
}

