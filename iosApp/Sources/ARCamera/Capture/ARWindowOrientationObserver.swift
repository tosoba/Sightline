import Shared
import UIKit

/// Watches the active window scene's interface orientation and reports Screen Rotation changes.
///
/// The interface orientation, not the raw device orientation, is what the SwiftUI viewport and the
/// AR projection are laid out against. Main-thread confined; the owner keeps the observer for the
/// lifetime of the capture session and forwards [onRotationChange] into its projection.
@MainActor
final class ARWindowOrientationObserver {
    private let center: NotificationCenter
    private var observers: [NSObjectProtocol] = []
    private var isGeneratingDeviceOrientationNotifications = false

    private(set) var rotation: ScreenRotation = .rotation0
    var onRotationChange: ((ScreenRotation) -> Void)?

    init(center: NotificationCenter = .default) {
        self.center = center
    }

    func start() {
        guard observers.isEmpty else { return }
        if !isGeneratingDeviceOrientationNotifications {
            UIDevice.current.beginGeneratingDeviceOrientationNotifications()
            isGeneratingDeviceOrientationNotifications = true
        }
        let names: [Notification.Name] = [
            UIDevice.orientationDidChangeNotification,
            UIApplication.didBecomeActiveNotification,
            UIWindow.didBecomeKeyNotification,
            UIScene.didActivateNotification,
        ]
        observers = names.map { name in
            center.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.refresh()
                }
            }
        }
    }

    func stop() {
        for observer in observers {
            center.removeObserver(observer)
        }
        observers.removeAll()
        if isGeneratingDeviceOrientationNotifications {
            UIDevice.current.endGeneratingDeviceOrientationNotifications()
            isGeneratingDeviceOrientationNotifications = false
        }
    }

    func refresh() {
        guard let scene = Self.activeWindowScene(),
              let mapped = Self.orientationMapping(
                  for: scene.effectiveGeometry.interfaceOrientation
              )
        else {
            onRotationChange?(rotation)
            return
        }
        rotation = mapped
        onRotationChange?(mapped)
    }

    private static func activeWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first { $0.activationState == .foregroundActive }
            ?? scenes.first { $0.activationState == .foregroundInactive }
            ?? scenes.first
    }

    private static func orientationMapping(
        for orientation: UIInterfaceOrientation
    ) -> ScreenRotation? {
        switch orientation {
        case .portrait: .rotation0
        case .landscapeRight: .rotation90
        case .landscapeLeft: .rotation270
        case .portraitUpsideDown: .rotation180
        default: nil
        }
    }

    /// AVFoundation asks for the angle that keeps the delivered buffers upright in the current
    /// orientation; the mapping is fixed by the capture pipeline.
    static func videoRotationAngle(for rotation: ScreenRotation) -> CGFloat {
        switch rotation {
        case .rotation0: 90
        case .rotation90: 0
        case .rotation180: 270
        case .rotation270: 180
        @unknown default: 90
        }
    }
}
