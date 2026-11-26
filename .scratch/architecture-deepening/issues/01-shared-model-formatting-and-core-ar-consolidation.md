# 01: Shared Place domain properties, distance formatting, and :core:ar module consolidation

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Enrich `Place` in `:core:model` with `location: GeoLocation` and `initials: String`, expose shared distance formatting across Android and iOS, and move the cross-platform AR modules (`ArBlurAnimator`, `ArOrientationTracker`, `ArProjection.markerRect`, and a shared `AngleLowPassFilter` deduplicating Android's `OrientationManager` low-pass filter) from `:shared` into `:core:ar/src/commonMain` with their unit tests in `:core:ar/src/commonTest`.

**Blocked by:** None

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **`Place` domain properties in `:core:model`**:
  - `Place` (`:core:model`) defines `val location: GeoLocation` and `val initials: String` (joining the uppercase first characters of up to the first two non-blank whitespace-delimited words).
  - Remove `core/common/.../util/PlaceExtensions.kt` (updating Android imports to `place.location`), remove the private `String.placeInitials()` in Android's `PlaceCategoryScreen.kt`, and remove the duplicate `Place.geoLocation` and `String.placeInitials` extensions in iOS's `CategoryRoute.swift` and manual `GeoLocation(latitude: place.latitude, longitude: place.longitude)` construction in `ARCameraModel.swift`.
  - Add unit tests for `Place.location` and `Place.initials` in `:core:model/src/commonTest`.
- [ ] **Unified distance formatting across Android and iOS**:
  - Expose a top-level or object-scoped distance formatter (`formatDistance(meters: Float): String` Alongside `Float.formattedDistance(): String`) in `:core:common` (which is exported by `:shared` to Swift) and `val PlaceSearchRadius.formattedLabel: String` (using the same meter/kilometer formatting).
  - Replace the duplicate distance formatting implementations in Android's `ARMarkerRenderer.kt` (`ARMarker.formattedDistance()`) and iOS's `DistanceFormat.swift`, `ARCameraOverlay.swift` (`distanceText`), and `MainScreen.swift` (`PlaceSearchRadius.title`) with the shared formatter, and delete `iosApp/Sources/DistanceFormat.swift`.
- [ ] **Relocate cross-platform AR modules from `:shared` into `:core:ar/src/commonMain`**:
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
