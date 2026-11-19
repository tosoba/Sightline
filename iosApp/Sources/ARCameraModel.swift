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

struct ARCameraLaunchOptions {
    let isArEntry: Bool
    let forceSynthetic: Bool
    let attitude: ARScriptedAttitude?
    let blurStrength: Float?
    let blurEnabled: Bool
    let holeFill: ARHoleFillStyle
    let debugHUD: Bool

    static var current: ARCameraLaunchOptions { ARCameraLaunchOptions(arguments: ProcessInfo.processInfo.arguments) }

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
        holeFill = value(after: "-arHoleFill").flatMap(ARHoleFillStyle.init(rawValue:)) ?? .sharp
        debugHUD = arguments.contains("-arDebug")
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
    private(set) var errorMessage: String?

    var blurEnabled: Bool = true

    private var viewport: CGSize = .zero
    private var displayScale: CGFloat = 3
    private var orientation: Orientation?
    private var cameraLocation: GeoLocation?
    private var syntheticLocation: GeoLocation?
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
        renderer.holeFillStyle = options.holeFill
        renderer.setContrastingColor(red: 0.22, green: 0.22, blue: 0.26)

        let useSynthetic = options.forceSynthetic || !ARCameraCapture.isCameraAvailable
        isSynthetic = useSynthetic
        if useSynthetic {
            syntheticLocation = Self.syntheticCameraLocation
            cameraLocation = syntheticLocation
            renderer.setSyntheticTexture(ARCameraSyntheticFeed.makeTexture(device: renderer.device))
            statusText = "Synthetic feed"
        } else {
            capture.onUnavailable = { [weak self] reason in
                self?.statusText = reason
                self?.isSynthetic = true
            }
            capture.start(renderer: renderer)
            cameraLocation = nearby.currentLocation
            statusText = "Live camera"
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
            orientationText = String(
                format: "az %.0f  pitch %.0f  roll %.0f",
                Self.normalizedDegrees(Double(next.azimuth) * 180 / .pi),
                pitchDegrees,
                Double(next.roll) * 180 / .pi
            )
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
            layoutDebugText = String(
                format: "vp %.0fx%.0f s%.0f  m0 %.0f,%.0f  m1 %.0f,%.0f",
                viewport.width,
                viewport.height,
                displayScale,
                placements.first?.rect.midX ?? -1,
                placements.first?.rect.midY ?? -1,
                placements.dropFirst().first?.rect.midX ?? -1,
                placements.dropFirst().first?.rect.midY ?? -1
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
