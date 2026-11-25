# 07: Shared PlacesCoordinator and address facade

**Spec:** `.scratch/ios-parity-and-ipad/spec.md` (Phase 2), `.scratch/ios-parity-and-ipad/issues/03-shared-facade-and-custom-location-flow.md`

**What to build:** Expose `AddressRepository` through `IosAppGraph` and `SightlineShared`, define SKIE-friendly non-generic sealed state models (`CategoryPlacesState`, `LocationAddressState`, `CustomLocationSearchState`), implement the reactive `PlacesCoordinator` in `shared/commonMain` with deterministic virtual-time unit tests in `shared/commonTest`, and rewire `NearbyPlacesModel` on iOS as a thin `@MainActor @Observable` adapter using `activeLocation` terminology from `GLOSSARY.md`.

**Blocked by:** None

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **Metro DI & `SightlineShared` surface**:
  - `IosAppGraph` exposes `val addressRepository: AddressRepository`.
  - `SightlineShared` accepts `addressRepository: AddressRepository`, exposes `suspend fun reverseGeocode(location: GeoLocation): String?` and `suspend fun searchAddresses(query: String, limit: Int = 100): List<CustomLocation>`, and provides `val placesCoordinator: PlacesCoordinator`.
- [ ] **SKIE-friendly non-generic state types in `shared/commonMain`**:
  - `CategoryPlacesState`: `data object Loading` | `data class Loaded(val places: List<Place>)`
  - `LocationAddressState`: `data class Loading(val previousAddress: String = "")` | `data class Loaded(val address: String)`
  - `CustomLocationSearchState`: `data object Idle` | `data object QueryTooShort` | `data object Loading` | `data class Loaded(val results: List<CustomLocation>)`
- [ ] **`PlacesCoordinator` in `shared/commonMain`**:
  - Exposes `StateFlow`s for `places: Map<PlaceCategory, CategoryPlacesState>`, `allPlaces: List<Place>`, `activeLocation: GeoLocation?`, `userLocation: GeoLocation?`, `userLocationAddress: LocationAddressState`, `customLocation: GeoLocation?`, `customLocationAddress: String`, `customLocationSearchResults: CustomLocationSearchState`, `userLocationEnabled: Boolean`, `searchRadius: PlaceSearchRadius`, `lastMapPosition: MapCameraPosition?`, and `error: RequestError?`.
  - Starts with `places = emptyMap()` (zero automatic network requests on launch).
  - Implements 1-second debounced `onTogglePlaceCategory(category: PlaceCategory)` with immediate `CategoryPlacesState.Loading` on toggle-on, immediate removal and job cancellation on toggle-off, 35-second fetch timeout (`TimeoutCancellationException -> RequestError.IO`), distance sorting from `activeLocation`, and 4-second `RequestError` auto-clear.
  - Computes `activeLocation = if (userLocationEnabled) userLocation else customLocation`; toggling `"Use my location"` ON does **not** clear persisted `CustomLocation`, and toggling it OFF restores the saved `CustomLocation` and `customLocationAddress`.
  - Clears `places` whenever `activeLocation` becomes `null`, and re-fetches active categories whenever `activeLocation` or `searchRadius` changes.
  - Delays 1 second before calling `addressRepository.getAddress` on `userLocation` updates, and automatically persists `setUserLocationEnabled(false)` if `locationProvider.isLocationEnabled()` emits `false` while `userLocationEnabled` is true.
  - Implements `setCustomLocationAddress(query: String)` (clearing in-memory `customLocation` to `null` while editing, emitting `Idle` for 0 trimmed chars, `QueryTooShort` for 1-2 trimmed chars, and `Loading` -> 1-second debounced `addressRepository.search(query.trim(), limit = 100)` -> `Loaded` for `>= 3` trimmed chars), `onCustomLocationSearchResultClick(location: CustomLocation)` (persisting via `UserPreferencesRepository.setCustomLocation`), and `cancelCustomLocationSearch()` (restoring persisted `CustomLocation` and `customLocationAddress`).
  - Debounces `saveMapPosition(position: MapCameraPosition)` by 1 second.
- [ ] **Deterministic unit tests in `shared/commonTest`**:
  - `PlacesCoordinatorTest` verifies initial zero-category state, 1-second category toggle debounce and cancellation, 35-second timeout and 4-second error auto-clear, `activeLocation` precedence and non-destructive `CustomLocation` persistence across `"Use my location"` toggles, reverse-geocoding delay, custom-location search validation/debounce/cancellation, and `isLocationEnabled == false` auto-disable.
- [ ] **Thin `@MainActor @Observable NearbyPlacesModel` on iOS**:
  - Observes `PlacesCoordinator` flows via SKIE `for await`, renames `currentLocation` to `activeLocation` across `iosApp/Sources/`, and forwards user actions to `PlacesCoordinator`.
  - Android remains untouched and green.

## Verification Commands

```sh
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
./gradlew :app:assembleDebug test --console=plain
```
