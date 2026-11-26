# 02: Move PlacesCoordinator to :core:domain and unify Android MainViewModel and iOS NearbyPlacesModel

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move `PlacesCoordinator` and `PlacesStates` (`CategoryPlacesState`, `LocationAddressState`, `CustomLocationSearchState`) from `:shared` into `:core:domain/src/commonMain`, add a consolidated `StateFlow<PlacesUiState>` snapshot stream at the coordinator seam, migrate `PlacesCoordinatorTest` to `:core:domain/src/commonTest` (running on both Android JVM and iOS Simulator), refactor Android's `MainViewModel` and UI to delegate to `PlacesCoordinator`, and simplify iOS's `NearbyPlacesModel` to collect the single consolidated state stream.

**Blocked by:** 01

**Status:** resolved

## Acceptance Criteria

- [x] **`PlacesCoordinator` and state types in `:core:domain`**:
  - Move `PlacesCoordinator.kt` and `PlacesStates.kt` from `:shared` into `:core:domain/src/commonMain/kotlin/com/trm/sightline/core/domain/places/` (adding `api(project(":core:common"))` to `:core:domain` and `export(project(":core:domain"))` in `shared/build.gradle.kts`).
  - Expose a consolidated `val state: StateFlow<PlacesUiState>` on `PlacesCoordinator` containing `places: Map<PlaceCategory, CategoryPlacesState>`, `allPlaces: List<Place>`, `activeLocation: GeoLocation?`, `userLocation: GeoLocation?`, `userLocationAddress: LocationAddressState`, `customLocation: GeoLocation?`, `customLocationAddress: String`, `customLocationSearchResults: CustomLocationSearchState`, `userLocationEnabled: Boolean`, `searchRadius: PlaceSearchRadius`, `lastMapPosition: MapCameraPosition?`, and `error: RequestError?`.
  - Allow passing an external `CoroutineScope` into `PlacesCoordinator`'s public constructor (defaulting to `CoroutineScope(SupervisorJob() + Dispatchers.Main)` for iOS), so Android's `MainViewModel` can pass `viewModelScope`.
  - Move `PlacesCoordinatorTest.kt` from `:shared/src/commonTest` to `:core:domain/src/commonTest` (adding `kotlinx-coroutines-test` to `:core:domain` `commonTest`), ensuring all coordinator tests pass on both Android host (`:core:domain:testAndroidHostTest`) and iOS Simulator (`:core:domain:iosSimulatorArm64Test`).
- [x] **Android `MainViewModel` & UI backed by `PlacesCoordinator`**:
  - Refactor `app/src/main/java/com/trm/sightline/MainViewModel.kt` to construct and delegate to `PlacesCoordinator(..., scope = viewModelScope)`, removing the duplicated flow/snapshot state plumbing (`handlePlaces`, `AtomicLong` debounce, `mutableStateMapOf`, separate `userLocation` / `customLocation` vars).
  - Update `MainActivity.kt`, `MainScreen.kt`, and `feature/places/.../PlacesContent.kt` to consume `PlacesUiState` (including `activeLocation`, `CategoryPlacesState`, `LocationAddressState`, and `CustomLocationSearchState`, plus `cancelCustomLocationSearch()`).
  - Verify that on Android:
    - Active Place Categories automatically refetch when `activeLocation` or `searchRadius` changes.
    - `activeLocation` is `null` (not falling back to `customLocation`) when `userLocationEnabled` is `true` and `userLocation` is not yet available.
    - Toggling `userLocationEnabled` on and off preserves the persisted `CustomLocation`.
- [x] **iOS `NearbyPlacesModel` single-stream observation**:
  - Refactor `iosApp/Sources/NearbyPlacesModel.swift` to observe `coordinator.state` in a single `for await state in coordinator.state` loop instead of spawning an 11-branch `withTaskGroup`.
  - Preserve all existing `@Observable` properties and intent methods on `NearbyPlacesModel` so all SwiftUI views and UI tests work unchanged.

## Verification Commands

```sh
./gradlew :core:domain:allTests :shared:iosSimulatorArm64Test :shared:assembleSharedXCFramework --console=plain
./gradlew :app:assembleDebug test --console=plain
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Answer

Implemented on `ticket/02-places-coordinator` as commit `4fd3075` and merged into `integration/architecture-deepening` as merge commit `61ed1c3`.

- `PlacesCoordinator`, `PlacesUiState`, `CategoryPlacesState`, `LocationAddressState`, and `CustomLocationSearchState` now live in `:core:domain/src/commonMain/kotlin/com/trm/sightline/core/domain/places/`, and `:shared` exports `:core:domain` so Swift sees the same types through `Shared.framework`.
- `:core:domain` declares `api(project(":core:common"))` for `RequestError` and `LocationProvider`, and `:core:domain` `commonTest` picks up `kotlinx-coroutines-test`.
- `PlacesCoordinator` exposes `val state: StateFlow<PlacesUiState>`, built by combining the fine-grained flows and started eagerly on the coordinator scope, alongside the individual flows that still back focused consumers.
- The public constructor accepts an external `CoroutineScope` defaulting to `CoroutineScope(SupervisorJob() + Dispatchers.Main)`, and Android passes `viewModelScope` so all coordinator effects are tied to the ViewModel.
- The moved coordinator keeps `cancelCustomLocationSearch()`, which cancels the pending query and restores the persisted custom-location address and location, and keeps the existing radius and active-location refetch of active categories.
- `activeLocation` is derived from `userLocation` when `userLocationEnabled` is true and from `customLocation` otherwise, so it stays null instead of falling back to the custom location while the user location is still pending.
- `PlacesCoordinatorTest` moved to `:core:domain/src/commonTest` with 28 tests, adding coverage for radius changes, active-location changes, null-location gating, consolidated state, and search-cancel restoration.
- Android's `MainViewModel` constructs `PlacesCoordinator(..., scope = viewModelScope)` and delegates intents to it; the duplicated flow plumbing (`handlePlaces`, `AtomicLong` debounce, `mutableStateMapOf`, separate `userLocation`/`customLocation` vars, `LoadingState` mapping, and the local error-clear job) is deleted, shrinking the file from 285 to roughly 80 lines.
- `MainActivity`, `MainScreen`, `PlacesContent`, and `PlaceCategoryItem` now consume `PlacesUiState` and the places-specific state types, including `activeLocation`, `CategoryPlacesState`, `LocationAddressState`, and `CustomLocationSearchState`.
- One UI deviation: Android has no dedicated cancel button, so `cancelCustomLocationSearch()` is wired to the search bar's back arrow when the search is expanded, which cancels the pending search and restores the saved location before collapsing the bar.
- iOS's `NearbyPlacesModel` replaces the 11-branch `withTaskGroup` with a single `for await state in coordinator.state` loop feeding `apply(_:)`, and keeps every existing `@Observable` property and intent method unchanged.

Verification: on the merge commit `./gradlew :core:domain:allTests :app:assembleDebug test --console=plain` reports `BUILD SUCCESSFUL` in 10s across 312 actionable tasks, and `PlacesCoordinatorTest` runs 28 tests with zero failures and zero skips on both `testAndroidHostTest` and `iosSimulatorArm64Test` (56 test executions in total).
