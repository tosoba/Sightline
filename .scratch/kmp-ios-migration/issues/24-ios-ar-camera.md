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

Commits on `feature/24-ios-ar-camera`:

- `Expose shared AR math and blur state to iOS through the framework`
- `Render the AR camera feed with a Metal blur and stencil holes`
- `Record the iOS AR camera port, its verification and the Mode.AR remap finding`

Plus the routine `integration/kmp-ios-migration` merge (ticket 23 landed there).

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

Holes: a stencil-only pass sets `stencil = 1` per rect inside the `udRoundBox` SDF (colour writes masked off), then a fill draw with `depthStencilState` compare `.equal` paints the holes.
This is the ticket's stated goal ("unblurred holes").
Android's literal draw order paints the max-lod blur plus contrasting tint *inside* the rounded rects, which is the frosted backing visible in `screenshots/camera_*.png`; `frosted` reproduces that order exactly and is the default, `-arHoleFill sharp` keeps the literal unblurred holes for comparison.

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

- `-arCamera` open the app on the camera page (also the pager's ordinary first page, so the flag only matters if that default changes).
- `-arSynthetic` force the generated test-pattern texture and the fixed Krakow fixture (two places at +/-12 degrees, three markers would overlap in the narrow projection FOV).
- `-arAttitude uprightNorth|uprightLeft|uprightRight|tilted|flat|sweep` scripted poses.
- `-arBlur 0..1` freeze the Android 18-frame ramp at that strength (deterministic screenshots).
- `-arHoleFill frosted|sharp` hole fill mode; `frosted` is the default.
- `-arNoBlur` start sharp, `-arDebug` show the HUD (angles, viewport, rect centres, lod, GPU/CPU ms, marker count, blur toggle).
- `-arUnavailable permission|configuration` debug stand-in for a capture failure: simulators have no camera device, so the OS cannot drive the denial or `configure()` branches there. It calls the same model handler, so the unavailable UI is verifiable without hardware.

Screenshots, all in `screenshots/`, all captured on iPhone 17 / iOS 27.2:

- `ios_ar_frosted_default.png` - the default path: no hole-fill flag, `frosted` fill at full blur, no HUD.
- `ios_ar_frosted_default_hud.png` - the same frame with the HUD, whose mode label reads `frosted`.
- `ios_ar_lod_low.png` - animated blur mid-ramp (`lod -0.00`), visible in the HUD.
- `ios_ar_lod_high.png` - animation complete (`lod +2.00`), full-strength multi-resolution blur.
- `ios_ar_holes.png` - clean shot of the sharp rounded-rect holes inside the heavy blur, no HUD (`-arHoleFill sharp`).
- `ios_ar_attitude_left.png` - bearing 350 degrees; both cards shift right by exactly `tan(10 deg) * (w+h)/2` (HUD reports rect centres 183 and 408 against 92/310 upright).
- `ios_ar_attitude_right.png` - bearing 10 degrees; the mirror shift.
- `ios_ar_tilted.png` - pitch 17, roll -8; cards rise and rotate with the projected field.
- `ios_ar_frosted.png` - the same frame with `-arHoleFill frosted` passed explicitly, for comparison with the pre-polish shot.
- `ios_ar_flat.png` - pitch 90; markers are hidden by the 45 degree pitch limit and the "hold upright" hint shows.
- `ios_ar_flat_hud.png` - pitch 90 with the HUD: placement metrics read `no placements`, no `nan`.
- `ios_ar_default.png` - the default animated sweep on the frosted default.
- `ios_ar_merge.png` - the two-marker frame recorded for the merge run, regenerated on the frosted default.
- `ios_ar_permission_denied.png` - `-arUnavailable permission` after `simctl privacy revoke camera`: the panel, the Settings button, and real OSM places (Skarbonka, Akcesja, Walenty...) rather than the Krakow fixture over black.
- `ios_ar_configuration_failed.png` - `-arUnavailable configuration`: the same panel with the configure() message and no Settings button.

HUD frame timings while sweeping both hole styles at 402x621 points (1206x1863 drawable pixels): GPU 0.2-0.6 ms per frame, and 2.0-4.7 ms of CPU inside the `draw(in:)` call.
The GPU figure is `MTLCommandBuffer.gpuStartTime/gpuEndTime`; the CPU figure is a `CACurrentMediaTime` bracket around the draw.
The Kotlin projection calls happen on the motion callback, outside that bracket, and were not measured separately; two markers per frame is well below the draw cost either way.

Validation: launches with `SIMCTL_CHILD_MTL_DEBUG_LAYER=1` log `Metal API Validation Enabled` and produce no Metal errors, no command-buffer errors and no validation assertions, for both `sharp` and `frosted`.
`MTL_SHADER_VALIDATION=1` (GPU shader validation) fails pipeline creation on this simulator with `buffer binding has argument index 31 that is greater than 30`; that extra layer is reported to be unqualified on the iOS 27 simulator and is not part of the gate.
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
xcrun simctl launch booted com.trm.sightline \
  -arCamera -arSynthetic -arAttitude uprightNorth -arBlur 1 -arHoleFill sharp -arDebug
xcrun simctl io booted screenshot screenshots/ios_ar_holes.png
```

### Risk to ticket 25

Low.
Ticket 25 can push the category screen from `MainScreen` and keep `NearbyPlacesView` as-is; the AR screen takes the shared `NearbyPlacesModel` and does not own navigation.
Two things to preserve: `MainScreen` must keep passing `isActive: page == .camera` so capture/motion stop on the Map page, and `NearbyPlacesModel.currentLocation` must stay readable.
The map page and its `:syncIosMapStyles` pre-build step are untouched.

### Merge-review polish (branch `feature/24b-ar-polish`)

Seven defects from the merge review, all fixed on the Swift side; the shared Kotlin facade and the Android app are untouched.

1. Marker fill default is now `frosted`, matching the product decision and the Android GL renderer.
   `ARHoleFillStyle` and the renderer both default to it, and `-arHoleFill sharp` remains the opt-in for the literal unblurred holes.
   The HUD already printed the active mode, so `ios_ar_frosted_default_hud.png` shows `frosted` with no flag passed.
   Every default-path screenshot was recaptured: `ios_ar_frosted_default.png` is new, and `ios_ar_default.png`, `ios_ar_lod_low.png`, `ios_ar_lod_high.png`, `ios_ar_attitude_left.png`, `ios_ar_attitude_right.png`, `ios_ar_tilted.png`, `ios_ar_flat.png`, `ios_ar_merge.png` and `ios_ar_frosted.png` were regenerated on the new default.
   `ios_ar_holes.png` was regenerated with `-arHoleFill sharp` so the sharp comparison stays documented.

2. Camera-permission denial no longer switches to the synthetic fixture.
   `ARCameraCaptureFailure` (`permissionDenied`, `configurationFailed(String)`) replaces the ad-hoc reason string, and `ARCameraModel.unavailability` is a distinct state from `isSynthetic`.
   On denial the model clears the synthetic texture, keeps `nearby.places` and the real `nearby.currentLocation`, and projects those real places; the page shows "Camera access denied" with "Enable camera access in Settings to see the live AR view." and an `Open Settings` button (`UIApplication.openSettingsURLString`).
   Returning to the foreground re-checks `AVCaptureDevice.authorizationStatus` via `resumeAfterSettings()`, so granting access in Settings recovers without leaving the page.
   The genuine no-camera path (simulator) still selects the synthetic feed and keeps the "Synthetic feed - no camera on this device" badge.

3. `ARCameraCapture.configure()` failures are reported through the same unavailable state with their message instead of returning into a silent black page.
   The capture queue hops back to the main actor to publish it.

4. `-arCamera` is consumed: `MainScreen` seeds its pager selection from `ARCameraLaunchOptions.requestedPage`, so a scripted launch asks for the camera page explicitly.
   The pager's ordinary first page is the camera too, so the flag only changes behaviour if that default changes; it is no longer parsed and ignored.

5. `ARCameraCapture` logs its lifecycle at info/notice level: start request with camera presence and authorization status, permission request and grant/denial, session configuration failure, session starting/running/stopped, and stop request with the running flag.
   No per-frame logging.
   The model logs page start (synthetic vs live) and stop, so the stop-on-map-page behaviour is visible in Console: the UI test run shows `start` on launch and `stop` when Map is tapped.

6. The debug HUD no longer formats placement metrics when placements are cleared.
   `debugLayoutText` returns `vp WxH sN no placements` when the pitch limit (or a missing location) empties the list, and the orientation line prints `-` instead of any non-finite angle.
   `ios_ar_flat_hud.png` records pitch 90 with `no placements` and no `nan`.

7. The `arNoBlurFragment` corner edge mix stays, because it is used by the sharp fill pass (only the stencil-mask pass masks the colour write).
   Android computes the mix into `color` and then returns `texColor`, so its mix is dead; the shader comment now says exactly that and explains why iOS keeps the one-pixel corner softening rather than changing the rendered output.

Also fixed in passing: the `var encoder` that was never mutated in `draw(in:)` is now a `let`, clearing the only warning in the AR sources.

Verification on this branch:

- `xcodegen generate`: no diff.
- `xcodebuild ... build`: `** BUILD SUCCEEDED **`, no warnings from the AR sources.
- `xcodebuild ... test`: `MapPageUITests` passed, 1 test, 0 failures.
- `./gradlew :app:assembleDebug test --console=plain`: `BUILD SUCCESSFUL`, 283 tasks executed (the `:app` module has no unit-test sources).
- `./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain`: `BUILD SUCCESSFUL`, 27 shared tests, 0 failures.

Denial-path evidence, and its limits, on the simulator:

- `xcrun simctl privacy <udid> revoke camera com.trm.sightline` was applied before the denial screenshot; iOS reports `denied` afterwards.
- A simulator has no camera device, so `ARCameraCapture.isCameraAvailable` is false and the model takes the synthetic branch before authorization is ever consulted.
  The OS therefore cannot reach the real denial branch there.
- `ios_ar_permission_denied.png` uses `-arUnavailable permission`, which calls the same `handle(.permissionDenied)` the capture callback calls; the screenshot shows the panel with the Settings button and real OSM places (the network fetch returned Skarbonka, Akcesja, Walenty, and others at the simulated Krakow location), not the Cloth Hall / St Mary's fixture over black.
- `ios_ar_configuration_failed.png` does the same for `configure()`.
- The live `AVCaptureSession` logs and the real permission prompt remain device-only, as listed above.

What was deliberately left alone: `NearbyPlacesView`, the map feature, the style sync pre-build step, `project.yml`, and the shared Kotlin AR facade.
The ticket stays `ready-for-human` because the device-only items are still open.

### Review fix: iOS marker layout comes from the shared slot assignment

The iOS overlay no longer re-derives marker positions in Swift.
`ARCameraModel.recompute` calls `MarkerSlotAssignment.assign` from `core/ar` commonMain with the same geometry Android's `ARMarkerRenderer` passes: marker width, marker height, a half-a-width overlap guard (`MARKER_OVERLAP_GUARD_FRACTION`), the vertical row gap, the row count for the current viewport, and the base row at the top padding plus half a marker.
Markers whose bearings fall inside the shared angular guard now stack into rows instead of overlapping, and the rows and pages come from one implementation for both platforms.

What stays in Swift, and why:

- marker width (`min(viewport.width * 0.42, 240)`, floor 150) and height (88) are the iOS pixel constants the overlay and the Metal stencil holes already use;
- only `page == 0` slots are drawn, because the iOS page has no marker pager to reach later pages, while Android still pages through them;
- the slot layout is cached and recomputed only when the viewport, the place set, or the device position changes by more than 10 m, matching Android's `LOCATION_REASSIGN_THRESHOLD_METERS`; the per-frame projection still supplies each marker's x;
- the old `maxMarkers = 8` cap is gone: the shared row count now bounds what the first page can hold, so a second cap would be redundant.

Two Swift defects fixed while wiring it:

- the pitch limit compared a constant named radians in degree space (`pitchLimitRadians * 180 / .pi`); it is now `pitchLimitDegrees = 45` and the numeric threshold is unchanged;
- `ARCameraCaptureFailure` duplicated `ARCameraUnavailability` and `handle` was a pure identity mapping; there is now one `ARCameraUnavailability`, declared next to the capture that produces it.

Proof: `screenshots/ios_ar_slot_stacking.png`, captured with `-arSynthetic -arAttitude uprightNorth` where the two fixture markers are 24 degrees apart, inside the shared angular guard.
The debug HUD in the same pose reports `vp 402x585 s3  m0 96,60  m1 306,164  markers 2`, which are the first two shared slots (`baseY` then `baseY + markerHeight + verticalSpacing`).
