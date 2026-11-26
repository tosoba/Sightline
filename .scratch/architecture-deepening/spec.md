# Spec: Cross-Platform Architecture Deepening and Source-Set Alignment

Status: ready-for-agent

## Problem Statement

As Sightline grew to support both Android and iOS with feature parity, several architectural seams drifted apart or remained shallow:

1. **Duplicated Place discovery and Active Location orchestration**:
   The reactive state coordinator for Place discovery, Active Location resolution (User Location versus Custom Location), Search Radius changes, Place Category toggle debouncing, reverse geocoding, Custom Location search validation, and Map camera persistence lives inside the iOS-only umbrella module (`:shared`), which has no Android target.
   Android's presentation layer re-implements this entire state machine with a mix of snapshot state and coroutine flows that has zero unit test coverage, fails to refetch active Place Categories when the Active Location or Search Radius changes, fails to clear the last User Location fix when live tracking is turned off, and resolves Active Location inconsistently between the screen host and view model.
   Meanwhile, the iOS presentation model spawns eleven parallel asynchronous collection loops to mirror individual state streams from the coordinator.

2. **Misleading `:shared` source sets and misplaced cross-platform AR modules**:
   The `:shared` module only declares iOS targets to build the iOS framework, yet places nearly all of its implementation and tests in `commonMain` and `commonTest`.
   Inside `:shared` sit cross-platform AR modules (blur level-of-detail animation, orientation matrix tracking, circular angle low-pass smoothing, and marker bounding rectangle projection) that belong in the AR core module (`:core:ar`), alongside dead legacy pass-through methods and a preferences facade that the iOS app no longer calls.
   At the same time, the circular low-pass angle smoothing algorithm is copy-pasted between the Android sensor manager and the shared orientation tracker.

3. **Duplicated Marker Slot caching, adaptive sizing, and Screen Rotation page-following**:
   The stateless Marker Slot assignment solver stops at a shallow seam.
   Both the Android canvas AR Marker renderer and the iOS AR camera model independently implement the 10-meter Active Location reassignment threshold, adaptive AR Marker card width/height and row count calculation, Marker Slot page-following across Screen Rotation changes, and the 3-page sliding picker window.
   On iOS, the AR camera model has grown into an 800-line module mixing hardware capture and motion coordination, window scene Screen Rotation observation, Marker Slot layout and caching, and hardcoded simulator test fixtures, driven by an 11-modifier `.onChange` synchronization chain in the SwiftUI view.

4. **Flat Swift directory layout and 4x duplicated Place and distance formatting**:
   All Swift and Metal files in the iOS app sit in a single flat source directory without feature or subsystem grouping.
   Basic domain formatting and derived properties - converting a Place to its geographic coordinate, computing 2-letter Place initials, and formatting distances in meters and kilometers - are implemented three to four separate times across Kotlin and Swift modules.

5. **Leaked AR layout constants, duplicated Android adaptive sheet plumbing, and Android AR rendering bugs**:
   Sheet dimension constants sit inside the AR core module, forcing non-AR feature modules (map, places, and place category detail) to declare dependencies on the AR module.
   The Android main screen and Place Category detail screen duplicate over a hundred lines of adaptive bottom-sheet and side-sheet scaffold and window inset math.
   The Android camera preview passes floating-point theme RGB channels truncated to integer zero into the native OpenGL blur renderer and mutates blur rectangles across threads without synchronization on every pixel of sheet drag.

## Solution

Deepen Sightline's cross-platform modules, align source sets with their actual compilation targets, unify Android and iOS behind shared domain and AR modules, organize Swift and Kotlin packages by responsibility, and preserve 100% of existing user-facing behavior across Android and iOS:

1. **Unify Place discovery in the domain module**:
   Relocate the Place discovery coordinator and its non-generic state models into the cross-platform domain module (`:core:domain`), expose a consolidated state snapshot stream alongside fine-grained streams and intent methods, and back both the Android main view model and the iOS observable model with this single tested coordinator.

