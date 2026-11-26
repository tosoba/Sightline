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

struct ARCameraLaunchOptions {
    let isArEntry: Bool
    let forceSynthetic: Bool
    let attitude: ARScriptedAttitude?
    let blurStrength: Float?
    let forceBlur: Bool
    let forceNoBlur: Bool
    let holeFill: ARHoleFillStyle
    let debugHUD: Bool
    let simulatedUnavailability: ARSimulatedUnavailability?

    static var current: ARCameraLaunchOptions { ARCameraLaunchOptions(arguments: ProcessInfo.processInfo.arguments) }

    /// Normal viewing starts sharp; only the debug blur flags override the pitch and sheet triggers.
    var forcesBlur: Bool { forceBlur || (blurStrength ?? 0) > 0 }

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
        forceBlur = arguments.contains("-arForceBlur")
        forceNoBlur = arguments.contains("-arNoBlur")
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

    private(set) var blurEnabled = false
    private(set) var debugBlurForced = false
    private(set) var screenRotation: ScreenRotation = .rotation0
    private(set) var currentPage = 0
    private(set) var totalPages = 1

    /// Set by the Places & Location sheet owner whenever the sheet obscures the camera viewport.
    var sheetObscuresCamera = false {
        didSet {
            guard sheetObscuresCamera != oldValue else { return }
            updateBlurState()
            recompute()
        }
    }

    private var viewport: CGSize = .zero
    private var safeAreaInsets = EdgeInsets()
    private var overlayOrigin: CGPoint = .zero
    private var displayScale: CGFloat = 3
    private var orientation: Orientation?
    private var cameraLocation: GeoLocation?
    private var lastHoleRects: [ARHoleRect] = []
    private var lastDrawnIDs: Set<String> = []
    private var cachedSlots: CachedSlots?
    private var contrastingColor: (red: Float, green: Float, blue: Float)?
    private var orientationObservers: [NSObjectProtocol] = []
    private var isGeneratingOrientationNotifications = false

    private static let minMarkerHeight: CGFloat = 72
    private static let maxMarkerHeight: CGFloat = 88
    private static let minMarkerWidth: CGFloat = 150
    private static let maxMarkerWidth: CGFloat = 220
    private static let markerTargetWidth: CGFloat = 160
    private static let minMarkerWidthDivisor = 2
    private static let maxMarkerWidthDivisor = 6
    private static let markerVerticalSpacing: CGFloat = 16
    private static let markerOverlapGuardFraction: CGFloat = 0.5
    private static let markerTopPadding: CGFloat = 12
    private static let minMarkerRows = 1
    private static let maxMarkerRows = 6
    private static let cornerRadius: CGFloat = 16
    private static let pitchLimitDegrees: Double = 45
    /// Same reassign policy as Android's `ARMarkerRenderer`: a slot layout only depends on the
    /// device position, not on the camera orientation, so a small move reuses it.
    private static let locationReassignThresholdMeters: Float = 10

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
        blurEnabled = resolvedBlur
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
        observeOrientationChanges()
        beginGeneratingOrientationNotifications()

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
            cameraLocation = nearby.activeLocation
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
        motion.screenRotation = screenRotation
        motion.start(scripted: options.attitude)
        refreshScreenRotation()

