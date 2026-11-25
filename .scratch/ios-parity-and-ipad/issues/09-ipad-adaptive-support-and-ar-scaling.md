# 09: Native iPad adaptive support and 4-orientation AR scaling

**Spec:** `.scratch/ios-parity-and-ipad/spec.md` (Phase 3), `.scratch/ios-parity-and-ipad/issues/04-ipad-adaptive-layout-and-ar-scaling.md`

**What to build:** Enable universal iPhone/iPad support (`TARGETED_DEVICE_FAMILY: "1,2"`), configure all 4 iPad interface orientations (`UISupportedInterfaceOrientations~ipad`) with full multitasking support (no `UIRequiresFullScreen`), adapt the unified Places & Location `.sheet` across `.compact` and `.regular` horizontal size classes (adding a `"Done"` toolbar button and camera blur in `.regular` width), and scale AR Marker card widths (`divisor` in `2...6`, width clamped to `150...220 pt`) for large iPad viewports.

**Blocked by:** 06, 08

**Status:** resolved

## Acceptance Criteria

- [ ] **Universal target, 4 orientations, and iPadOS multitasking**:
  - `iosApp/project.yml` sets `TARGETED_DEVICE_FAMILY: "1,2"`, and `xcodegen generate` updates `iosApp/iosApp.xcodeproj` cleanly.
  - `iosApp/Support/Info.plist` keeps the 3 iPhone orientations in `UISupportedInterfaceOrientations` and adds `UISupportedInterfaceOrientations~ipad` with all 4 orientations (`UIInterfaceOrientationPortrait`, `UIInterfaceOrientationPortraitUpsideDown`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`), without `UIRequiresFullScreen`.
  - `ARCameraModel` and `ARCameraCapture` handle all 4 orientations (`.rotation0`, `.rotation90`, `.rotation180`, `.rotation270` -> `videoRotationAngle` `90°`, `0°`, `270°`, `180°`) and recompute Marker Slots on live window resize (Split View / Stage Manager).
- [ ] **Adaptive `.sheet` across `.compact` and `.regular` horizontal size classes**:
  - In `.compact` width (`horizontalSizeClass == .compact`), the Places & Location `.sheet` presents with `.presentationDetents([.medium, .large], selection: $sheetDetent)`, `.presentationDragIndicator(.visible)`, and `.presentationBackgroundInteraction(.enabled(upThrough: .medium))`.
  - In `.regular` width (`horizontalSizeClass == .regular`), the `.sheet` presents as a centered iPadOS page sheet and shows a top-trailing **"Done"** toolbar button on the root view of its `NavigationStack`.
  - `ARCameraModel.sheetObscuresCamera` evaluates to `showsPlacesSheet && (horizontalSizeClass == .regular || sheetDetent == .large)`, blurring the background AR camera feed whenever the centered iPad page sheet or `.large` compact sheet is open.
- [ ] **iPad AR Marker width scaling**:
  - `ARCameraModel.MarkerSlotGeometry` uses `divisor = Int(usableWidth / 160).clamped(to: 2...6)` and clamps `markerWidth = (usableWidth / CGFloat(divisor)).clamped(to: 150...220)` while keeping `markerHeight` in `72...88 pt` and `rows` in `1...6`.
- [ ] **Verification on iPhone and iPad simulators**:
  - `iosApp` builds and passes `iosAppUITests` on iPhone simulator and builds/runs cleanly on an iPad simulator in both portrait and landscape; Android stays green.

## Verification Commands

```sh
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
./gradlew :app:assembleDebug test --console=plain
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M5)' build
```

## Answer

Implemented on `ios-parity/09-ipad` as commits `bf56d09` and `d6b389c`, merged to `integration/ios-parity-and-ipad` as `61341a8`.

- `iosApp/project.yml` targets `TARGETED_DEVICE_FAMILY: "1,2"`; the regenerated project is committed and `xcodegen generate` is idempotent.
- `Info.plist` keeps the three iPhone orientations and adds `UISupportedInterfaceOrientations~ipad` with all four; no `UIRequiresFullScreen`, and the built app reports `UIDeviceFamily = [1, 2]`.
- AR marker geometry now caps the divisor at 6, so wide usable widths divide into up to six columns while height stays `72...88 pt`, rows `1...6`, and width clamps to `150...220 pt`.
  The debug HUD prints the width and divisor (`mw 172 d6`) and the iPad test asserts a width in `150...220 pt` and divisor 6 in portrait and landscape.
- Orientation reads the non-deprecated `scene.effectiveGeometry.interfaceOrientation`.
  The four-way `ScreenRotation` and `videoRotationAngle` mapping and cache invalidation were already in place; the iPad test now exercises all four orientations and live viewport changes.
- The existing single `.sheet` works as a centered iPad page sheet with the Done button and `sheetObscuresCamera` blurring the camera; no product fix was needed.
  `sheetObscuresCamera` uses `showsPlacesSheet && (horizontalSizeClass == .regular || verticalSizeClass == .compact || sheetDetent == .large)`, so iPhone landscape's full-screen cover also blurs.
  The Done button, `.medium`/`.large` detents, and drag indicator are applied once for both size classes; iPadOS ignores the detents in a regular-width page sheet, while Done remains the explicit one-tap and pointer dismissal.

Verification: all 55 shared iOS tests and the Android gate pass; iPhone 17 / iOS 26.5 runs 7 tests with 1 skip (the iPad-only test) and 0 failures; iPad Pro 13-inch (M5) / iOS 26.5 runs 7 of 7 with 0 failures, covering page-sheet geometry, Done dismissal, camera blur behind the sheet, marker geometry, and `r0`/`r90`/`r180`/`r270` rotations with viewport-aspect assertions.
Removing the `~ipad` orientation key makes the upside-down assertion fail, so the test is not vacuous.
Portrait and landscape iPad screenshots were captured during verification.

Limitations: Split View and Stage Manager cannot be driven by `simctl` and this Xcode install has no `Simulator.app`, so resize was verified through the same viewport-change path during rotation rather than by dragging.
A reused iPad simulator can remember an iPhone-compat window frame (erase it before a run).
Pre-existing ticket 06 behavior: a non-full-screen portrait iPad window that rotates reports landscape orientation while keeping its window aspect, so the AR projection rotates relative to the window; this is outside this ticket's acceptance criteria.
