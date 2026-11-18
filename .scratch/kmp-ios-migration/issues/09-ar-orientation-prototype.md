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

### 2026-11-18 - simulator run and renderer fix

Ran the prototype on the iOS 27.0 simulator (iPhone 18 Pro) to sanity-check it beyond compile.

Two Metal debug-validation asserts surfaced, both on the path where no camera frame is available yet
(the simulator's virtual camera never delivers a frame, so `latestTexture` stays nil):

1. `missing Sampler binding at index 0 for smp[0]` - the fragment shader's texture and sampler were
   bound only when the texture existed while `drawPrimitives` ran unconditionally.
2. `Command encoder released without endEncoding` - the first fix created the render encoder before
   checking the texture, leaking an un-ended encoder on the texture-missing path.

Both are fixed by checking the camera texture exists and returning before creating the encoder or
command buffer.
See "Renderer pitfalls for ticket 24" in `.scratch/kmp-ios-migration/research/09-ar-orientation-prototype.md`.

Pixel analysis of the rendered simulator frame confirms the projection math at the identity pose:
the N marker lands at exact screen center and E/S/W are culled as behind the camera (`rotZ <= 0`).
The remapping itself and live camera alignment still require a real device with a magnetometer.