2. **Make `:shared` an explicit `iosMain` umbrella and deepen `:core:ar`**:
   Move the blur animator, orientation tracker, shared circular low-pass angle filter, and marker rectangle helper into well-structured packages in `:core:ar` (`commonMain`), deduplicate Android's orientation low-pass filter, remove dead legacy pass-through facades from `:shared`, and move the remaining iOS framework entry point and dependency graph in `:shared` from `commonMain`/`commonTest` to `iosMain`/`iosTest`.

3. **Deepen Marker Slot session management in `:core:ar` and decompose the iOS AR camera model**:
   Introduce a stateful Marker Slot layout, caching, and paging module in `:core:ar` (`commonMain`) that encapsulates adaptive card geometry, the 10-meter Active Location cache threshold, Screen Rotation page-following, and the 3-page sliding picker window for both Android and iOS.
   Split simulator launch flags, synthetic fixtures, and window scene Screen Rotation observation out of the iOS AR camera model.

4. **Organize iOS Swift sources into feature directories and enrich `:core:model`**:
   Group the iOS Swift and Metal sources into clear feature and subsystem folders (`App`, `Places`, `Map`, and `ARCamera` with `Capture`, `Rendering`, and `Testing` sub-folders), and centralize `Place` coordinate conversion, 2-letter `Place` initials, and meter/kilometer distance formatting in shared KMP modules exported to Swift.

5. **Clean up Android module dependencies, extract a shared adaptive sheet scaffold, and fix Android AR rendering bugs**:
   Move sheet layout constants and a reusable adaptive bottom/side-sheet scaffold into the shared Android UI module (`:core:ui`), remove unused AR dependencies from non-AR feature modules, collapse shallow camera screen prop-drilling, and fix the native OpenGL blur contrasting-color RGB conversion and cross-thread rectangle updates.

## User Stories

1. As an Android user, I want active Place Categories to automatically refetch when my Active Location moves or when I switch between User Location and Custom Location, so that displayed Places always reflect my current origin.
2. As an Android user, I want active Place Categories to automatically refetch when I change the Search Radius, so that expanding or shrinking the radius immediately updates the Places on the camera and map.
3. As an Android user, I want my saved Custom Location to remain intact when I toggle User Location on and off, and to be restored when I cancel an in-progress Custom Location address search, so that I do not lose my previous Custom Location.
4. As an Android user, I want the AR camera and map to use the exact same Active Location rules as the Place fetcher when live User Location is enabled but waiting for a GPS fix, so that the UI never displays a stale Custom Location while refusing to load Places.
5. As an Android user, I want the AR camera full-frame and marker card blur tint to blend the actual theme surface color rather than pure black when contrasting color mixing is applied, so that frosted AR Marker cards match the material theme in both light and dark modes.
6. As an Android user, I want dragging the Places bottom sheet over the AR camera preview to update the blurred sheet region smoothly without data races between the UI thread and the OpenGL render thread.
7. As an iOS user, I want Place discovery, Custom Location search, Search Radius selection, Map annotations, and AR Marker projection to continue working identically while the underlying state observation runs through a single cohesive state stream.
8. As an iOS or iPad user, I want rotating the device across supported Screen Rotations to preserve my current Marker Slot page and recompute adaptive AR Marker card sizes consistently through the shared AR layout engine.
9. As a developer working in the shared KMP codebase, I want `:shared` to place its iOS-only umbrella facade and dependency graph in `iosMain` and `iosTest`, so that it is immediately obvious that `:shared` is the iOS framework entry point rather than a cross-platform library.
10. As a developer working on AR features, I want 3D math, AR Marker projection, Marker Slot assignment and caching, orientation low-pass smoothing, and blur level-of-detail animation to live in distinct packages inside `:core:ar`, so that all cross-platform AR logic has high locality and is tested in one place.
11. As a developer working on domain rules, I want `:core:domain` to own the Place discovery coordinator and state models rather than just three pass-through repository interfaces, so that domain invariants are enforced and tested once across Android and iOS.
12. As a developer working on the iOS app, I want Swift and Metal files organized into `App`, `Places`, `Map`, and `ARCamera` (`Capture`, `Rendering`, `Testing`) directories with matching Xcode groups, so that navigating the iOS project is fast and predictable.
13. As a developer working on the iOS AR camera, I want simulator test flags, synthetic Place fixtures, and window scene Screen Rotation observers separated from the production AR camera coordinator, so that hardware lifecycle and marker projection logic are easy to read in isolation.
14. As a developer working on Place presentation, I want `Place` initials, `Place` geographic coordinate conversion, and meter/kilometer distance formatting defined once in shared KMP code, so that Android and iOS never drift in how they format Place cards and rows.
15. As a developer working on Android feature modules, I want non-AR feature modules (`:feature:map`, `:feature:places`, `:feature:place-category`) to have zero dependencies on `:core:ar`, so that build graphs and module responsibilities remain clean.
16. As a developer working on Android adaptive layouts, I want a single adaptive bottom-sheet / side-sheet scaffold module in `:core:ui`, so that compact-height landscape side sheets and portrait bottom sheets behave identically across the main screen and the Place Category detail screen without duplicated inset math.

