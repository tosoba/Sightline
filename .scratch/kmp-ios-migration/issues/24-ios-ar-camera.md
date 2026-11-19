# 24: iOS AR camera (Metal)

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** An iOS user points the camera and sees the blurred live feed with unblurred holes around place markers, matching the Android AR effect to visual-equivalence parity, using the orientation remapping verified by the prototype.

**Blocked by:** 9 (prototype), 21 (ar KMP), 22 (shared + iosApp)

**Orientation remapping (from ticket 9):** `.scratch/kmp-ios-migration/research/09-ar-orientation-prototype.md`. The verified remapping is: `azimuth = atan2(-m.m12, m.m11)`, `pitch = asin(-m.m13)`, `roll = atan2(-m.m23, m.m33)` from `CMAttitude.rotationMatrix` (`.xMagneticNorthZVertical`), fed unchanged into the shared `Math3D.getCamRotation`. Use the matrix form, not ZXY Euler (gimbal lock at upright portrait). Prototype code: branch `prototype/09-ar-orientation`, `prototype/ar-orientation/`. See "Orientation finding" below: the shipped app uses `Mode.AR`, which needs a different remap than the `Mode.COMPASS` formula in that note.

**Status:** ready-for-human

- [x] The Metal pipeline (`AVCaptureSession` -> `CVMetalTextureCache` -> `MTKView`) renders the blurred feed.
  Verified on the simulator with the synthetic frame source, which is the only source a simulator can provide. The live `AVCaptureSession` -> `CVMetalTextureCache` leg is wired and compiles but cannot run without a camera: device-pending.
- [x] Rounded-rect holes punch around markers via stencil/scissor.
  Verified on the simulator: `MTLStencilDescriptor` (write `.replace`, read `.equal`), per-rect `MTLScissorRect`, and the `udRoundBox` SDF with `discard_fragment` outside the rounded rect.
- [x] The shared math is consumed through the framework (simd fallback only if the boundary proves awkward).
  No simd fallback for the projection chain: `ArProjection` wraps the shared `MarkerProjection`. simd appears only in the shader uniform struct and the aspect-fill UV matrix, which have no Kotlin counterpart.
- [ ] Marker alignment is correct across device orientations, to visual-equivalence parity.
  Verified analytically (Kotlin tests) and visually at scripted poses on the simulator. Real camera + magnetometer position matching on a physical device is the remaining human step, exactly as research 09 predicts.

## Comments

### What shipped

Two commits on `feature/24-ios-ar-camera`:

- `09a6e06 Expose shared AR math and blur state to iOS through the framework`
- `6760e19 Render the AR camera feed with a Metal blur and stencil holes`

The Camera page of `MainScreen`'s pager now hosts `ARCameraScreen`; capture and motion start on appearance and stop when the page changes or the view disappears.
`SightlineApp` and the map feature are untouched.
`NearbyPlacesView` is still content-only and compiling for ticket 25.
The only change to a ticket 23 file is `NearbyPlacesModel.currentLocation` becoming `private(set)` so the camera reads the already-observed fix instead of starting a second location stream.

Files, all owned by this ticket:

- `iosApp/Sources/ARCameraShaders.metal` - the seven GLSL shaders ported to MSL.
- `iosApp/Sources/ARCameraRenderer.swift` - Metal pipeline, textures, stencil holes, blur state.
- `iosApp/Sources/ARCameraCapture.swift` - `AVCaptureSession` + `AVCaptureVideoDataOutput`.
- `iosApp/Sources/ARCameraMotion.swift` - Core Motion and the scripted debug poses.
- `iosApp/Sources/ARCameraSyntheticFeed.swift` - generated test-pattern texture.
- `iosApp/Sources/ARCameraModel.swift` - observable state, launch options, overlay layout.
- `iosApp/Sources/ARCameraOverlay.swift` - SwiftUI `Canvas` marker cards.
- `iosApp/Sources/ARCameraScreen.swift` - page composition, Metal view, debug HUD.

### Metal pipeline

