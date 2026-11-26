# 04: Deepen Marker Slot session and paging into :core:ar, decompose ARCameraModel, and structure iosApp/Sources

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Introduce a deep Marker Slot layout, 10-meter Active Location caching, Screen Rotation page-following, and 3-page picker window module in `:core:ar/src/commonMain` used by both Android and iOS; decompose the 813-line `ARCameraModel.swift` by extracting simulator test launch flags/fixtures and window scene Screen Rotation observation; consolidate the 8 `.onChange` modifiers in `ARCameraScreen.swift`; and organize the flat `iosApp/Sources/` directory into `App/`, `Places/`, `Map/`, and `ARCamera/` (`Capture/`, `Rendering/`, `Testing/`) subdirectories.

**Blocked by:** 01, 02, 03

**Status:** resolved

## Acceptance Criteria

- [x] **Shared Marker Slot layout, caching, and paging in `:core:ar/src/commonMain`**:
  - Create a `layout` (or `paging`) package in `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/` containing:
    - Adaptive AR Marker card geometry calculation (`MarkerLayoutConfig` / `MarkerLayoutGeometry`: computing `rows` in `1..6`, `markerWidth`, `markerHeight`, `overlapGuard = markerWidth * 0.5f`, and `baseY` from usable viewport dimensions and platform bounds).
    - Stateful `MarkerSlotSession` encapsulating the `LOCATION_REASSIGN_THRESHOLD_METERS = 10f` Active Location cache check, slot invalidation on Place ID list / Screen Rotation / viewport geometry changes, and Screen Rotation page-following (tracking previously drawn Place IDs on `currentPage` and following the page with the highest match count or clamping to `0..maxPage`).
    - 3-page sliding picker window calculation (`MarkerPageWindow.compute(currentPage: Int, totalPages: Int, maxVisiblePages: Int = 3): IntRange` or `List<Int>`).
  - Add comprehensive unit tests in `:core:ar/src/commonTest` covering adaptive layout sizing, 10-meter cache reuse vs invalidation, Screen Rotation page-following, and 3-page sliding windows.
  - Refactor Android's `ARMarkerRenderer.kt` and `CameraPreviewPagePicker.kt` to use the shared `:core:ar` layout, session, and page-window modules.
  - Refactor iOS's `ARCameraModel.swift` and `ARMarkerPagePicker` (`ARCameraScreen.swift`) to use the shared `:core:ar` layout, session, and page-window modules.
- [x] **Decompose `ARCameraModel.swift` and simplify `ARCameraScreen.swift`**:
  - Extract `ARCameraLaunchOptions`, `ARSimulatedUnavailability`, and the synthetic Krakow `Place` / altitude / location fixtures out of `ARCameraModel.swift` into `iosApp/Sources/ARCamera/Testing/ARCameraLaunchOptions.swift`.
  - Extract the `UIWindowScene` / `NotificationCenter` orientation observer and `UIInterfaceOrientation -> (ScreenRotation, videoRotationAngle)` mapping out of `ARCameraModel.swift` into `iosApp/Sources/ARCamera/Capture/ARWindowOrientationObserver.swift`.
  - Collapse the multi-modifier `.onChange` synchronization chain in `ARCameraScreen.swift` into a single cohesive input sync step.
- [x] **Structured folder hierarchy for `iosApp/Sources/`**:
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

## Answer

Implemented on `ticket/04-marker-slot-ios-structure` as commits `f7351cf`, `b94ad03`, `5116cf0`, and `98116b5`, and merged into `integration/architecture-deepening` as merge commit `21d098f`.

