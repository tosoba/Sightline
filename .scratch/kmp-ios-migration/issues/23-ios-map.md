# 23: iOS map (MapLibre)

**Spec:** `.scratch/kmp-ios-migration/specs/map.md`

**What to build:** An iOS user sees nearby places on a MapLibre map (wrapped via `UIViewRepresentable`) with the shared style JSONs, a current-location marker, and bounding-box fit, matching the Android map's features.

**Blocked by:** 22 (shared + iosApp)

**Status:** resolved

- [x] The MapLibre map renders with the shared style JSONs.
- [x] User location, place markers, and bounding-box fit work.
- [x] MapKit remains the documented fallback if MapLibre proves awkward.

## Comments

### MapLibre version

Pinned `7.0.0` (exact version) from `https://github.com/maplibre/maplibre-gl-native-distribution`, resolved by SPM in `iosApp/project.yml`.
It is the latest stable release (28 Oct 2026), raises the minimum iOS version to 15.5 - below our 17.0 deployment target - and only replaces the Objective-C umbrella header (`Mapbox.h` to `MapLibre.h`), so Swift code keeps `import MapLibre`.
It built cleanly as a dynamic XCFramework on Xcode 27.2 with the iOS 27.2 simulator, so the documented `6.31.0` fallback was not needed.
The dependency is on the `iosApp` target only; `shared` stays free of map rendering code.

### Style resources

`light_style.json` and `dark_style.json` in `iosApp/Resources/` are copies of `core/ui/src/main/res/raw`, produced by the root Gradle task `:syncIosMapStyles`.
The Xcode "Build Shared framework" pre-build phase runs `./gradlew :syncIosMapStyles :shared:embedAndSignAppleFrameworkForXcode`, so both platforms refresh from one source of truth on every iOS build.
`iosApp/project.yml` also names that source in a comment next to the `Resources` source entry.
The map loads the JSON through a local file `styleURL` chosen from the SwiftUI `colorScheme`, and re-adds annotations after the asynchronous style change, so light/dark follows the system like Android.

### Map features

`iosApp/Sources/MapLibreMapView.swift` is the `UIViewRepresentable` around `MLNMapView`.
It sets `showsUserLocation = true` (blue dot) and leaves `isZoomEnabled`, `isScrollEnabled`, `isRotateEnabled`, and `isPitchEnabled` explicitly `true`, so pan/zoom/rotate/pitch are all available.
Place markers are `MLNPointAnnotation`s with `title` = place name and `subtitle` = category title; `mapView(_:annotationCanShowCallout:)` returns `true`, so tapping a marker shows the standard two-line callout.
One `Coordinator` owns the annotations, the applied style, and the last fitted bounds.
It syncs annotations when the place IDs change and on `didFinishLoadingStyle`, which also re-fits the camera after a style change.
The camera fit uses the shared bounds with `percentageIncrease = 0.1` and `UIEdgeInsets(top: 64, left: 40, bottom: 120, right: 40)`, mirroring Android's padded fit.

### Shared bounding-box math

The pure calculation moved to `core:model` commonMain as `GeoBounds(west, south, east, north)` plus `GeoBounds.paddedAround(places, percentageIncrease = 0.0): GeoBounds?`.
It also exposes `center: GeoLocation`.
The padding semantics are unchanged from `core/ui/MapPlacesBoundingBox.kt`: each side extends by `percentageIncrease / 2` of the latitude/longitude span, and an empty list returns `null`.
`core/ui/MapPlacesBoundingBox.kt` now only converts `GeoBounds` to MapLibre's `BoundingBox`, so Android and iOS share one implementation.
`GeoBounds` is exported through `shared` (it lives in the already exported `core:model`); Swift calls `GeoBounds.companion.paddedAround(places:percentageIncrease:)`.
`core/model/src/commonTest/.../GeoBoundsTest.kt` covers empty input, exact extremes, padded arithmetic, a single place, and the center, and runs on both the Android host and the iOS simulator targets.

### Pager shell

`iosApp/Sources/MainScreen.swift` is the new root: a `NavigationStack` containing a shared toolbar and a paged `TabView`.
The toolbar holds the category `Picker` and a segmented Camera/Map `Picker`; both pages read the same `NearbyPlacesModel`, so switching pages keeps the selected category.
`MainPage` in the same file enumerates `.camera` and `.map`.
The Camera page is `NearbyPlacesView`, now content-only (it takes the shared model and no longer owns `NavigationStack`, the category picker, or the observation task); ticket 24 replaces that content with the AR camera.
The Map page is `iosApp/Sources/MapScreen.swift`.
`SightlineApp.swift` now starts `MainScreen`.
Observation of authorization and location moved to the root `.task`, so both pages receive the same state.
The `PlaceCategory.title` extension moved to `iosApp/Sources/PlaceCategoryTitle.swift` so the map callouts can reuse it.
`iosApp/UITests/MapPageUITests.swift` plus the `iosAppUITests` target exist so the map page can be switched to and captured in CI; run them with `xcodebuild test`.

### Verification

Android and the shared module:

```text
$ ./gradlew :app:assembleDebug test --console=plain
BUILD SUCCESSFUL in 20s
283 actionable tasks: 279 executed, 4 up-to-date

$ ./gradlew :core:model:iosSimulatorArm64Test --console=plain
BUILD SUCCESSFUL in 4s
iosSimulatorArm64Test.com.trm.sightline.core.model.GeoBoundsTest tests="5" failures="0"
```

iOS build and run (iPhone 17, iOS 27.2 simulator, Xcode 27.2):

```text
$ cd iosApp && xcodegen generate
Created project at .../iosApp/iosApp.xcodeproj

$ xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 17' build
** BUILD SUCCEEDED **

$ xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 17' test
Test Case '-[iosAppUITests.MapPageUITests testMapPageRendersPlaces]' passed
** TEST SUCCEEDED **
```

The UI test launches the app, taps the Map segment, and keeps a screenshot attachment.
Location was pre-granted and set to Krakow (`50.0614,19.9366`); the app logged the usual photon request and place fetch.
The captured map shows the OpenFreeMap vector tiles from the bundled light style, the blue user-location dot, the place markers, and the padded bounds fit.
The same test with the simulator in dark appearance shows the bundled dark style with the same markers.
Screenshots: `screenshots/ios_map_light.png` and `screenshots/ios_map_dark.png`.
Tiles loaded normally; no MapLibre tile errors were observed.

### Handover for ticket 25

- The root `NavigationStack` already lives in `MainScreen.swift`; push the place-category destination from there instead of wrapping the root again.
- The category `Picker` also lives in `MainScreen` and drives the shared model, so a pushed detail screen should receive the selected `PlaceCategory` and `[Place]` rather than own another picker.
- `NearbyPlacesModel` exposes `selectedCategory`, `places`, `authorization`, `hasLocation`, and `isLoading`; add the preferences wiring through `SightlineShared.preferences` rather than creating a second model owner.
- Ticket 24 should swap the `NearbyPlacesView` branch inside `MainScreen`'s `TabView` for the AR camera, and keep `MapScreen` as the second page.