The GL draw order is preserved pass for pass.
Seven offscreen `MTLTexture` targets (`.bgra8Unorm`, `.private`, `.renderTarget | .shaderRead`) mirror the seven GL FBOs at `w/2`, `w/2`, `w/4`, `w/4`, `w/2`, `w/2`, `w`:

| pass | shader | target | sample spacing |
| --- | --- | --- | --- |
| 1 | vertical, camera texture | `w/2 x h/2` | `exp2(lod) / (h/2)` |
| 2 | horizontal | `w/2 x h/2` | `exp2(lod) / (w/2)` |
| 3 | vertical | `w/4 x h/4` | `exp2(lod) / (h/4)` |
| 4 | horizontal | `w/4 x h/4` | `exp2(lod) / (w/4)` |
| 5 | vertical | `w/2 x h/2` | `exp2(lod) / (h/2)` |
| 6 | horizontal | `w/2 x h/2` | `exp2(lod) / (w/2)` |
| 7 | vertical | `w x h` | `exp2(lod) / h` |
| 8 | horizontal | drawable | `exp2(lod) / w` |

Constants are verbatim: `sigma = 3`, `r = 2 * sigma`, five taps `i = 1..5`, `lod` in `-2..2` over 18 frames, contrasting mix `0..0.025` over 18 frames, colour easing over 60 frames, and the `contrastingColor == (-1,-1,-1)` sentinel.
GL's third `texture()` argument is a mip bias and the GL textures have no mip maps, so the Metal port samples the base level and keeps `lod` for the tap spacing only.
Sampler is linear/linear/clampToEdge with `mipFilter = .notMipmapped`.
The multiply-divided-by-alpha normalisation (`c / c.a`) is kept; it is a no-op for the opaque camera feed and normalises the weighted tap sum.

`AVCaptureVideoDataOutput` asks for `kCVPixelFormatType_32BGRA` and the connection uses `videoRotationAngle = 90` (portrait-only app), so the renderer normally receives upright buffers.
If a device ignores that and hands back the sensor-native landscape buffer, a 90 degree UV rotation fallback kicks in and logs a warning.
Centre-crop ("aspect fill") is a UV transform, `uv * min(surface/source) + (1 - scale)/2`, derived from the GL `calculateSurfaceTransform`; the vertex stage is a plain full-screen triangle.

Holes: a stencil-only pass sets `stencil = 1` per rect inside the `udRoundBox` SDF (colour writes masked off), then a sharp full-screen camera draw with `depthStencilState` compare `.equal` fills the holes.
This is the ticket's stated goal ("unblurred holes").
Android's literal draw order paints the max-lod blur plus contrasting tint *inside* the rounded rects, which is the frosted backing visible in `screenshots/camera_*.png`; `-arHoleFill frosted` reproduces that order exactly, `sharp` is the default.

The drawable carries a `.stencil8` attachment via `MTKView.depthStencilPixelFormat`.
Camera textures are wrapped with `CVMetalTextureCacheCreateTextureFromImage` and held until the `MTLCommandBuffer` completes, which keeps the zero-copy path safe.
The "no camera texture yet" path returns before any encoder or command buffer is created, and every created encoder is ended on all branches (the two pitfalls recorded in research 09).

### Shared facade API

`shared` now has `api(project(":core:ar"))` and `export(project(":core:ar"))`.
Three facades in `com.trm.sightline.shared`:

```kotlin
class ArProjection {                               // one per renderer, not thread-safe
  fun updateViewport(widthPx: Int, heightPx: Int)
  fun updateCamera(location: GeoLocation, orientation: Orientation, screenRotation: ScreenRotation)
  fun project(location: GeoLocation): ProjectedMarker
  fun projectAll(locations: List<GeoLocation>): List<ProjectedMarker>
  fun markerRect(marker: ProjectedMarker, widthPx: Float, heightPx: Float, cornerRadiusPx: Float): RoundedRectF
}

data class RotationMatrix(val m11..m33: Double)     // CMRotationMatrix flattened
class ArOrientationTracker {                       // remap + Android low-pass, one per renderer
  fun update(rotationMatrix: RotationMatrix): Orientation
  fun reset()
  companion object { fun remap(matrix: RotationMatrix): Orientation }
}

class ArBlurAnimator {                             // Android animated-lod state machine
  fun setBlurEnabled(enabled: Boolean, animated: Boolean)
  fun setContrastingColor(red: Float, green: Float, blue: Float)
  fun nextFrame(): ArBlurFrame
  fun reset()
}
```