## Implementation Decisions

### 1. Place Discovery Coordinator in `:core:domain`

- Move the Place discovery coordinator (`PlacesCoordinator`) and its non-generic state models (`CategoryPlacesState`, `LocationAddressState`, `CustomLocationSearchState`) from `:shared` into `:core:domain` (`commonMain`), organized into clear sub-packages (`repository` for repository interfaces, `coordinator` or `places` for the coordinator and state models).
- Move `:core:common`'s `LocationProvider` and `RequestError` dependencies so `:core:domain` can depend on them cleanly (either by having `:core:domain` depend on `:core:common`, or by placing `LocationProvider` and `RequestError` in `:core:domain` / `:core:model`).
- Add a consolidated `StateFlow` snapshot (`PlacesUiState` or `PlacesCoordinatorState`) to `PlacesCoordinator` that bundles the current Place Category states, flattened `allPlaces`, `activeLocation`, `userLocation`, `userLocationAddress`, `customLocation`, `customLocationAddress`, `customLocationSearchResults`, `userLocationEnabled`, `searchRadius`, `lastMapPosition`, and `error`.
  Keep existing intent methods (`onTogglePlaceCategory`, `setUserLocationEnabled`, `setCustomLocationAddress`, `onCustomLocationSearchResultClick`, `cancelCustomLocationSearch`, `setSearchRadius`, `saveMapPosition`, `refreshLocationSettings`) on the coordinator interface.
- Add an explicit lifecycle cleanup or scope injection mechanism on `PlacesCoordinator`: when hosted inside Android's `MainViewModel`, pass `viewModelScope` into `PlacesCoordinator` so all location and preference subscriptions cancel automatically when the `ViewModel` clears.
- Refactor Android's `MainViewModel` to delegate all Place, Active Location, Search Radius, Custom Location search, and Map camera position state and intents to `PlacesCoordinator`, removing the ~250 lines of duplicated flow and snapshot state orchestration.
- Update Android's `MainActivity`, `MainScreen`, and `PlacesContent` to consume `PlacesCoordinator`'s state models (`CategoryPlacesState`, `LocationAddressState`, `CustomLocationSearchState`, and `activeLocation`), eliminating the manual `if (userLocationEnabled && userLocation != null)` fallback in `MainActivity` and the manual `< 3` character check in `PlacesContent`.
- Simplify iOS's `NearbyPlacesModel` to observe the consolidated state stream from `PlacesCoordinator` in a single asynchronous loop instead of spawning 11 child tasks in a `TaskGroup`.

### 2. `:shared` as an Explicit `iosMain` Module and `:core:ar` Package Consolidation

- Move `ArBlurAnimator`, `ArOrientationTracker`, and `ArProjection` (merging `markerRect` into `MarkerProjection`) from `:shared` into `:core:ar` (`commonMain`), organized under focused sub-packages:
  - `math`: vector/matrix math, `ScreenRotation`, `RoundedRectF`, `MarkerProjection` (including centered `markerRect`), and `MarkerSlotAssignment`.
  - `orientation`: `Orientation`, `AngleLowPassFilter` (shared circular low-pass smoothing with `SMOOTH_FACTOR = 0.12f` and `SMOOTH_THRESHOLD = CIRCLE / 20f`), and `ArOrientationTracker`.
  - `blur`: `ArBlurAnimator` and `ArBlurFrame`.
