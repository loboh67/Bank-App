import Foundation
import Network
import Combine

/// Lightweight reachability helper to gate network calls and allow instant offline fallbacks.
final class NetworkMonitor {
    static let shared = NetworkMonitor()
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    private(set) var isOnline: Bool = true
    
    let onChange = PassthroughSubject<Bool, Never>()
    
    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let status = (path.status == .satisfied)
            self?.isOnline = status
            self?.onChange.send(status)
            NotificationCenter.default.post(name: .reachabilityChanged, object: status)
        }
        monitor.start(queue: queue)
    }
}