`MarkerProjection` is stateful and scratch-based, so `ArProjection` documents and enforces exactly one instance per renderer, touched from one thread; on iOS the MTKView draws on the main thread and the overlay updates on the main actor, so both live there.
Across the boundary the `shared` functions are plain members; no `simd_double3` fallback was needed, and the boundary was not awkward for a handful of markers per frame (the CPU time below includes the Kotlin calls).

Tests, run on `iosSimulatorArm64`:

- `ArProjectionTest` (4): centre projection, camera replacement, ordered `projectAll` with behind-camera flags, hole-rect geometry. Expected values are hand-derived (a point dead ahead lands at the viewport centre; east is right).
- `ArOrientationTrackerTest` (11): upright north/east, flat, pitch-down, roll, first-reading pass-through, 0.12 smoothing, 18 degree snap, circular wrap at +/-pi, reset, independent pitch/roll smoothing.
- `ArBlurAnimatorTest` (8): initial state, idle, 18-frame ramp up, instant enable, animated disable, first colour immediate, 60-frame colour ease, reset.

### Orientation finding (read this before trusting research 09's formula)

Research 09 derived the remap for `OrientationManager.Mode.COMPASS` (`AXIS_Y`, `AXIS_MINUS_X`).
The shipped AR screen uses `OrientationManager.Mode.AR` (`AXIS_X`, `AXIS_Z`, see `feature/camera/.../CameraPreview.kt`), whose AOSP remap produces columns `[X | -Z | Y]`; the new device Y axis is the rear camera axis.
So the Android triple means azimuth = compass bearing of the camera direction, pitch = how far down the camera points, roll = screen roll about the camera axis, and the correct matrix form is:

```swift
azimuth = atan2(m32, -m31)
pitch   = asin(m33)
roll    = atan2(-m13, m23)
```

Applied verbatim on the COMPASS formula instead, an upright phone would map markers 90 degrees off.
The `Mode.AR` form is implemented in `ArOrientationTracker.remap`, verified against `Math3D` characterization expectations (azimuth 0 faces north, azimuth +pi/2 faces east), and the `-arAttitude` poses produce the expected screen shifts.
The remaining uncertainty is Core Motion's real reference frame on a device, which no simulator can exercise.

The Android low-pass is ported exactly: `SMOOTH_FACTOR = 0.12`, `SMOOTH_THRESHOLD = 2*pi/20`, with the same circular-wrap branch.

### Simulator verification

The simulator has no camera and no magnetometer, so the app selects the synthetic path automatically (`ARCameraCapture.isCameraAvailable == false`) and the scripted attitude path (`CMMotionManager.isDeviceMotionAvailable == false`).
Launch arguments:

- `-arSynthetic` force the generated test-pattern texture and the fixed Krakow fixture (two places at +/-12 degrees, three markers would overlap in the narrow projection FOV).
- `-arAttitude uprightNorth|uprightLeft|uprightRight|tilted|flat|sweep` scripted poses.
- `-arBlur 0..1` freeze the Android 18-frame ramp at that strength (deterministic screenshots).
- `-arHoleFill sharp|frosted` hole fill mode.
- `-arNoBlur` start sharp, `-arDebug` show the HUD (angles, viewport, rect centres, lod, GPU/CPU ms, marker count, blur toggle).

Screenshots, all in `screenshots/`, all captured on iPhone 17 / iOS 27.2:

