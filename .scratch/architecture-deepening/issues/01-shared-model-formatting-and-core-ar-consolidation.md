# 01: Shared Place domain properties, distance formatting, and :core:ar module consolidation

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Enrich `Place` in `:core:model` with `location: GeoLocation` and `initials: String`, expose shared distance formatting across Android and iOS, and move the cross-platform AR modules (`ArBlurAnimator`, `ArOrientationTracker`, `ArProjection.markerRect`, and a shared `AngleLowPassFilter` deduplicating Android's `OrientationManager` low-pass filter) from `:shared` into `:core:ar/src/commonMain` with their unit tests in `:core:ar/src/commonTest`.

**Blocked by:** None

**Status:** resolved

## Acceptance Criteria

- [x] **`Place` domain properties in `:core:model`**:
  - `Place` (`:core:model`) defines `val location: GeoLocation` and `val initials: String` (joining the uppercase first characters of up to the first two non-blank whitespace-delimited words).
  - Remove `core/common/.../util/PlaceExtensions.kt` (updating Android imports to `place.location`), remove the private `String.placeInitials()` in Android's `PlaceCategoryScreen.kt`, and remove the duplicate `Place.geoLocation` and `String.placeInitials` extensions in iOS's `CategoryRoute.swift` and manual `GeoLocation(latitude: place.latitude, longitude: place.longitude)` construction in `ARCameraModel.swift`.
  - Add unit tests for `Place.location` and `Place.initials` in `:core:model/src/commonTest`.
- [x] **Unified distance formatting across Android and iOS**:
  - Expose a top-level or object-scoped distance formatter (`formatDistance(meters: Float): String` Alongside `Float.formattedDistance(): String`) in `:core:common` (which is exported by `:shared` to Swift) and `val PlaceSearchRadius.formattedLabel: String` (using the same meter/kilometer formatting).
  - Replace the duplicate distance formatting implementations in Android's `ARMarkerRenderer.kt` (`ARMarker.formattedDistance()`) and iOS's `DistanceFormat.swift`, `ARCameraOverlay.swift` (`distanceText`), and `MainScreen.swift` (`PlaceSearchRadius.title`) with the shared formatter, and delete `iosApp/Sources/DistanceFormat.swift`.
- [x] **Relocate cross-platform AR modules from `:shared` into `:core:ar/src/commonMain`**:
  - Move `ArBlurAnimator` (and `ArBlurFrame`) into `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/blur/`.
  - Extract `AngleLowPassFilter` into `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/orientation/` encapsulating the circular low-pass smoothing (`CIRCLE = 2 * PI`, `SMOOTH_THRESHOLD = CIRCLE / 20f`, `SMOOTH_FACTOR = 0.12f`).
  - Move `ArOrientationTracker` into `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/orientation/` and refactor both `ArOrientationTracker` (`commonMain`) and `OrientationManager` (`androidMain`) to use `AngleLowPassFilter`.
  - Move `ArProjection` (and `markerRect(marker, width, height, cornerRadius)`) into `:core:ar/src/commonMain/kotlin/com/trm/sightline/core/ar/math/` (and use `markerRect` in Android's `ARMarkerRenderer.kt` for `ARMarker.rectF`).
  - Move `ArBlurAnimatorTest`, `ArOrientationTrackerTest`, and `ArProjectionTest` from `:shared/src/commonTest` into `:core:ar/src/commonTest`, and add unit tests for `AngleLowPassFilter`.
  - Since `:shared` already declares `api(project(":core:ar"))` and `export(project(":core:ar"))`, verify `iosApp` continues to compile against `ArBlurAnimator`, `ArOrientationTracker`, and `ArProjection` exported from `Shared.framework`.

## Verification Commands

```sh
./gradlew :core:model:allTests :core:common:allTests :core:ar:allTests :shared:iosSimulatorArm64Test :shared:assembleSharedXCFramework --console=plain
./gradlew :app:assembleDebug test --console=plain
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Answer

Implemented on `ticket/01-model-formatting-ar` as commit `7200583` and merged into `integration/architecture-deepening` as merge commit `97d7b35`.

- `Place` in `:core:model` now exposes `val location: GeoLocation` derived from its coordinates and `val initials: String`, which joins the uppercase first characters of up to the first two non-blank whitespace-delimited words and falls back to `"?"` when the name has none.
- `GeoLocation` gained `withAltitude(altitude)`, so `ARCameraModel.swift` places a marker with `place.location.withAltitude(altitude: ...)` instead of rebuilding a whole `GeoLocation`.
- `core/common/.../util/PlaceExtensions.kt` is deleted; Android call sites (`MainViewModel`, `ARMarker`, `PlaceCategoryScreen`) read `place.location`, and the private `String.placeInitials()` in `PlaceCategoryScreen.kt` plus the `Place.geoLocation` and `String.placeInitials` extensions in `CategoryRoute.swift` are gone.
- `:core:common` now owns distance formatting through `formatDistance(meters)`, `Float.formattedDistance()`, and `PlaceSearchRadius.formattedLabel`, replacing the duplicates in Android's `ARMarkerRenderer.kt`, iOS's `DistanceFormat.swift`, `ARCameraOverlay.swift`, `MainScreen.swift`, and both radius menus.
- AR marker cards keep their one-decimal kilometre rendering through a separate `formatMarkerDistance(meters)` that always emits a decimal (`1.0 km`), while category rows, list subtitles, and radius labels keep the trimmed form (`1 km`).
- `iosApp/Sources/DistanceFormat.swift` is deleted and `iosApp/iosApp.xcodeproj/project.pbxproj` is regenerated without its file, build, and source-phase references.
- The cross-platform AR helpers now live in `:core:ar/src/commonMain` with tests in `:core:ar/src/commonTest`: `blur/ArBlurAnimator` with `ArBlurFrame`, `math/ArProjection` plus the new shared `math/markerRect(centerX, centerY, widthPx, heightPx, cornerRadiusPx)`, `orientation/ArOrientationTracker`, and the new `orientation/AngleLowPassFilter`, which holds `CIRCLE`, `SMOOTH_THRESHOLD`, and `SMOOTH_FACTOR` and now backs both `ArOrientationTracker` and Android's `OrientationManager`, replacing its private `lowPass`.
- Android's `ARMarkerRenderer.rectF` builds its bounds through `markerRect`, and `:shared` still declares `api(project(":core:ar"))` and `export(project(":core:ar"))`, so SKIE exposes `ArProjection` and the new top-level distance functions to Swift.

Verification: `:core:model:allTests` runs 15 tests per platform with `PlaceTest` at 5, `:core:ar:allTests` runs 61 per platform with `AngleLowPassFilterTest` at 6, `ArBlurAnimatorTest` at 8, `ArProjectionTest` at 5, and `ArOrientationTrackerTest` at 11, and `:core:common:allTests` runs 10 per platform with `DistanceFormattingTest` at 5, all with zero failures.
On the merge commit `./gradlew :core:model:allTests :core:ar:allTests :app:assembleDebug --console=plain` reports `BUILD SUCCESSFUL`, and `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build` reports `BUILD SUCCEEDED`, confirming `iosApp` compiles against the exported AR types.