- Refactor Android's `OrientationManager` in `:core:ar` (`androidMain`) to use `AngleLowPassFilter` from `commonMain` instead of maintaining a duplicate private low-pass filter method.
- Delete the unused `PreferencesFacade` class and its test, and remove the unused legacy pass-through methods on `SightlineShared` (`preferences`, `nearbyPlaces`, `reverseGeocode`, `searchAddresses`, `locationUpdates`, `categories`) that were superseded by `PlacesCoordinator`.
- Move `SightlineShared` and `IosAppGraph` from `shared/src/commonMain` into `shared/src/iosMain` (alongside `UserPreferencesDirectory.ios.kt`), replacing the `expect fun userPreferencesDirectory()` with a direct `internal fun userPreferencesDirectory()` in `iosMain`.
- Move `SightlineSharedTest` into `shared/src/iosTest` (and relocate `PlacesCoordinatorTest` to `:core:domain/src/commonTest` and `ArBlurAnimatorTest`, `ArOrientationTrackerTest`, and `ArProjectionTest` to `:core:ar/src/commonTest`).

### 3. Shared Marker Slot Session & Paging in `:core:ar` and `ARCameraModel` Decomposition

- Introduce a deep Marker Slot layout, caching, and paging module (`MarkerSlotSession` / `MarkerLayoutCalculator` / `MarkerPageWindow`) in `:core:ar` (`commonMain`, under a `layout` or `paging` package) that encapsulates:
  - Adaptive AR Marker card dimensions and row count (`1..6` rows, target width divisor with platform or configurable bounds, `overlapGuard = markerWidth * 0.5f`, and `baseY` from top inset and vertical spacing).
  - The 10-meter Active Location reassignment threshold (`LOCATION_REASSIGN_THRESHOLD_METERS = 10f`), invalidating cached Marker Slots only when the Active Location moves by at least 10 meters, the Place ID set changes, the Screen Rotation changes, or the usable viewport dimensions change.
  - Screen Rotation and slot-reassignment page-following: tracking previously visible Place IDs on the active page and following the page with the highest overlap count when slots are reassigned, or clamping `currentPage` to `0..maxPage`.
  - The 3-page sliding picker window calculation (`pageWindow(currentPage, totalPages, maxVisiblePages = 3)`) used by both the Android and iOS AR Marker Page Pickers.
- Update both Android's `ARMarkerRenderer` / `CameraPreviewPagePicker` and iOS's `ARCameraModel` / `ARMarkerPagePicker` to use this shared `:core:ar` module instead of maintaining duplicate caching, page-following, and page-window math.
- Decompose iOS's `ARCameraModel` into focused modules:
  - Extract CLI launch arguments (`ARCameraLaunchOptions`, `ARSimulatedUnavailability`) and synthetic Krakow Place / attitude test fixtures into dedicated files under `ARCamera/Testing/`.
  - Extract `UIWindowScene` / `NotificationCenter` interface orientation observation and `ScreenRotation` / `videoRotationAngle` mapping into a dedicated orientation observer module under `ARCamera/Capture/`.
  - Replace the 8 separate `.onChange` modifiers in `ARCameraScreen` with a consolidated input synchronization step (e.g. a single value-type configuration/context struct passed via `.onChange` or direct observation).

### 4. Swift Folder Structure and Shared Place / Distance Formatting

- Reorganize `iosApp/Sources/` from a flat 18-file directory into feature and subsystem directories (which XcodeGen automatically reflects as Xcode groups):
  - `App/`: app entry point (`SightlineApp`), root tab shell (`MainScreen`), and system settings opener (`AppSettings`).
  - `Places/`: observable adapter (`NearbyPlacesModel`), Places & Location sheet (`PlacesSheet`), category drill-down screen (`CategoryPlacesScreen`, renamed from `CategoryRoute`), and category display extensions (`PlaceCategory+Display`, renamed from `PlaceCategoryTitle`).
  - `Map/`: main map tab (`MapScreen`) and MapLibre representable wrapper (`MapLibreMapView`).
  - `ARCamera/`: SwiftUI camera screen (`ARCameraScreen`), camera coordinator (`ARCameraModel`), marker card canvas overlay (`ARCameraOverlay`), plus subdirectories:
    - `ARCamera/Capture/`: `ARCameraCapture`, `ARCameraMotion`, and window scene Screen Rotation observer.
    - `ARCamera/Rendering/`: `ARCameraRenderer` and `ARCameraShaders.metal`.
    - `ARCamera/Testing/`: `ARCameraLaunchOptions` and `ARCameraSyntheticFeed`.
