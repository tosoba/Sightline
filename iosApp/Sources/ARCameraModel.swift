import CoreGraphics
import Foundation
import Metal
import Observation
import Shared
import SwiftUI
import os

/// One marker as laid out by the overlay: the card rect is also the stencil hole the renderer punches.
struct ARPlacement: Identifiable, Equatable {
    let id: String
    let name: String
    let category: PlaceCategory
    let distanceMeters: Float
    let rect: CGRect
    let cornerRadius: CGFloat
}

/// Debug-only stand-in for a capture failure. Simulators have no camera device, so the OS cannot
/// drive the permission or configuration branches there; `-arUnavailable` injects the same handler.
enum ARSimulatedUnavailability: String {
    case permission
    case configuration
}

/// Why the page cannot show a live feed. Kept apart from the synthetic feed: a genuine no-camera
/// device still renders the generated texture, while these states show a message and keep the real
/// nearby places instead of the synthetic fixture.
enum ARCameraUnavailability: Equatable {
    case permissionDenied
    case configurationFailed(String)
}

struct ARCameraLaunchOptions {
    let isArEntry: Bool
    let forceSynthetic: Bool
    let attitude: ARScriptedAttitude?
    let blurStrength: Float?
    let blurEnabled: Bool
    let holeFill: ARHoleFillStyle
    let debugHUD: Bool
    let simulatedUnavailability: ARSimulatedUnavailability?

    static var current: ARCameraLaunchOptions { ARCameraLaunchOptions(arguments: ProcessInfo.processInfo.arguments) }

    /// `-arCamera` pins scripted launches to the AR page; without it the pager keeps its own default.
    var requestedPage: MainPage? { isArEntry ? .camera : nil }

    init(arguments: [String]) {
        func value(after flag: String) -> String? {
            guard let index = arguments.firstIndex(of: flag), arguments.indices.contains(index + 1) else {
                return nil
            }
            return arguments[index + 1]
        }

        isArEntry = arguments.contains("-arCamera")
        forceSynthetic = arguments.contains("-arSynthetic")
        attitude = value(after: "-arAttitude").flatMap(ARScriptedAttitude.init(rawValue:))
        blurStrength = value(after: "-arBlur").flatMap(Float.init)
        blurEnabled = !arguments.contains("-arNoBlur")
        holeFill = value(after: "-arHoleFill").flatMap(ARHoleFillStyle.init(rawValue:)) ?? .frosted
        debugHUD = arguments.contains("-arDebug")
        simulatedUnavailability = value(after: "-arUnavailable").flatMap(ARSimulatedUnavailability.init(rawValue:))
    }
}

