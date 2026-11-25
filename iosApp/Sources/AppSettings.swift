import UIKit

/// The app's Settings page. Shared by the camera denial panel and the location settings alert.
enum AppSettings {
    static func open() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}
