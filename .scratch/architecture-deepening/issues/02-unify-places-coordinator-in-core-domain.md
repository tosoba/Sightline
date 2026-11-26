# 02: Move PlacesCoordinator to :core:domain and unify Android MainViewModel and iOS NearbyPlacesModel

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move `PlacesCoordinator` and `PlacesStates` (`CategoryPlacesState`, `LocationAddressState`, `CustomLocationSearchState`) from `:shared` into `:core:domain/src/commonMain`, add a consolidated `StateFlow<PlacesUiState>` snapshot stream at the coordinator seam, migrate `PlacesCoordinatorTest` to `:core:domain/src/commonTest` (running on both Android JVM and iOS Simulator), refactor Android's `MainViewModel` and UI to delegate to `PlacesCoordinator`, and simplify iOS's `NearbyPlacesModel` to collect the single consolidated state stream.

**Blocked by:** 01

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **`PlacesCoordinator` and state types in `:core:domain`**:
  - Move `PlacesCoordinator.kt` and `PlacesStates.kt` from `:shared` into `:core:domain/src/commonMain/kotlin/com/trm/sightline/core/domain/places/` (adding `api(project(":core:common"))` to `:core:domain` and `export(project(":core:domain"))` in `shared/build.gradle.kts`).
  - Expose a consolidated `val state: StateFlow<PlacesUiState>` on `PlacesCoordinator` containing `places: Map<PlaceCategory, CategoryPlacesState>`, `allPlaces: List<Place>`, `activeLocation: GeoLocation?`, `userLocation: GeoLocation?`, `userLocationAddress: LocationAddressState`, `customLocation: GeoLocation?`, `customLocationAddress: String`, `customLocationSearchResults: CustomLocationSearchState`, `userLocationEnabled: Boolean`, `searchRadius: PlaceSearchRadius`, `lastMapPosition: MapCameraPosition?`, and `error: RequestError?`.
  - Allow passing an external `CoroutineScope` into `PlacesCoordinator`'s public constructor (defaulting to `CoroutineScope(SupervisorJob() + Dispatchers.Main)` for iOS), so Android's `MainViewModel` can pass `viewModelScope`.
  - Move `PlacesCoordinatorTest.kt` from `:shared/src/commonTest` to `:core:domain/src/commonTest` (adding `kotlinx-coroutines-test` to `:core:domain` `commonTest`), ensuring all coordinator tests pass on both Android host (`:core:domain:testAndroidHostTest`) and iOS Simulator (`:core:domain:iosSimulatorArm64Test`).
- [ ] **Android `MainViewModel` & UI backed by `PlacesCoordinator`**:
  - Refactor `app/src/main/java/com/trm/sightline/MainViewModel.kt` to construct and delegate to `PlacesCoordinator(..., scope = viewModelScope)`, removing the duplicated flow/snapshot state plumbing (`handlePlaces`, `AtomicLong` debounce, `mutableStateMapOf`, separate `userLocation` / `customLocation` vars).
  - Update `MainActivity.kt`, `MainScreen.kt`, and `feature/places/.../PlacesContent.kt` to consume `PlacesUiState` (including `activeLocation`, `CategoryPlacesState`, `LocationAddressState`, and `CustomLocationSearchState`, plus `cancelCustomLocationSearch()`).
  - Verify that on Android:
    - Active Place Categories automatically refetch when `activeLocation` or `searchRadius` changes.
    - `activeLocation` is `null` (not falling back to `customLocation`) when `userLocationEnabled` is `true` and `userLocation` is not yet available.
    - Toggling `userLocationEnabled` on and off preserves the persisted `CustomLocation`.
- [ ] **iOS `NearbyPlacesModel` single-stream observation**:
  - Refactor `iosApp/Sources/NearbyPlacesModel.swift` to observe `coordinator.state` in a single `for await state in coordinator.state` loop instead of spawning an 11-branch `withTaskGroup`.
  - Preserve all existing `@Observable` properties and intent methods on `NearbyPlacesModel` so all SwiftUI views and UI tests work unchanged.

## Verification Commands

```sh
./gradlew :core:domain:allTests :shared:iosSimulatorArm64Test :shared:assembleSharedXCFramework --console=plain
./gradlew :app:assembleDebug test --console=plain
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```
