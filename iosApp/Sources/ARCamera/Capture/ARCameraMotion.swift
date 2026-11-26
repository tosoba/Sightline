import CoreMotion
import Foundation
import os
import QuartzCore
import Shared

/// Scripted poses for the simulator (no magnetometer) and for on-device debugging.
enum ARScriptedAttitude: String, CaseIterable {
    case uprightNorth
    case uprightLeft
    case uprightRight
    case tilted
    case flat
    case sweep
}

/// Feeds Core Motion attitude into the shared remapping.
///
/// The production path is `CMMotionManager` in `.xMagneticNorthZVertical`; `CMAttitude.rotationMatrix`
/// maps the reference frame onto the device frame and is passed to `ArOrientationTracker` unchanged.
/// The scripted path fabricates matrices from the same pose geometry so the simulator can verify
/// alignment visually; it is debug scaffolding, never a second projection implementation.
final class ARCameraMotion {
    private static let log = Logger(subsystem: "com.trm.sightline", category: "arcamera.motion")

    private let manager = CMMotionManager()
    private var timer: Timer?
    private var scriptedStart = CACurrentMediaTime()

    /// Scripted poses describe a portrait hold; offsetting the roll by the Screen Rotation makes the
    /// simulator feed the same rolled orientation a physically rotated device would report.
    var screenRotation: ScreenRotation = .rotation0

    var onRotationMatrix: ((RotationMatrix) -> Void)?
    var onScriptedPose: ((String) -> Void)?

    var isDeviceMotionAvailable: Bool {
        manager.isDeviceMotionAvailable
    }

    func start(scripted: ARScriptedAttitude?) {
        stop()
        if let scripted {
            startScripted(scripted)
            return
        }
        guard manager.isDeviceMotionAvailable else {
            startScripted(.sweep)
            return
        }
        manager.deviceMotionUpdateInterval = 1.0 / 50.0
        manager.startDeviceMotionUpdates(using: .xMagneticNorthZVertical, to: .main) { [weak self] motion, error in
            guard let self else { return }
            if let error {
                Self.log.error("device motion failed: \(error.localizedDescription, privacy: .public)")
                return
            }
            guard let matrix = motion?.attitude.rotationMatrix else { return }
            self.onRotationMatrix?(
                RotationMatrix(
                    m11: matrix.m11,
                    m12: matrix.m12,
                    m13: matrix.m13,
                    m21: matrix.m21,
                    m22: matrix.m22,
                    m23: matrix.m23,
                    m31: matrix.m31,
                    m32: matrix.m32,
                    m33: matrix.m33
                )
            )
        }
    }

    func stop() {
        manager.stopDeviceMotionUpdates()
        timer?.invalidate()
        timer = nil
    }

    private func startScripted(_ pose: ARScriptedAttitude) {
        scriptedStart = CACurrentMediaTime()
        onScriptedPose?(pose.rawValue)
        emitScripted(pose)
        timer = Timer.scheduledTimer(withTimeInterval: 1.0 / 30.0, repeats: true) { [weak self] _ in
            self?.emitScripted(pose)
        }
    }

    private func emitScripted(_ pose: ARScriptedAttitude) {
        let elapsed = CACurrentMediaTime() - scriptedStart
        let rollOffset = Double(screenRotation.rollOffsetRadians)
        let matrix: RotationMatrix
        switch pose {
        case .uprightNorth:
            matrix = Self.pose(bearing: 0, pitchDown: 0, roll: rollOffset)
        case .uprightLeft:
            matrix = Self.pose(bearing: -.pi / 18, pitchDown: 0, roll: rollOffset)
        case .uprightRight:
            matrix = Self.pose(bearing: .pi / 18, pitchDown: 0, roll: rollOffset)
        case .tilted:
            matrix = Self.pose(bearing: 0, pitchDown: 0.30, roll: 0.14 + rollOffset)
        case .flat:
            matrix = Self.flat()
        case .sweep:
            // Oscillate inside the marker field so the alignment can be watched live.
            matrix = Self.pose(
                bearing: sin(elapsed * 0.8) * (25.0 * .pi / 180.0),
                pitchDown: 0,
                roll: rollOffset
            )
        }
        onRotationMatrix?(matrix)
    }

    // MARK: - Pose geometry (debug scaffolding)

    //
    // Device axes are built in ENU and converted to the Core Motion reference frame
    // (north, west, up) row by row. Camera direction is -Z.

    private static func pose(bearing b: Double, pitchDown delta: Double, roll phi: Double) -> RotationMatrix {
        let cosB = cos(b), sinB = sin(b)
        let cosD = cos(delta), sinD = sin(delta)
        let cosR = cos(phi), sinR = sin(phi)

        let right = SIMD3<Double>(cosB, -sinB, 0)
        let out = SIMD3<Double>(-sinB * cosD, -cosB * cosD, sinD)
        let top = SIMD3<Double>(sinB * sinD, cosB * sinD, cosD)

        let rolledRight = right * cosR + top * sinR
        let rolledTop = top * cosR - right * sinR
        return matrix(right: rolledRight, top: rolledTop, out: out)
    }

    private static func flat() -> RotationMatrix {
        matrix(
            right: SIMD3<Double>(1, 0, 0),
            top: SIMD3<Double>(0, 1, 0),
            out: SIMD3<Double>(0, 0, 1)
        )
    }

    private static func matrix(right: SIMD3<Double>, top: SIMD3<Double>, out: SIMD3<Double>) -> RotationMatrix {
        func row(_ axis: SIMD3<Double>) -> (Double, Double, Double) {
            (axis.y, -axis.x, axis.z)
        }
        let r = row(right)
        let u = row(top)
        let o = row(out)
        return RotationMatrix(
            m11: r.0,
            m12: r.1,
            m13: r.2,
            m21: u.0,
            m22: u.1,
            m23: u.2,
            m31: o.0,
            m32: o.1,
            m33: o.2
        )
    }
}