- `:core:ar` gains a `layout` package in `commonMain` where `MarkerLayoutConfig` carries every platform-specific bound, `MarkerLayoutGeometry` carries the resolved card geometry, and `MarkerLayoutCalculator` computes `divisor`, `markerWidth`, `rows`, `markerHeight`, `baseY`, and `overlapGuard = markerWidth * 0.5f` from the usable viewport.
- `MarkerSlotSession` owns the slot cache and page state, reusing slots while the Place ID list, layout geometry, Screen Rotation, and canvas size are unchanged and the Active Location moved at most 10 meters (`LOCATION_REASSIGN_THRESHOLD_METERS = 10f`).
- Every reassignment follows the page holding the most previously visible Place IDs with ties to the lowest page, keeps the current page when no previously visible Place survived, and clamps `currentPage` into `0..totalPages - 1`; `selectPage` clamps explicit pager intents the same way.
- `MarkerPageWindow.compute(currentPage, totalPages, maxVisiblePages = 3)` returns at most three page indices centered on the current page and clamped to the start or end of the pager.
- The canonical sizing formula is rows-first: `divisor` comes from the target width, `markerWidth` is clamped by the platform width bounds, `rows = clamp(usableHeight / (minMarkerHeight + verticalSpacing), minRows, maxRows)`, and `markerHeight = clamp(usableHeight / rows - verticalSpacing, minMarkerHeight, maxMarkerHeight)`.
- Android produces the same geometry as before at normal sizes, and its only delta is the pathological tiny-viewport case where `maxMarkerHeight` now takes `maxOf(cameraViewHeightPx, minMarkerHeightPx)` so the config stays valid and cards clamp to the minimum height instead of undershooting.
- iOS accepts one deviation: a short landscape viewport on iPhone 17 resolves to 79.25 pt cards instead of the old fixed 72 pt, still inside the 72...88 pt bounds, while portrait and iPad geometry stays at 88 pt.
- Rows-first was chosen because it reproduces Android's text-metric-driven card height exactly, so the shared calculator preserves Android's existing rendering while only moving iOS in the one viewport where six minimum rows do not fit.
- Android's `ARMarkerRenderer` now delegates geometry to `MarkerLayoutCalculator` and slot/page state to `MarkerSlotSession`, and `CameraPreviewPagePicker` reads its buttons from `MarkerPageWindow`.
- Android follows the page on every reassignment rather than only after the renderer's first frame, and a marker-set change clears the previously visible ID set and still resets to page 0.
- iOS's `ARCameraModel` shrinks from 809 to 530 lines: it drives the same `MarkerSlotSession`, builds `MarkerLayoutGeometry` through `MarkerLayoutCalculator` with its 72...88 pt config, and reads the pager window from `MarkerPageWindow` in `ARCameraScreen`.
- `ARWindowOrientationObserver` owns the `UIWindowScene` and `NotificationCenter` observation, the `UIInterfaceOrientation` to `ScreenRotation` mapping, and the AVFoundation `videoRotationAngle`, so `ARCameraModel` only consumes rotation changes.
- `ARCameraLaunchOptions.swift` now holds `ARCameraLaunchOptions`, `ARSimulatedUnavailability`, and the synthetic Krakow camera location, Places, and altitude offsets under `ARCamera/Testing/`.
- `ARCameraScreen` collapses the eight `.onChange` modifiers into one `ARCameraInput` snapshot compared in a single `.onChange`, whose synchronizer dispatches only the side effects whose fields changed.
- `iosApp/Sources` now contains only feature folders: `App/`, `Places/`, `Map/`, and `ARCamera/` with `Capture/`, `Rendering/`, and `Testing/`; `CategoryRoute.swift` and `PlaceCategoryTitle.swift` became `Places/CategoryPlacesScreen.swift` and `Places/PlaceCategory+Display.swift`, and `project.pbxproj` was regenerated to match.
- Verification: `:core:ar:allTests` runs 86 tests per target, up from 61 with 4 layout calculator, 7 page window, and 14 slot session tests added, and `:shared:iosSimulatorArm64Test` runs 6 tests, all with zero failures.
- Android's `:app:assembleDebug` and Gradle `test` tasks pass, `xcodegen generate` reproduces the committed `project.pbxproj`, and `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build` reports `BUILD SUCCEEDED`.
- The UI suite reports 7 executed / 1 skipped / 0 failed on iPhone 17 and 7 executed / 0 skipped / 0 failed on iPad Pro 13-inch (M5), where the skipped test is the live location scenario that requires a simulator location scenario to be installed.