/// States the AR camera screen: owns the Metal renderer, the capture session, the motion source and
/// the overlay layout. Main-thread confined, matching `ArProjection`'s scratch-state contract.
@MainActor
@Observable
final class ARCameraModel {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "arcamera")

    let renderer: ARCameraRenderer?
    let options: ARCameraLaunchOptions

    private let nearby: NearbyPlacesModel
    private let projection = ArProjection()
    private let tracker = ArOrientationTracker()
    private let motion = ARCameraMotion()
    private let capture = ARCameraCapture()

    private(set) var placements: [ARPlacement] = []
    private(set) var statusText = ""
    private(set) var orientationText = "-"
    private(set) var isSynthetic = false
    private(set) var isRunning = false
    private(set) var pitchOutOfRange = false
    private(set) var markerCount = 0
    private(set) var layoutDebugText = "-"
    private(set) var unavailability: ARCameraUnavailability?
    private(set) var errorMessage: String?

    var blurEnabled: Bool = true

    private var viewport: CGSize = .zero
    private var displayScale: CGFloat = 3
    private var orientation: Orientation?
    private var cameraLocation: GeoLocation?
    private var lastHoleRects: [ARHoleRect] = []

    private static let markerHeight: CGFloat = 88
    private static let markerSpacing: CGFloat = 12
    private static let cornerRadius: CGFloat = 16
    private static let maxMarkers = 8
    private static let pitchLimitRadians = Double.pi / 4

    init(nearby: NearbyPlacesModel, options: ARCameraLaunchOptions = .current) {
        self.nearby = nearby
        self.options = options
        renderer = MTLCreateSystemDefaultDevice().flatMap { device in
            ARCameraRenderer(
                device: device,
                colorPixelFormat: .bgra8Unorm,
                stencilPixelFormat: .stencil8
            )
        }
        if renderer == nil {
            errorMessage = "Metal is unavailable on this device."
        }
        blurEnabled = options.blurEnabled
    }

    // MARK: - Lifecycle

    func setActive(_ active: Bool) {
        guard active != isRunning else { return }
        if active {
            start()
        } else {
            stop()
        }
    }

    private func start() {
        guard let renderer else { return }
        isRunning = true
        unavailability = nil
        renderer.holeFillStyle = options.holeFill
        renderer.setContrastingColor(red: 0.22, green: 0.22, blue: 0.26)

        let useSynthetic = options.forceSynthetic || !ARCameraCapture.isCameraAvailable
        isSynthetic = useSynthetic
        if useSynthetic {
            cameraLocation = Self.syntheticCameraLocation
            renderer.setSyntheticTexture(ARCameraSyntheticFeed.makeTexture(device: renderer.device))
            statusText = "Synthetic feed"
        } else {
            renderer.setSyntheticTexture(nil)
            capture.onUnavailable = { [weak self] failure in
                self?.handle(failure)
            }
            capture.start(renderer: renderer)
            cameraLocation = nearby.currentLocation
            statusText = "Live camera"
        }
        Self.log.info(
            "start: \(useSynthetic ? "synthetic" : "live", privacy: .public) feed, camera \(ARCameraCapture.isCameraAvailable ? "present" : "absent", privacy: .public)"
        )

        if let simulated = options.simulatedUnavailability {
            switch simulated {
            case .permission:
                handle(.permissionDenied)
            case .configuration:
                handle(.configurationFailed("The camera session could not be configured."))
            }
        }

        motion.onRotationMatrix = { [weak self] matrix in
            self?.apply(matrix)
        }
        motion.start(scripted: options.attitude)

        if let strength = options.blurStrength {
            renderer.freezeBlur(strength: strength)
        } else {
            renderer.setBlurEnabled(blurEnabled, animated: true)
        }
        recompute()
    }

    private func stop() {
        isRunning = false
        motion.stop()
        capture.stop()
        renderer?.setBlurEnabled(false, animated: false)
        Self.log.info("stop")
    }

    private func handle(_ failure: ARCameraCaptureFailure) {
        switch failure {
        case .permissionDenied:
            unavailability = .permissionDenied
            statusText = "Camera access denied"
        case .configurationFailed(let message):
            unavailability = .configurationFailed(message)
            statusText = "Camera unavailable"
        }
        Self.log.notice("camera unavailable: \(self.statusText, privacy: .public)")
        isSynthetic = false
        renderer?.setSyntheticTexture(nil)
        cameraLocation = nearby.currentLocation
        recompute()
    }

    /// Called when the app returns to the foreground: a round trip through Settings should recover
    /// without leaving the page.
    func resumeAfterSettings() {
        guard unavailability == .permissionDenied,
              options.simulatedUnavailability == nil,
              let renderer,
              ARCameraCapture.isAuthorized else {
            return
        }
        Self.log.info("camera access granted while in Settings")
        unavailability = nil
        capture.start(renderer: renderer)
        cameraLocation = nearby.currentLocation
        statusText = "Live camera"
        recompute()
    }

    // MARK: - Inputs

    func updateViewport(_ size: CGSize, displayScale: CGFloat) {
        guard size.width > 0, size.height > 0 else { return }
        self.displayScale = displayScale
        guard size != viewport else { return }
        viewport = size
        recompute()
    }

    func placesChanged() {
        if !isSynthetic {
            cameraLocation = nearby.currentLocation
        }
        recompute()
    }

    func locationChanged() {
        guard !isSynthetic else { return }
        cameraLocation = nearby.currentLocation
        recompute()
    }

    func toggleBlur() {
        blurEnabled.toggle()
        renderer?.setBlurEnabled(blurEnabled, animated: true)
    }

    private func apply(_ matrix: RotationMatrix) {
        let next = tracker.update(rotationMatrix: matrix)
        orientation = next
        let pitchDegrees = Double(next.pitch) * 180 / .pi
        let outOfRange = abs(pitchDegrees) > Self.pitchLimitRadians * 180 / .pi
        if outOfRange != pitchOutOfRange {
            pitchOutOfRange = outOfRange
        }
        if options.debugHUD {
            func degrees(_ value: Double) -> String {
                guard value.isFinite else { return "-" }
                return String(format: "%.0f", value)
            }
            orientationText = "az \(degrees(Self.normalizedDegrees(Double(next.azimuth) * 180 / .pi)))"
                + "  pitch \(degrees(pitchDegrees))"
                + "  roll \(degrees(Double(next.roll) * 180 / .pi))"
        }
        recompute()
    }

    // MARK: - Layout

    private func recompute() {
        guard viewport.width > 0, viewport.height > 0 else { return }
        guard let location = cameraLocation else {
            placements = []
            markerCount = 0
            pushHoleRects([])
            return
        }
        guard let orientation else {
            placements = []
            markerCount = 0
            pushHoleRects([])
            return
        }

        projection.updateViewport(
            widthPx: Int32(viewport.width.rounded()),
            heightPx: Int32(viewport.height.rounded())
        )
        projection.updateCamera(
            location: location,
            orientation: orientation,
            screenRotation: .rotation0
        )

        let markerWidth = max(min(viewport.width * 0.42, 240), 150)
        let places: [Place] = isSynthetic ? Self.syntheticPlaces() : nearby.places
        var placements: [ARPlacement] = []
        for place in places {
            let placeLocation = GeoLocation(
                latitude: place.latitude,
                longitude: place.longitude,
                altitude: location.altitude + Self.altitudeOffset(for: place),
                horizontalAccuracyMeters: nil,
                timestampMillis: 0
            )
            let projected = projection.project(location: placeLocation)
            guard projected.isInFront, projected.isDrawn else { continue }
            let rect = projection.markerRect(
                marker: projected,
                widthPx: Float(markerWidth),
                heightPx: Float(Self.markerHeight),
                cornerRadiusPx: Float(Self.cornerRadius)
            )
            let frame = CGRect(
                x: CGFloat(rect.left),
                y: CGFloat(rect.top),
                width: CGFloat(rect.width),
                height: CGFloat(rect.height)
            )
            guard frame.maxY > 0, frame.minY < viewport.height else { continue }
            placements.append(
                ARPlacement(
                    id: place.id,
                    name: place.name,
                    category: place.category,
                    distanceMeters: placeLocation.distanceTo(other: location),
                    rect: frame,
                    cornerRadius: Self.cornerRadius
                )
            )
        }

        placements.sort { $0.distanceMeters < $1.distanceMeters }
        if placements.count > Self.maxMarkers {
            placements.removeLast(placements.count - Self.maxMarkers)
        }
        if pitchOutOfRange {
            placements = []
        }
        self.placements = placements
        markerCount = placements.count
        if options.debugHUD {
            layoutDebugText = Self.debugLayoutText(
                viewport: viewport,
                displayScale: displayScale,
                placements: placements
            )
        }

        let scale = displayScale
        pushHoleRects(
            placements.map { placement in
                ARHoleRect(
                    rect: placement.rect.applying(CGAffineTransform(scaleX: scale, y: scale)),
                    cornerRadius: Float(placement.cornerRadius * scale)
                )
            }
        )
    }

    private func pushHoleRects(_ rects: [ARHoleRect]) {
        guard rects != lastHoleRects else { return }
        lastHoleRects = rects
        renderer?.setHoleRects(rects)
    }

    /// Metrics are blanked when no placement survives, which is exactly the pitch-out-of-range and
    /// no-location states; formatting a missing rect would otherwise print a sentinel or `nan`.
    private static func debugLayoutText(
        viewport: CGSize,
        displayScale: CGFloat,
        placements: [ARPlacement]
    ) -> String {
        let viewportText = String(format: "vp %.0fx%.0f s%.0f", viewport.width, viewport.height, displayScale)
        guard !placements.isEmpty else { return "\(viewportText)  no placements" }

        func centre(_ placement: ARPlacement?) -> String {
            guard let placement, placement.rect.midX.isFinite, placement.rect.midY.isFinite else {
                return "-"
            }
            return String(format: "%.0f,%.0f", placement.rect.midX, placement.rect.midY)
        }

        return "\(viewportText)  m0 \(centre(placements.first))  m1 \(centre(placements.dropFirst().first))"
    }

    // MARK: - Synthetic fixtures

    private static let syntheticCameraLocation = GeoLocation(
        latitude: 50.0614,
        longitude: 19.9366,
        altitude: 210,
        horizontalAccuracyMeters: nil,
        timestampMillis: 0
    )

    private static let syntheticBearings: [Double] = [-12, 12]
    private static let syntheticDistances: [Double] = [140, 240]
    private static let syntheticAltitudes: [Double] = [10, -4]
    private static let syntheticNames = ["Cloth Hall", "St Mary's"]
    private static let syntheticCategories: [PlaceCategory] = [.sightsLandmarks, .artsCulture]

    private static func syntheticPlaces() -> [Place] {
        let camera = syntheticCameraLocation
        // The shared projection treats one degree of longitude as 111111 m (the Android behaviour);
        // mirroring that here keeps the fixture bearings exact.
        let metersPerDegree = 111_111.0
        return syntheticBearings.enumerated().map { index, bearingDegrees in
            let bearing = bearingDegrees * .pi / 180
            let distance = syntheticDistances[index % syntheticDistances.count]
            let latitude = camera.latitude + cos(bearing) * distance / metersPerDegree
            let longitude = camera.longitude + sin(bearing) * distance / metersPerDegree
            return Place(
                id: "synthetic-\(index)",
                name: syntheticNames[index % syntheticNames.count],
                latitude: latitude,
                longitude: longitude,
                category: syntheticCategories[index % syntheticCategories.count]
            )
        }
    }

    /// Synthetic-fixture altitude, used only to keep the two demo markers off the horizon line.
    private static func altitudeOffset(for place: Place) -> Double {
        let index = Int(place.id.split(separator: "-").last ?? "0") ?? 0
        return syntheticAltitudes[index % syntheticAltitudes.count]
    }

    private static func normalizedDegrees(_ degrees: Double) -> Double {
        let wrapped = degrees.truncatingRemainder(dividingBy: 360)
        return wrapped < 0 ? wrapped + 360 : wrapped
    }
}