- `ios_ar_lod_low.png` - animated blur mid-ramp (`lod -0.00`), visible in the HUD.
- `ios_ar_lod_high.png` - animation complete (`lod +2.00`), full-strength multi-resolution blur.
- `ios_ar_holes.png` - clean shot of the sharp rounded-rect holes inside the heavy blur, no HUD.
- `ios_ar_attitude_left.png` - bearing 350 degrees; both cards shift right by exactly `tan(10 deg) * (w+h)/2` (HUD reports rect centres 183 and 408 against 92/310 upright).
- `ios_ar_attitude_right.png` - bearing 10 degrees; the mirror shift.
- `ios_ar_tilted.png` - pitch 17, roll -8; cards rise and rotate with the projected field.
- `ios_ar_frosted.png` - Android-literal hole fill (max-lod blur plus contrast tint inside the rounded rect), matching `screenshots/camera_*.png`.
- `ios_ar_flat.png` - pitch 90; markers are hidden by the 45 degree pitch limit and the "hold upright" hint shows.
- `ios_ar_default.png` - the default animated sweep with a sharp hole.

HUD frame timings while sweeping both hole styles at 402x621 points (1206x1863 drawable pixels): GPU 0.22-0.62 ms per frame, CPU 2.0-4.2 ms including the Kotlin projection calls.
That is `MTLCommandBuffer.gpuStartTime/gpuEndTime` and a `CACurrentMediaTime` bracket, shown live in the HUD.

Validation: launches with `SIMCTL_CHILD_MTL_DEBUG_LAYER=1` log `Metal API Validation Enabled` and produce no Metal errors, no command-buffer errors and no validation assertions, for both `sharp` and `frosted`.
`MTL_SHADER_VALIDATION=1` (GPU shader validation) fails pipeline creation on this simulator with `buffer binding has argument index 31 that is greater than 30`; this is a known simulator-only limitation of that extra layer, not an app error, and it is not part of the gate.
The shader compiles cleanly with `xcrun -sdk iphonesimulator metal -c iosApp/Sources/ARCameraShaders.metal`.

### What remains device-only

- Live `AVCaptureSession` frames and the documented `videoRotationAngle = 90` portrait delivery, plus the landscape UV fallback.
- Core Motion `.xMagneticNorthZVertical` in the real reference frame; the remap is derived and pose-tested, but magnetometer sign/offset can only be confirmed on hardware.
- Physical camera/scene alignment through the projection, i.e. the actual position-matching that research 09 flags as the human step.
- Camera permission prompt.

### Other findings

- Android divides the contrasting colour components by 256 (`setContrastingColor(red = color.red.toInt())` -> 0 or 1, then `/256`). The iOS side passes the 0..1 components directly; with `sharp` holes the tint is unused anyway.
- Xcode 27 needs the Metal toolchain component before any `.metal` file compiles: `xcodebuild -downloadComponent MetalToolchain` (838 MB). Any other worktree/CI machine will need the same step.

### Run commands

```sh
# shared facade + tests
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain

# Android stays green
./gradlew :app:assembleDebug test --console=plain

# app
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build

# install and drive the simulator
APP=$(ls -dt ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator | head -1)/Sightline.app
xcrun simctl boot "iPhone 17"
xcrun simctl install booted "$APP"
SIMCTL_CHILD_MTL_DEBUG_LAYER=1 xcrun simctl launch booted com.trm.sightline \
  -arSynthetic -arAttitude uprightNorth -arBlur 1 -arHoleFill sharp -arDebug
xcrun simctl io booted screenshot screenshots/ios_ar_holes.png
```

### Risk to ticket 25

Low.
Ticket 25 can push the category screen from `MainScreen` and keep `NearbyPlacesView` as-is; the AR screen takes the shared `NearbyPlacesModel` and does not own navigation.
Two things to preserve: `MainScreen` must keep passing `isActive: page == .camera` so capture/motion stop on the Map page, and `NearbyPlacesModel.currentLocation` must stay readable.
The map page and its `:syncIosMapStyles` pre-build step are untouched.
