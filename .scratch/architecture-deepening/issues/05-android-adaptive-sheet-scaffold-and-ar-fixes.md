# 05: Clean up Android :core:ar dependency leaks, extract AdaptiveSheetScaffold in :core:ui, and fix Android AR rendering bugs

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move sheet layout constants out of `:core:ar` into `:core:ui`, remove the `:core:ar` dependency from `:feature:map`, `:feature:places`, and `:feature:place-category`, extract a shared `AdaptiveSheetScaffold` composable into `:core:ui` to deduplicate bottom-sheet/side-sheet layout and inset math between `MainScreen` and `PlaceCategoryScreen`, collapse the shallow `CameraScreen` pass-through, and fix the `contrastingColor` float-to-int truncation bug and cross-thread rectangle updates in Android's AR camera renderer.

**Blocked by:** 02, 04

**Status:** resolved

## Acceptance Criteria

- [x] **Eliminate `:core:ar` dependency leakage into non-AR feature modules**:
  - Move `sideSheetWidthDp`, `collapsedBottomSheetContentHeightDp`, and `collapsedBottomSheetDragHandleHeightDp` from `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/util/ARContextExtensions.kt` into `:core:ui`.
  - Remove `implementation(project(":core:ar"))` from `feature/map/build.gradle.kts`, `feature/places/build.gradle.kts`, and `feature/place-category/build.gradle.kts`.
- [x] **Extract `AdaptiveSheetScaffold` into `:core:ui`**:
  - Create a reusable `AdaptiveSheetScaffold` composable in `:core:ui` that encapsulates:
    - `rememberBottomSheetScaffoldStateForScreenHeight(isCompactHeight)`
    - `rememberBottomSheetExpandedProgress(sheetState)`
    - Safe-drawing top inset drag handle padding scaled by `expandedProgress`
    - `sheetNonPeekHeight` measurement
    - Automatic switching between `BottomSheetScaffold` (when `!isCompactHeight`) and a right-aligned `Surface` side sheet of width `sideSheetWidthDp.dp` (when `isCompactHeight`), exposing current sheet bounds / expansion progress to the caller for AR camera blur.
  - Refactor `app/src/main/java/com/trm/sightline/ui/MainScreen.kt` and `feature/place-category/src/main/java/com/trm/sightline/feature/category/PlaceCategoryScreen.kt` to use `AdaptiveSheetScaffold`.
- [x] **Collapse shallow `CameraScreen` pass-through & dead location methods**:
  - Simplify `feature/camera/.../CameraScreen.kt` (or fold its permission branch cleanly) so 10 parameters are not prop-drilled unchanged from `MainPager` through `CameraScreen` into `CameraPreview`.
  - Clean up unused `LocationProvider.checkLocationSettings()` / `AndroidLocationProvider.authorizationState` or align Android location settings checks with `:core:common`.
- [x] **Fix Android AR contrasting-color truncation and thread-safety bugs**:
  - In `feature/camera/src/main/java/com/trm/sightline/feature/camera/CameraPreview.kt`, fix `openGLRenderer.setContrastingColor(...)` so Compose `Color.red`, `Color.green`, and `Color.blue` (`0f..1f` floats) are not truncated to `0` by `.toInt()` before dividing by `256f` in `OpenGLRenderer.kt` (pass `0f..1f` floats directly or scale by `255f`).
  - In `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/camera/OpenGLRenderer.kt`, mark `markerRectFs` and `otherRectFs` `@Volatile` and assign immutable list snapshots so cross-thread reads on the `GLRenderer` `HandlerThread` are thread-safe.
  - In `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/util/ARContextExtensions.kt`, guard `Context.rootWindowInsets` when `window.decorView.rootWindowInsets` is `null` (falling back to `WindowInsetsCompat.CONSUMED`).

## Verification Commands

```sh
./gradlew test assembleDebug --console=plain
```

## Answer

Implemented on `ticket/05-android-sheet-and-ar-fixes` as commit `18e2eba` and merged into `integration/architecture-deepening` as merge commit `a111d23`.

