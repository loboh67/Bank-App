import SwiftUI

struct OfflineBadge: View {
    let isOffline: Bool
    
    var body: some View {
        Group {
            if isOffline {
                HStack(spacing: 4) {
                    Image(systemName: "wifi.slash")
                    Text("Offline")
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.orange.opacity(0.15))
                .foregroundColor(.orange)
                .clipShape(Capsule())
            } else {
                EmptyView()
            }
        }
        .animation(.easeInOut(duration: 0.5), value: isOffline)
    }
}
