# 06: iOS AR camera blur, landscape orientation, and slot pagination

**Spec:** `.scratch/ios-parity-and-ipad/spec.md` (Phase 1), `.scratch/ios-parity-and-ipad/issues/01-ar-camera-blur-and-landscape-orientation.md`

**What to build:** Fix the Issue 24 AR camera gaps on iOS so the live camera feed stays sharp during normal viewing with frosted max-LOD stencil blur inside AR Marker cards, full-frame blur activates only when device pitch exceeds ±45° or an obscuring sheet covers the camera (or when forced by debug launch flags), iPhone landscape orientations (`LandscapeLeft`, `LandscapeRight`) align camera frames and projected AR Markers via `ScreenRotation` and `AVCaptureConnection.videoRotationAngle`, and overflowing Marker Slots can be paged with a floating page picker and tap-to-toggle overlay visibility.

**Blocked by:** None

**Status:** resolved

## Acceptance Criteria

- [ ] **Sharp live feed with frosted AR Marker cards by default**:
  - `ARCameraModel.blurEnabled` defaults to `false` during normal viewing (unless `-arForceBlur` or `-arBlur <0..1>` is passed).
  - When `blurEnabled == false` and `!frame.isAnimatingLod`, `ARCameraRenderer` renders the base camera pass sharp (`arNoBlurFragment`), writes stencil masks (`stencil = 1`) for each AR Marker in `holeRects` via `udRoundBox`, and runs the 8-pass Gaussian pyramid at max LOD (`lod = 2.0`, `contrastingColorMix = 0.025`) to fill the marker cards with frosted blur (`holeFillStyle = .frosted`).
  - During the 18-frame unblur transition (`!blurEnabled && frame.isAnimatingLod`), visible marker holes continue to render frosted so cards never flash transparent mid-ramp.
  - `contrastingColor` is derived dynamically from `UIColor.secondarySystemBackground` resolved for the active SwiftUI `ColorScheme` and eased over 60 frames via `ArBlurAnimator`.
- [ ] **Conditional full-frame blur & marker hiding**:
  - `blurEnabled` becomes `true` (animating over 18 frames via `ArBlurAnimator`) only when `pitchOutOfRange` (pitch outside `±45°`), `sheetObscuresCamera == true`, or forced by `-arForceBlur` / `-arBlur <0..1>`.
  - Whenever `pitchOutOfRange || sheetObscuresCamera`, `ARCameraModel` clears `placements` and `holeRects` so AR Markers are hidden and `ARCameraRenderer` skips the second (hole-fill) pass.
- [ ] **Landscape orientation & Screen Rotation alignment**:
  - `iosApp/Support/Info.plist` unlocks `UIInterfaceOrientationPortrait`, `UIInterfaceOrientationLandscapeLeft`, and `UIInterfaceOrientationLandscapeRight` on iPhone.
  - `ARCameraModel` observes the active window scene's `UIInterfaceOrientation` and maps it to both `ScreenRotation` (for `ArProjection.updateCamera`) and `AVCaptureConnection.videoRotationAngle`:
    - `.portrait` -> `ScreenRotation.rotation0`, `videoRotationAngle = 90`
    - `.landscapeRight` -> `ScreenRotation.rotation90`, `videoRotationAngle = 0`
    - `.landscapeLeft` -> `ScreenRotation.rotation270`, `videoRotationAngle = 180`
    - `.portraitUpsideDown` -> `ScreenRotation.rotation180`, `videoRotationAngle = 270`
  - `ARCameraMotion` offsets scripted simulator pose roll by the active `ScreenRotation` (`0` for `.rotation0`, `-π/2` for `.rotation90`, `+π/2` for `.rotation270`, `π` for `.rotation180`) so simulator landscape verification exercises the exact same projection path as physical hardware.
  - `ARCameraRenderer.updateTexTransform` applies the 90° UV rotation fallback only when source pixel buffer and drawable aspect orientations disagree (`(sourceWidth > sourceHeight) != (drawableWidth > drawableHeight)`).
  - `cachedSlots` invalidation includes `screenRotation` and the usable viewport geometry.
