import Shared
import SwiftUI
import os

@main
struct SightlineApp: App {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "app")

    private let shared = SightlineShared.companion.create()
    private let resetsPreferences = ProcessInfo.processInfo.arguments.contains("-resetPreferences")

    @State private var isReady = false

    var body: some Scene {
        WindowGroup {
            Group {
                if isReady {
                    MainScreen(shared: shared)
                } else {
                    ProgressView()
                }
            }
            .task {
                guard !isReady else { return }
                if resetsPreferences {
                    do {
                        try await shared.resetPreferencesForTesting()
                        Self.log.info("reset shared preferences for a UI test launch")
                    } catch {
                        Self.log.error(
                            "resetting shared preferences failed: \(error.localizedDescription, privacy: .public)"
                        )
                    }
                }
                isReady = true
            }
        }
    }
}
