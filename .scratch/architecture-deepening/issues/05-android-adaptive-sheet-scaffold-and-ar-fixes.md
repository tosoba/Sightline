# 05: Clean up Android :core:ar dependency leaks, extract AdaptiveSheetScaffold in :core:ui, and fix Android AR rendering bugs

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move sheet layout constants out of `:core:ar` into `:core:ui`, remove the `:core:ar` dependency from `:feature:map`, `:feature:places`, and `:feature:place-category`, extract a shared `AdaptiveSheetScaffold` composable into `:core:ui` to deduplicate bottom-sheet/side-sheet layout and inset math between `MainScreen` and `PlaceCategoryScreen`, collapse the shallow `CameraScreen` pass-through, and fix the `contrastingColor` float-to-int truncation bug and cross-thread rectangle updates in Android's AR camera renderer.

**Blocked by:** 02, 04

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **Eliminate `:core:ar` dependency leakage into non-AR feature modules**:
  - Move `sideSheetWidthDp`, `collapsedBottomSheetContentHeightDp`, and `collapsedBottomSheetDragHandleHeightDp` from `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/util/ARContextExtensions.kt` into `:core:ui`.
  - Remove `implementation(project(":core:ar"))` from `feature/map/build.gradle.kts`, `feature/places/build.gradle.kts`, and `feature/place-category/build.gradle.kts`.
- [ ] **Extract `AdaptiveSheetScaffold` into `:core:ui`**:
  - Create a reusable `AdaptiveSheetScaffold` composable in `:core:ui` that encapsulates:
    - `rememberBottomSheetScaffoldStateForScreenHeight(isCompactHeight)`
    - `rememberBottomSheetExpandedProgress(sheetState)`
    - Safe-drawing top inset drag handle padding scaled by `expandedProgress`
    - `sheetNonPeekHeight` measurement
    - Automatic switching between `BottomSheetScaffold` (when `!isCompactHeight`) and a right-aligned `Surface` side sheet of width `sideSheetWidthDp.dp` (when `isCompactHeight`), exposing current sheet bounds / expansion progress to the caller for AR camera blur.
  - Refactor `app/src/main/java/com/trm/sightline/ui/MainScreen.kt` and `feature/place-category/src/main/java/com/trm/sightline/feature/category/PlaceCategoryScreen.kt` to use `AdaptiveSheetScaffold`.
- [ ] **Collapse shallow `CameraScreen` pass-through & dead location methods**:
  - Simplify `feature/camera/.../CameraScreen.kt` (or fold its permission branch cleanly) so 10 parameters are not prop-drilled unchanged from `MainPager` through `CameraScreen` into `CameraPreview`.
  - Clean up unused `LocationProvider.checkLocationSettings()` / `AndroidLocationProvider.authorizationState` or align Android location settings checks with `:core:common`.
- [ ] **Fix Android AR contrasting-color truncation and thread-safety bugs**:
  - In `feature/camera/src/main/java/com/trm/sightline/feature/camera/CameraPreview.kt`, fix `openGLRenderer.setContrastingColor(...)` so Compose `Color.red`, `Color.green`, and `Color.blue` (`0f..1f` floats) are not truncated to `0` by `.toInt()` before dividing by `256f` in `OpenGLRenderer.kt` (pass `0f..1f` floats directly or scale by `255f`).
  - In `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/camera/OpenGLRenderer.kt`, mark `markerRectFs` and `otherRectFs` `@Volatile` and assign immutable list snapshots so cross-thread reads on the `GLRenderer` `HandlerThread` are thread-safe.
  - In `core/ar/src/androidMain/kotlin/com/trm/sightline/core/ar/util/ARContextExtensions.kt`, guard `Context.rootWindowInsets` when `window.decorView.rootWindowInsets` is `null` (falling back to `WindowInsetsCompat.CONSUMED`).

## Verification Commands

```sh
./gradlew test assembleDebug --console=plain
```