- [ ] **Adaptive marker card sizing, Marker Slot pagination, and tap-to-toggle overlay**:
  - `MarkerSlotGeometry` computes `markerHeight` in `72...88 pt`, `rows` in `1...6`, `divisor = Int(usableWidth / 160).clamped(to: 2...5)`, and `markerWidth = (usableWidth / CGFloat(divisor)).clamped(to: 150...220)` from the usable unobscured camera region.
  - `ARCameraModel` tracks `currentPage: Int` (0-based) and `totalPages: Int` (`maxPage + 1`), preserves the page of previously drawn markers across slot reassignments, clamps `currentPage` to `0...maxPage`, and filters displayed `placements` and `holeRects` to `slot.page == currentPage`.
  - `ARCameraScreen` displays a floating **AR Marker Page Picker** (up to 3 visible 1-based page numbers in a sliding window) when `totalPages > 1`, and toggles `toolbarsVisible` when the user taps empty space on the AR camera preview.
- [ ] **Tests and verification**:
  - `ArProjectionTest` in `shared/commonTest` verifies horizontal and vertical marker coordinates across `.rotation0`, `.rotation90`, `.rotation180`, and `.rotation270`.
  - Simulator builds and runs cleanly in portrait and landscape with `SIMCTL_CHILD_MTL_DEBUG_LAYER=1` and zero Metal validation errors; Android stays green.

## Verification Commands

```sh
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
./gradlew :app:assembleDebug test --console=plain
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Answer

Implemented on `ios-parity/06-ar-camera` as commit `0ecf170` and merged to `integration/ios-parity-and-ipad` as `33c33f4`.

- Normal viewing is sharp (`blurEnabled` defaults false); the renderer draws the sharp base pass, writes rounded stencil masks for AR Markers, and fills them with the max-LOD Gaussian chain (`lod = 2.0`, `contrastingColorMix = 0.025`, frosted style).
  Full-frame blur is derived only from pitch outside +/-45 degrees, `sheetObscuresCamera`, or the debug overrides, and markers are cleared whenever the frame blurs for those reasons.
- `contrastingColor` is resolved from `UIColor.secondarySystemBackground` for the active `ColorScheme` and eased by `ArBlurAnimator`.
- The active window scene orientation maps to `ScreenRotation` and `AVCaptureConnection.videoRotationAngle` per the ticket table, iPhone landscape is unlocked, scripted poses are rolled by the active rotation, and slot caches invalidate on rotation and usable geometry.
  The scripted roll offsets live on the shared `ScreenRotation` enum as `rollOffsetRadians`, so `ARCameraMotion` and the four-rotation shared tests use one table.
- Marker cards are sized from the usable camera region, Marker Slots page with a 3-window floating picker, and tapping empty camera space toggles the floating controls.
  The debug HUD always reports the marker width and effective divisor (`mw 172 d6`), even with no placements, so simulator runs can check the adaptive sizing.

Verification: `ArProjectionTest` covers all four rotations (5 tests), and `ArOrientationTrackerTest` pins the four scripted roll offsets.
30 shared iOS tests pass, Android `assembleDebug`/`test` stays green, and the iPhone 17 simulator build succeeds.
A simulator smoke run with `SIMCTL_CHILD_MTL_DEBUG_LAYER=1` produced zero Metal validation errors.

Deviation: this machine has headless simulator runtimes and `simctl` has no rotation command, so the landscape path is covered by the four-rotation projection tests and the orientation mapping rather than a landscape screenshot.
The page picker is hidden when the toolbars are hidden or the frame is fully blurred, matching Android.
`-arBlur 0` freezes sharp, `-arBlur > 0` and `-arForceBlur` force blur, and `-arNoBlur` forces the sharp debug override and wins over every other trigger.
The README documents the new semantics.