        blurEnabled = resolvedBlur
        if let strength = options.blurStrength {
            renderer.freezeBlur(strength: options.forceNoBlur ? 0 : strength)
            // `freezeBlur` resets the animator, so restore the appearance-derived tint.
            if let color = contrastingColor {
                renderer.setContrastingColor(red: color.red, green: color.green, blue: color.blue)
            }
        } else {
            renderer.setBlurEnabled(blurEnabled, animated: true)
        }
        recompute()
    }

    private func stop() {
        isRunning = false
        motion.stop()
        capture.stop()
        removeOrientationObservers()
        endGeneratingOrientationNotifications()
        renderer?.clearCameraTexture()
        renderer?.setBlurEnabled(false, animated: false)
        Self.log.info("stop")
    }

    private func handle(_ failure: ARCameraUnavailability) {
        unavailability = failure
        switch failure {
        case .permissionDenied:
            statusText = "Camera access denied"
        case .configurationFailed:
            statusText = "Camera unavailable"
        }
        Self.log.notice("camera unavailable: \(self.statusText, privacy: .public)")
        isSynthetic = false
        renderer?.setSyntheticTexture(nil)
        cameraLocation = nearby.activeLocation
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
        cameraLocation = nearby.activeLocation
        statusText = "Live camera"
        recompute()
    }

    // MARK: - Inputs

    func updateViewport(
        _ size: CGSize,
        displayScale: CGFloat,
        safeAreaInsets: EdgeInsets = EdgeInsets(),
        overlayOrigin: CGPoint = .zero
    ) {
        guard size.width > 0, size.height > 0 else { return }
        self.displayScale = displayScale
        let geometryChanged =
            size != viewport
            || safeAreaInsets != self.safeAreaInsets
            || overlayOrigin != self.overlayOrigin
        viewport = size
        self.safeAreaInsets = safeAreaInsets
        self.overlayOrigin = overlayOrigin
        if geometryChanged {
            cachedSlots = nil
        }
        refreshScreenRotation()
        if geometryChanged {
            recompute()
        }
    }

    func placesChanged() {
        if !isSynthetic {
            cameraLocation = nearby.activeLocation
        }
        recompute()
    }

    func locationChanged() {
        guard !isSynthetic else { return }
        cameraLocation = nearby.activeLocation
        recompute()
    }

    func selectPage(_ page: Int) {
        let clamped = min(max(page, 0), max(totalPages - 1, 0))
        guard clamped != currentPage else { return }
        currentPage = clamped
        recompute()
    }

    /// Debug HUD affordance: forces full-frame blur the same way `-arForceBlur` does.
    func toggleBlur() {
        debugBlurForced.toggle()
        updateBlurState()
        recompute()
    }

    /// `secondarySystemBackground` resolved for the active appearance, eased by `ArBlurAnimator`.
    func updateContrastingColor(for colorScheme: ColorScheme) {
        let style: UIUserInterfaceStyle = colorScheme == .dark ? .dark : .light
        let color = UIColor.secondarySystemBackground.resolvedColor(
            with: UITraitCollection(userInterfaceStyle: style)
        )
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 1
        guard color.getRed(&red, green: &green, blue: &blue, alpha: &alpha) else {
            var white: CGFloat = 0
            guard color.getWhite(&white, alpha: &alpha) else { return }
            let component = Float(white)
            contrastingColor = (red: component, green: component, blue: component)
            renderer?.setContrastingColor(red: component, green: component, blue: component)
            return
        }
        let resolved = (red: Float(red), green: Float(green), blue: Float(blue))
        contrastingColor = resolved
        renderer?.setContrastingColor(red: resolved.red, green: resolved.green, blue: resolved.blue)
    }

    private func apply(_ matrix: RotationMatrix) {
        let next = tracker.update(rotationMatrix: matrix)
        orientation = next
        let pitchDegrees = Double(next.pitch) * 180 / .pi
        let outOfRange = abs(pitchDegrees) > Self.pitchLimitDegrees
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
        updateBlurState()
        recompute()
    }

    // MARK: - Blur

    private var resolvedBlur: Bool {
        guard !options.forceNoBlur else { return false }
        return options.forcesBlur || debugBlurForced || pitchOutOfRange || sheetObscuresCamera
    }

    private func updateBlurState() {
        let next = resolvedBlur
        guard next != blurEnabled else { return }
        blurEnabled = next
        if options.blurStrength == nil {
            renderer?.setBlurEnabled(next, animated: true)
        }
    }

    // MARK: - Screen rotation

    private func observeOrientationChanges() {
        guard orientationObservers.isEmpty else { return }
        let center = NotificationCenter.default
        let names: [Notification.Name] = [
            UIDevice.orientationDidChangeNotification,
            UIApplication.didBecomeActiveNotification,
            UIWindow.didBecomeKeyNotification,
            UIScene.didActivateNotification,
        ]
        orientationObservers = names.map { name in
            center.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.refreshScreenRotation()
                }
            }
        }
    }

    private func removeOrientationObservers() {
        let center = NotificationCenter.default
        for observer in orientationObservers {
            center.removeObserver(observer)
        }
        orientationObservers.removeAll()
    }

    private func beginGeneratingOrientationNotifications() {
        guard !isGeneratingOrientationNotifications else { return }
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        isGeneratingOrientationNotifications = true
    }

    private func endGeneratingOrientationNotifications() {
        guard isGeneratingOrientationNotifications else { return }
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
        isGeneratingOrientationNotifications = false
    }

    /// Reads the active window scene instead of the raw device orientation: the interface
    /// orientation is what the SwiftUI viewport and the AR projection are laid out against.
    func refreshScreenRotation() {
        guard let scene = activeWindowScene(),
              let mapping = Self.orientationMapping(
                  for: scene.effectiveGeometry.interfaceOrientation
              ) else {
            capture.setVideoRotationAngle(Self.videoRotationAngle(for: screenRotation))
            return
        }
        applyScreenRotation(mapping)
    }

    private func activeWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first { $0.activationState == .foregroundActive }
            ?? scenes.first { $0.activationState == .foregroundInactive }
            ?? scenes.first
    }

    private func applyScreenRotation(_ rotation: ScreenRotation) {
        capture.setVideoRotationAngle(Self.videoRotationAngle(for: rotation))
        motion.screenRotation = rotation
        guard rotation != screenRotation else { return }
        screenRotation = rotation
        cachedSlots = nil
        recompute()
    }

    private static func orientationMapping(
        for orientation: UIInterfaceOrientation
    ) -> ScreenRotation? {
        switch orientation {
        case .portrait: .rotation0
        case .landscapeRight: .rotation90
        case .landscapeLeft: .rotation270
        case .portraitUpsideDown: .rotation180
        @unknown default: nil
        }
    }

    private static func videoRotationAngle(for rotation: ScreenRotation) -> CGFloat {
        switch rotation {
        case .rotation0: 90
        case .rotation90: 0
        case .rotation180: 270
        case .rotation270: 180
        }
    }

    // MARK: - Layout

    private func recompute() {
        guard viewport.width > 0, viewport.height > 0 else { return }
        let layout = markerLayout()
        guard let location = cameraLocation, let orientation else {
            totalPages = 1
            apply(placements: [], layout: layout)
            return
        }

        projection.updateViewport(
            widthPx: Int32(viewport.width.rounded()),
            heightPx: Int32(viewport.height.rounded())
        )
        projection.updateCamera(
            location: location,
            orientation: orientation,
            screenRotation: screenRotation
        )

        let places: [Place] = isSynthetic ? Self.syntheticPlaces() : nearby.allPlaces
        let candidates = places.map { Self.candidate(for: $0, camera: location) }
        let assignment = slotAssignment(
            for: candidates,
            at: location,
            layout: layout
        )

        if assignment.didReassign, !lastDrawnIDs.isEmpty {
            currentPage = Self.followedPage(
                for: lastDrawnIDs,
                slots: assignment.slots,
                fallback: currentPage
            )
        }
        let maxPage = assignment.slots.values.map { Int($0.page) }.max() ?? 0
        currentPage = min(max(currentPage, 0), maxPage)
        totalPages = maxPage + 1

        guard !(pitchOutOfRange || sheetObscuresCamera) else {
            apply(placements: [], layout: layout)
            return
        }

        var placements: [ARPlacement] = []
        for candidate in candidates {
            guard let slot = assignment.slots[candidate.place.id],
                  Int(slot.page) == currentPage else { continue }
            let projected = projection.project(location: candidate.location)
            guard projected.isInFront, projected.isDrawn else { continue }
            let rect = projection.markerRect(
                marker: projected,
                widthPx: Float(layout.markerWidth),
                heightPx: Float(layout.markerHeight),
                cornerRadiusPx: Float(Self.cornerRadius)
            )
            let frame = CGRect(
                x: CGFloat(rect.left),
                y: CGFloat(slot.y) - layout.markerHeight / 2,
                width: CGFloat(rect.width),
                height: CGFloat(rect.height)
            )
            guard frame.maxY > 0, frame.minY < viewport.height else { continue }
            placements.append(
                ARPlacement(
                    id: candidate.place.id,
                    name: candidate.place.name,
                    category: candidate.place.category,
                    distanceMeters: candidate.target.distance,
                    rect: frame,
                    cornerRadius: Self.cornerRadius
                )
            )
        }

        placements.sort { $0.distanceMeters < $1.distanceMeters }
        lastDrawnIDs = Set(placements.map(\.id))
        apply(placements: placements, layout: layout)
    }

    private func apply(placements: [ARPlacement], layout: MarkerLayout) {
        self.placements = placements
        markerCount = placements.count
        if options.debugHUD {
            layoutDebugText = Self.debugLayoutText(
                layout: layout,
                displayScale: displayScale,
                screenRotation: screenRotation,
                currentPage: currentPage,
                totalPages: totalPages,
                placements: placements
            )
        }

        let scale = displayScale
        // The overlay lays cards out in the safe-area-respecting viewport, while the Metal view
        // ignores the safe area and fills the window. Translate into the drawable's space so the
        // stencil holes land exactly under the cards.
        let origin = overlayOrigin
        pushHoleRects(
            placements.map { placement in
                ARHoleRect(
                    rect: placement.rect
                        .offsetBy(dx: origin.x, dy: origin.y)
                        .applying(CGAffineTransform(scaleX: scale, y: scale)),
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

    private struct MarkerCandidate {
        let place: Place
        let location: GeoLocation
        let target: MarkerSlotTarget
    }

    private struct MarkerLayout: Equatable {
        let viewport: CGSize
        let safeAreaInsets: EdgeInsets
        let markerWidth: CGFloat
        let markerHeight: CGFloat
        let divisor: Int
        let rows: Int
        let baseY: CGFloat
        let verticalSpacing: CGFloat
    }

    private struct CachedSlots {
        let location: GeoLocation
        let layout: MarkerLayout
        let screenRotation: ScreenRotation
        let placeIDs: [String]
        let slots: [String: MarkerSlot]
    }

    private struct SlotAssignment {
        let slots: [String: MarkerSlot]
        let didReassign: Bool
    }

    private func markerLayout() -> MarkerLayout {
        let usableWidth = max(0, viewport.width - safeAreaInsets.leading - safeAreaInsets.trailing)
        let usableHeight = max(0, viewport.height - safeAreaInsets.top - safeAreaInsets.bottom)
        let markerHeight = Self.clamped(
            usableHeight / 6,
            Self.minMarkerHeight...Self.maxMarkerHeight
        )
        let rows = Int((usableHeight + Self.markerVerticalSpacing) / (markerHeight + Self.markerVerticalSpacing))
        let divisor = min(
            max(Int(usableWidth / Self.markerTargetWidth), Self.minMarkerWidthDivisor),
            Self.maxMarkerWidthDivisor
        )
        let markerWidth = Self.clamped(
            usableWidth / CGFloat(divisor),
            Self.minMarkerWidth...Self.maxMarkerWidth
        )
        return MarkerLayout(
            viewport: viewport,
            safeAreaInsets: safeAreaInsets,
            markerWidth: markerWidth,
            markerHeight: markerHeight,
            divisor: divisor,
            rows: min(max(rows, Self.minMarkerRows), Self.maxMarkerRows),
            baseY: safeAreaInsets.top + Self.markerTopPadding + markerHeight / 2,
            verticalSpacing: Self.markerVerticalSpacing
        )
    }

    private static func clamped(_ value: CGFloat, _ range: ClosedRange<CGFloat>) -> CGFloat {
        min(max(value, range.lowerBound), range.upperBound)
    }

    private func slotAssignment(
        for candidates: [MarkerCandidate],
        at location: GeoLocation,
        layout: MarkerLayout
    ) -> SlotAssignment {
        let placeIDs = candidates.map(\.place.id)
        if let cached = cachedSlots,
            cached.layout == layout,
            cached.screenRotation == screenRotation,
            cached.placeIDs == placeIDs,
            cached.location.distanceTo(other: location) <= Self.locationReassignThresholdMeters {
            return SlotAssignment(slots: cached.slots, didReassign: false)
        }

        let slots = MarkerSlotAssignment.shared.assign(
            povLocation: location,
            targets: candidates.map(\.target),
            geometry: MarkerSlotGeometry(
                markerWidthPx: Float(layout.markerWidth),
                markerHeightPx: Float(layout.markerHeight),
                overlapGuardPx: Float(layout.markerWidth * Self.markerOverlapGuardFraction),
                verticalSpacingPx: Float(layout.verticalSpacing),
                rows: Int32(layout.rows),
                baseY: Float(layout.baseY)
            ),
            canvasWidth: Int32(viewport.width.rounded()),
            canvasHeight: Int32(viewport.height.rounded())
        )
        cachedSlots = CachedSlots(
            location: location,
            layout: layout,
            screenRotation: screenRotation,
            placeIDs: placeIDs,
            slots: slots
        )
        return SlotAssignment(slots: slots, didReassign: true)
    }

    /// Follows the reassigned markers that were on screen: the page holding the most of them wins,
    /// and ties fall back to the lowest page so the result is deterministic.
    private static func followedPage(
        for previousIDs: Set<String>,
        slots: [String: MarkerSlot],
        fallback: Int
    ) -> Int {
        var counts: [Int: Int] = [:]
        for id in previousIDs {
            guard let slot = slots[id] else { continue }
            counts[Int(slot.page), default: 0] += 1
        }
        guard let best = counts.sorted(by: { lhs, rhs in
            lhs.value == rhs.value ? lhs.key < rhs.key : lhs.value > rhs.value
        }).first else {
            return fallback
        }
        return best.key
    }

    private static func candidate(for place: Place, camera: GeoLocation) -> MarkerCandidate {
        let location = place.location.withAltitude(
            altitude: camera.altitude + altitudeOffset(for: place)
        )
        return MarkerCandidate(
            place: place,
            location: location,
            target: MarkerSlotTarget(
                id: place.id,
                location: location,
                distance: location.distanceTo(other: camera)
            )
        )
    }

    /// Metrics are blanked when no placement survives, which is exactly the pitch-out-of-range,
    /// sheet-obscured and no-location states; formatting a missing rect would otherwise print a
    /// sentinel or `nan`. The marker geometry stays in the text so a debug run can check the sizing
    /// even when there is nothing to place.
    private static func debugLayoutText(
        layout: MarkerLayout,
        displayScale: CGFloat,
        screenRotation: ScreenRotation,
        currentPage: Int,
        totalPages: Int,
        placements: [ARPlacement]
    ) -> String {
        let viewportText =
            String(
                format: "vp %.0fx%.0f s%.0f mw %.0f d%d",
                layout.viewport.width,
                layout.viewport.height,
                displayScale,
                layout.markerWidth,
                layout.divisor
            )
            + " r\(rotationLabel(screenRotation)) p\(currentPage + 1)/\(totalPages)"
        guard !placements.isEmpty else { return "\(viewportText)  no placements" }

        func centre(_ placement: ARPlacement?) -> String {
            guard let placement, placement.rect.midX.isFinite, placement.rect.midY.isFinite else {
                return "-"
            }
            return String(format: "%.0f,%.0f", placement.rect.midX, placement.rect.midY)
        }

        return "\(viewportText)  m0 \(centre(placements.first))  m1 \(centre(placements.dropFirst().first))"
    }

    private static func rotationLabel(_ rotation: ScreenRotation) -> String {
        switch rotation {
        case .rotation0: "0"
        case .rotation90: "90"
        case .rotation180: "180"
        case .rotation270: "270"
        }
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