- Enrich `Place` in `:core:model` (`commonMain`) with:
  - `val location: GeoLocation` (replacing `PlaceExtensions.kt` in `:core:common` and Swift `Place.geoLocation` extensions).
  - `val initials: String` (computing the 1- or 2-character uppercase initials from whitespace-delimited words, replacing duplicate implementations in Android's `PlaceCategoryScreen` and iOS's `CategoryPlacesScreen`).
- Expose shared meter/kilometer distance formatting (`formatDistance(meters: Float): String` / `PlaceSearchRadius.formattedDistance`) from KMP so Android (`ARMarkerRenderer`, `PlaceCategoryScreen`, `MainScreen`) and iOS (`ARCameraOverlay`, `CategoryPlacesScreen`, `MainScreen`) call the exact same formatter, and delete `iosApp/Sources/DistanceFormat.swift`.

### 5. Android UI Seams, `:core:ar` Dependency Cleanup, and AR Bug Fixes

- Move sheet dimension constants (`sideSheetWidthDp`, `collapsedBottomSheetContentHeightDp`, `collapsedBottomSheetDragHandleHeightDp`) from `:core:ar` (`ARContextExtensions.kt`) into `:core:ui`.
- Remove the unused or constant-only `implementation(project(":core:ar"))` dependency from `:feature:map`, `:feature:places`, and `:feature:place-category`.
- Extract a deep `AdaptiveSheetScaffold` composable module in `:core:ui` that encapsulates the compact-height check, `BottomSheetScaffold` state and expanded progress tracking, safe-drawing drag handle insets, non-peek sheet height measurement, and the automatic switch between a portrait `BottomSheetScaffold` and a landscape right-aligned side-sheet `Surface`.
  Refactor both `MainScreen` (`:app`) and `PlaceCategoryScreen` (`:feature:place-category`) to use `AdaptiveSheetScaffold`.
- Collapse the shallow `CameraScreen` pass-through wrapper in `:feature:camera` (or narrow its parameter surface) so camera state is not prop-drilled unchanged through an extra layer.
- Fix the contrasting-color RGB conversion bug in `:feature:camera` (`CameraPreview.kt`): convert Compose `Color.red`, `Color.green`, and `Color.blue` (`0f..1f` floats) by scaling `(channel * 255f).roundToInt()` (or passing normalized `0f..1f` floats directly to `OpenGLRenderer.setContrastingColor`) instead of calling `.toInt()` which truncated every channel below `1.0f` to `0`.
- Make `OpenGLRenderer.markerRectFs` and `OpenGLRenderer.otherRectFs` `@Volatile` (with immutable list snapshots) and safely guard `Context.rootWindowInsets` when `decorView.rootWindowInsets` is null before window attachment.

## Testing Decisions

- **What makes a good test**:
  Tests exercise observable behavior at the highest module seam using deterministic fake adapters rather than mocking internal helpers or asserting private state.
  Cross-platform domain, AR, and model behaviors are tested once in KMP `commonTest` and executed on both the Android JVM host (`testAndroidHostTest`) and the iOS Simulator (`iosSimulatorArm64Test`).

- **Seam 1 - Place discovery & Active Location orchestration (`:core:domain` `commonTest`)**:
  - `PlacesCoordinatorTest` migrated from `:shared` to `:core:domain/src/commonTest` so it runs on both Android JVM and iOS Simulator targets.
  - Exercises `PlacesCoordinator` through fake adapters for `PlacesRepository`, `AddressRepository`, `UserPreferencesRepository`, and `LocationProvider` using `kotlinx.coroutines.test.runTest` and virtual time (`advanceTimeBy`).
  - Covers initial zero-category state, 1-second per-category debounce and cancellation, 35-second timeout and 4-second `RequestError` auto-clear, Active Location precedence (`userLocationEnabled ? userLocation : customLocation`), automatic active category refetches when Active Location or Search Radius changes, non-destructive Custom Location preservation across User Location toggles, 1-second reverse geocoding delay, Custom Location search validation (`Idle`, `QueryTooShort`, `Loading`, `Loaded`) and cancel restoration, and consolidated UI state emissions.

