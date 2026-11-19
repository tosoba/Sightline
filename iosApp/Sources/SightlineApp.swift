import Shared
import SwiftUI

@main
struct SightlineApp: App {
    private let shared = SightlineShared.companion.create()

    var body: some Scene {
        WindowGroup {
            NearbyPlacesView(shared: shared)
        }
    }
}