- `sideSheetWidthDp`, `collapsedBottomSheetContentHeightDp`, and `collapsedBottomSheetDragHandleHeightDp` moved from `:core:ar`'s `ARContextExtensions.kt` into the new `:core:ui` file `SheetDimensions.kt`.
- `ARContextExtensions.kt` and `ARMarkerRenderer.kt` now import those constants from `com.trm.sightline.core.ui`, so the AR module still renders the same 400 dp side sheet and 186 dp collapsed content without owning the values.
- `:core:ui` gains `AdaptiveSheetScaffold` and a stable `AdaptiveSheetScaffoldState` that exposes `isCompactHeight`, the underlying `BottomSheetScaffoldState` and `SheetState`, `expandedProgress`, `sheetOffset`, `sheetNonPeekHeight`, `contentPadding`, and a `cameraPreviewBlurred` helper for the AR camera blur.
- The scaffold owns the state construction, the safe-drawing drag-handle spacer scaled by `expandedProgress`, the `sheetNonPeekHeight` measurement, the `BottomSheetScaffold` versus right-aligned side `Surface` switch, and the display-cutout and navigation-bar insets for both sheet forms.
- Callers receive the state plus a ready-made sheet modifier and render the sheet body themselves, so the two screens keep their own list and header content.
- `adaptiveSheetContentPadding(isCompactHeight, sideSheetWidth, sheetPeekHeight, safeDrawingBottom)` is a pure function that returns bottom plus end padding in compact height and bottom peek-height padding in portrait.
- `MainScreen` consumes the scaffold with the default `applyContentPadding = true`, which reproduces its previous behavior of wrapping the pager content in the scaffold's top, start, and end padding with the bottom only applied in compact height.
- `MainScreen` passes `sheet.contentPadding` to `MainPager` and reads `sheet.cameraPreviewBlurred`, so the AR blur and toolbar visibility still follow sheet expansion.
- `PlaceCategoryScreen` consumes the scaffold with `applyContentPadding = false`, which preserves its previous behavior of letting the map fill the whole screen while the same computed `contentPadding` still drives the camera update and `animateToPlace` padding.
- `PlaceCategoryScreen` sets `sheetContentHorizontalPadding = 0.dp` and `sheetContentFillModifier = Modifier.fillMaxWidth()` to keep its header and list flush to the sheet edges, matching the pre-refactor layout.
- `implementation(project(":core:ar"))` was removed from `feature/map/build.gradle.kts`, `feature/places/build.gradle.kts`, and `feature/place-category/build.gradle.kts`.
- The dependency configuration check proves the removal: `./gradlew :feature:map:dependencies --configuration debugCompileClasspath`, and the same task for `places` and `place-category`, each reports no `core:ar` entry.
- `CameraScreen.kt` is deleted and its `AnimatedContent` permission branch moved into `CameraPreview`, which now takes `cameraPermissionGranted` and `onGrantPermissionClick` alongside the renamed `enabled` and `blurred` parameters.
- `MainPager` calls `CameraPreview` directly and still receives the `ARMarkerRenderer` through the unchanged `overlayContent` lambda, so the marker overlay and the blurred rectangles are wired as before.
- `openGLRenderer.setContrastingColor` now accepts normalized `0f..1f` floats, so `contrastingColor.red`, `.green`, and `.blue` are forwarded unchanged and `OpenGLRenderer.kt` no longer divides by `256f`.
- The old `.toInt()` truncation collapsed every normalized channel to `0` because Compose colors never reach `1f`, and dividing `0` by `256f` yielded pure black for every sheet container color.
- `markerRectFs` and `otherRectFs` are now backed by private `@Volatile` immutable snapshots, and their setters store `value.toList()` so a caller's mutable list can no longer be mutated underneath the renderer thread.
- `OpenGLRenderer.onDrawFrame` captures one `RectFsSnapshot` before building the coordinate array, which keeps the marker and other rect counts consistent with the coordinates sent to native code on the `GLRenderer` `HandlerThread`.
- `Context.rootWindowInsets` now returns `WindowInsetsCompat.CONSUMED` when `decorView.rootWindowInsets` is null, which avoids the previous crash when insets were read before the decor view attached.
- The location cleanup deliberately kept both `LocationProvider.authorizationState` and `LocationProvider.checkLocationSettings()`: `authorizationState` is consumed by the shared iOS surface and `NearbyPlacesModel.swift` and is implemented by both providers, while `checkLocationSettings()` stays as the platform-neutral contract for the Android check.
- `Context.checkLocationSettings()` returns `DisabledResolvable` carrying an `IntentSenderRequest` that an activity-result API must present, and that Android type cannot cross the shared boundary, so the Android app keeps using the `Context` extension and the provider member keeps the symmetric `LocationSettingsState` form instead of being deleted.
- Verification: `./gradlew test assembleDebug --console=plain` reports `BUILD SUCCESSFUL`.
- Verification: `./gradlew :core:ar:allTests` reports `BUILD SUCCESSFUL` with 86 tests per target and zero failures.
- Verification: the new `:core:ui` `AdaptiveSheetContentPaddingTest` runs 2 tests covering the compact-height and portrait padding branches, both passing.
- The only new test infrastructure is `testImplementation(kotlin("test-junit"))` in `core/ui/build.gradle.kts`.
