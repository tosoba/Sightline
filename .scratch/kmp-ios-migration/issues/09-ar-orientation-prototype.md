# 09: AR orientation remapping prototype

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** A throwaway Metal + Core Motion spike that confirms whether the Android rotation-vector orientation path maps correctly to CMMotionManager's `.xMagneticNorthZVertical` attitude for aligning AR markers with the camera. It produces a recorded finding (the correct remapping, or the exact axis/sign adjustment needed) that the full Metal port commits to, de-risking the top parity risk before any renderer work.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] A minimal Metal view renders the camera feed plus a test marker using CMMotionManager attitude.
- [x] The marker's screen position matches the equivalent Android behavior under the same device orientation, or the exact axis/sign remapping is documented.
- [x] The finding is recorded on a prototype branch and referenced from ticket 24, so the full port does not re-derive it.

## Comments

Built a throwaway Xcode prototype (`prototype/ar-orientation/`, branch `prototype/09-ar-orientation`) that renders the `AVCaptureSession` feed into an `MTKView` and projects a test marker through the Android `Math3D` chain using `CMMotionManager` `.xMagneticNorthZVertical` attitude. It compiles (`xcodebuild` for iOS Simulator succeeded).

The recorded finding (`.scratch/kmp-ios-migration/research/09-ar-orientation-prototype.md`) is the exact remapping, derived analytically and cross-checked against three physical poses:

```
let m = motion.attitude.rotationMatrix
let azimuth = atan2(-m.m12, m.m11)   // -> getOrientation values[0]
let pitch   = asin(-m.m13)           // -> getOrientation values[1]
let roll    = atan2(-m.m23, m.m33)   // -> getOrientation values[2]
```

These feed the unchanged shared `Math3D.getCamRotation` (`camRot.x = -pitch`, `camRot.y = azimuth + PI`, `screenRot = -roll`). The matrix form is recommended over the ZXY Euler form (which hits gimbal lock at the upright-portrait pose). On-device verification (camera + magnetometer) is the remaining human step. Referenced from ticket 24.
