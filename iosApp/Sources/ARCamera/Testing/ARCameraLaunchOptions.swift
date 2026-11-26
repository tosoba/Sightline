import Foundation
import Shared

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

/// Synthetic Krakow Places and the matching camera pose used by the simulator and scripted UI runs.
/// The projection treats one degree of longitude as 111111 m (the Android behaviour), so mirroring
/// that here keeps the fixture bearings exact.
enum ARCameraSyntheticFixtures {
    static let cameraLocation = GeoLocation(
        latitude: 50.0614,
        longitude: 19.9366,
        altitude: 210,
        horizontalAccuracyMeters: nil,
        timestampMillis: 0
    )

    private static let bearings: [Double] = [-12, 12]
    private static let distances: [Double] = [140, 240]
    private static let altitudes: [Double] = [10, -4]
    private static let names = ["Cloth Hall", "St Mary's"]
    private static let categories: [PlaceCategory] = [.sightsLandmarks, .artsCulture]
    private static let metersPerDegree = 111_111.0

    static func places() -> [Place] {
        bearings.enumerated().map { index, bearingDegrees in
            let bearing = bearingDegrees * .pi / 180
            let distance = distances[index % distances.count]
            let latitude = cameraLocation.latitude + cos(bearing) * distance / metersPerDegree
            let longitude = cameraLocation.longitude + sin(bearing) * distance / metersPerDegree
            return Place(
                id: "synthetic-\(index)",
                name: names[index % names.count],
                latitude: latitude,
                longitude: longitude,
                category: categories[index % categories.count]
            )
        }
    }

    /// Synthetic-fixture altitude, used only to keep the two demo markers off the horizon line.
    static func altitudeOffset(for place: Place) -> Double {
        let index = Int(place.id.split(separator: "-").last ?? "0") ?? 0
        return altitudes[index % altitudes.count]
    }
}