- **Seam 2 - AR math, blur, orientation smoothing, and Marker Slot session (`:core:ar` `commonTest`)**:
  - `MarkerProjectionTest`, `MarkerSlotAssignmentTest`, `Math3DTest`, `RoundedRectFTest`, and `VectorTrigTest` remain in `:core:ar/src/commonTest`.
  - `ArBlurAnimatorTest`, `ArOrientationTrackerTest`, and `ArProjectionTest` move from `:shared` into `:core:ar/src/commonTest` so they execute on both Android and iOS targets.
  - Add unit tests for `AngleLowPassFilter` and `MarkerSlotSession` / `MarkerPageWindow` in `:core:ar/src/commonTest` covering the 10-meter Active Location cache threshold, adaptive row and card width clamping across iPhone/Android phone and iPad/tablet viewports, Screen Rotation page-following when previously visible Places move to a new page, and 3-page sliding picker windows.

- **Seam 3 - Place domain properties & distance formatting (`:core:model` and `:core:common` `commonTest`)**:
  - Unit tests in `:core:model/src/commonTest` and `:core:common/src/commonTest` verifying `Place.location`, `Place.initials` (single-word, multi-word, extra whitespace, empty/punctuation edge cases), and `formattedDistance` (`< 1000 m` rounded to meters, `>= 1000 m` formatted to 1 decimal kilometer).

- **Seam 4 - Platform integration & UI gates (`:shared:iosSimulatorArm64Test`, Android Gradle test/build, iOS XCUITests)**:
  - `SightlineSharedTest` in `:shared/src/iosTest` verifying the iOS framework entry point and `PlacesCoordinator` wiring.
  - Android build and unit test suite (`./gradlew test assembleDebug --console=plain`).
  - iOS XcodeGen regeneration, simulator build, and existing XCUITest suites (`SmokeUITests`, `MapPageUITests`, `IPadAdaptiveUITests`) verifying that every user-facing flow on iPhone and iPad behaves identically after the refactoring.

## Sequenced Implementation Tickets

1. [01: Shared Place domain properties, distance formatting, and :core:ar moduleconsolidation](issues/01-shared-model-formatting-and-core-ar-consolidation.md)
2. [02: Move PlacesCoordinator to :core:domain and unify Android MainViewModel and iOS NearbyPlacesModel](issues/02-unify-places-coordinator-in-core-domain.md)
3. [03: Convert :shared into an explicit iosMain umbrella module and remove dead legacy facades](issues/03-shared-iosmain-umbrella-cleanup.md)
4. [04: Deepen Marker Slot session and paging into :core:ar, decompose ARCameraModel, and structure iosApp/Sources](issues/04-deepen-marker-slot-session-and-ios-folder-structure.md)
5. [05: Clean up Android :core:ar dependency leaks, extract AdaptiveSheetScaffold in :core:ui, and fix Android AR rendering bugs](issues/05-android-adaptive-sheet-scaffold-and-ar-fixes.md)

## Out of Scope

- Changing any user-facing functionality, visual design, navigation flow, or network API behavior on Android or iOS.
- Introducing Compose Multiplatform on iOS (iOS UI remains 100% native SwiftUI, UIKit, MapLibre Native, and Metal).
- Replacing the Android C++ OpenGL ES renderer (`:core:ar-native`) or the iOS Metal renderer with a single shared GPU shader pipeline.

## Further Notes

- Tickets are sequenced with explicit `Blocked by:` dependencies so separate agent sessions can execute them safely without merge conflicts.
- After Ticket 03 completes, `:shared` will have zero files in `commonMain`/`commonTest` and will live entirely in `iosMain`/`iosTest` as the iOS framework root.
