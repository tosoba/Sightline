# 04: Deepen Marker Slot session and paging into :core:ar, decompose ARCameraModel, and structure iosApp/Sources

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Introduce a deep Marker Slot layout, 10-meter Active Location caching, Screen Rotation page-following, and 3-page picker window module in `:core:ar/src/commonMain` used by both Android and iOS; decompose the 813-line `ARCameraModel.swift` by extracting simulator test launch flags/fixtures and window scene Screen Rotation observation; consolidate the 8 `.onChange` modifiers in `ARCameraScreen.swift`; and organize the flat `iosApp/Sources/` directory into `App/`, `Places/`, `Map/`, and `ARCamera/` (`Capture/`, `Rendering/`, `Testing/`) subdirectories.

**Blocked by:** 01, 02, 03

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **Shared Marker Slot layout, caching, and paging in `:core:ar/src/commonMain`**:
  - Create a `layout` (or `paging`) package in `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/` containing:
    - Adaptive AR Marker card geometry calculation (`MarkerLayoutConfig` / `MarkerLayoutGeometry`: computing `rows` in `1..6`, `markerWidth`, `markerHeight`, `overlapGuard = markerWidth * 0.5f`, and `baseY` from usable viewport dimensions and platform bounds).
    - Stateful `MarkerSlotSession` encapsulating the `LOCATION_REASSIGN_THRESHOLD_METERS = 10f` Active Location cache check, slot invalidation on Place ID list / Screen Rotation / viewport geometry changes, and Screen Rotation page-following (tracking previously drawn Place IDs on `currentPage` and following the page with the highest match count or clamping to `0..maxPage`).
    - 3-page sliding picker window calculation (`MarkerPageWindow.compute(currentPage: Int, totalPages: Int, maxVisiblePages: Int = 3): IntRange` or `List<Int>`).
  - Add comprehensive unit tests in `:core:ar/src/commonTest` covering adaptive layout sizing, 10-meter cache reuse vs invalidation, Screen Rotation page-following, and 3-page sliding windows.
  - Refactor Android's `ARMarkerRenderer.kt` and `CameraPreviewPagePicker.kt` to use the shared `:core:ar` layout, session, and page-window modules.
  - Refactor iOS's `ARCameraModel.swift` and `ARMarkerPagePicker` (`ARCameraScreen.swift`) to use the shared `:core:ar` layout, session, and page-window modules.
- [ ] **Decompose `ARCameraModel.swift` and simplify `ARCameraScreen.swift`**:
  - Extract `ARCameraLaunchOptions`, `ARSimulatedUnavailability`, and the synthetic Krakow `Place` / altitude / location fixtures out of `ARCameraModel.swift` into `iosApp/Sources/ARCamera/Testing/ARCameraLaunchOptions.swift`.
  - Extract the `UIWindowScene` / `NotificationCenter` orientation observer and `UIInterfaceOrientation -> (ScreenRotation, videoRotationAngle)` mapping out of `ARCameraModel.swift` into `iosApp/Sources/ARCamera/Capture/ARWindowOrientationObserver.swift`.
  - Collapse the multi-modifier `.onChange` synchronization chain in `ARCameraScreen.swift` into a single cohesive input sync step.
- [ ] **Structured folder hierarchy for `iosApp/Sources/`**:
  - Organize `iosApp/Sources/` into:
    - `App/`: `SightlineApp.swift`, `MainScreen.swift`, `AppSettings.swift`
    - `Places/`: `NearbyPlacesModel.swift`, `PlacesSheet.swift`, `CategoryPlacesScreen.swift` (renamed from `CategoryRoute.swift`), `PlaceCategory+Display.swift` (renamed from `PlaceCategoryTitle.swift`)
    - `Map/`: `MapScreen.swift`, `MapLibreMapView.swift`
    - `ARCamera/`: `ARCameraScreen.swift`, `ARCameraModel.swift`, `ARCameraOverlay.swift`
    - `ARCamera/Capture/`: `ARCameraCapture.swift`, `ARCameraMotion.swift`, `ARWindowOrientationObserver.swift`
    - `ARCamera/Rendering/`: `ARCameraRenderer.swift`, `ARCameraShaders.metal`
    - `ARCamera/Testing/`: `ARCameraLaunchOptions.swift`, `ARCameraSyntheticFeed.swift`
  - Run `xcodegen generate` in `iosApp/` and verify all Xcode groups and targets build and pass `iosAppUITests`.

## Verification Commands

```sh
./gradlew :core:ar:allTests :shared:iosSimulatorArm64Test :shared:assembleSharedXCFramework --console=plain
./gradlew :app:assembleDebug test --console=plain
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
```
