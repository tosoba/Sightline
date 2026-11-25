# 09: Native iPad adaptive support and 4-orientation AR scaling

**Spec:** `.scratch/ios-parity-and-ipad/spec.md` (Phase 3), `.scratch/ios-parity-and-ipad/issues/04-ipad-adaptive-layout-and-ar-scaling.md`

**What to build:** Enable universal iPhone/iPad support (`TARGETED_DEVICE_FAMILY: "1,2"`), configure all 4 iPad interface orientations (`UISupportedInterfaceOrientations~ipad`) with full multitasking support (no `UIRequiresFullScreen`), adapt the unified Places & Location `.sheet` across `.compact` and `.regular` horizontal size classes (adding a `"Done"` toolbar button and camera blur in `.regular` width), and scale AR Marker card widths (`divisor` in `2...6`, width clamped to `150...220 pt`) for large iPad viewports.

**Blocked by:** 06, 08

**Status:** ready-for-agent

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
