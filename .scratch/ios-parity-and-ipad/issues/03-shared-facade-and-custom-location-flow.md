Type: grilling
Status: resolved
Blocked by: 02

## Question

Lock the shared KMP facade surface and iOS state model for multi-category places, reverse geocoding, and custom location search (Issue 26):

1. **Shared Facade vs. Shared ViewModel logic (`SightlineShared`)**:
   - Currently `MainViewModel.kt` on Android is an AndroidX `ViewModel` holding the multi-category state (`Map<PlaceCategory, LoadingState<List<Place>>>`), 1-second debounced category toggles, `AddressRepository.getAddress` reverse geocoding of the live `userLocation`, and 1-second debounced `AddressRepository.search(query, limit = 100)` for custom location search.
   - Decide what `SightlineShared` exposes to `iosApp` (`AddressRepository` methods `getAddress` and `search`, or higher-level shared state coordinator in `shared`) and how `NearbyPlacesModel` manages per-category `LoadingState`, active location precedence (`userLocationEnabled ? userLocation : customLocation`), and error banners/toasts.

2. **Custom Location UX on iOS (Issue 26)**:
   - How the custom location search bar inside the HIG places sheet (from [Full-screen layout and HIG places sheet](02-fullscreen-layout-and-hig-places-sheet.md)) transitions into search mode, displays query validation ("Start typing to search" / "Query too short" for `< 3` chars), lists `CustomLocation` results, persists the chosen `CustomLocation` via `PreferencesFacade.setCustomLocation`, and interacts with the "Use my location" toggle and permission/settings banners.

## Answer

1. **Shared Reactive `PlacesCoordinator` in `shared/commonMain` with SKIE-Friendly State Types**:
   - Expose `addressRepository: AddressRepository` on `IosAppGraph` and wire it into `SightlineShared` alongside `reverseGeocode(location: GeoLocation): String?` and `searchAddresses(query: String, limit: Int = 100): List<CustomLocation>`.
   - To maximize code sharing without hitting Objective-C generic-interface type erasure on `LoadingState<out T : Any>`, define concrete, non-generic sealed types in `shared/commonMain` that SKIE bridges into exhaustive Swift `enum`s with typed payloads:
     - `CategoryPlacesState`: `Loading` | `Loaded(val places: List<Place>)`
     - `LocationAddressState`: `Loading(val previousAddress: String)` | `Loaded(val address: String)`
     - `CustomLocationSearchState`: `Idle` (0 trimmed chars) | `QueryTooShort` (1-2 trimmed chars) | `Loading` | `Loaded(val results: List<CustomLocation>)`
   - Implement `PlacesCoordinator` in `shared/src/commonMain/kotlin/com/trm/sightline/shared/` (owned/created by `SightlineShared` with a `CoroutineScope` and tested deterministically with virtual time `runTest` + `advanceTimeBy` in `shared/commonTest`):
     - Exposes `StateFlow`s for `places: Map<PlaceCategory, CategoryPlacesState>`, `allPlaces: List<Place>`, `activeLocation: GeoLocation?`, `userLocation: GeoLocation?`, `userLocationAddress: LocationAddressState`, `customLocation: GeoLocation?`, `customLocationAddress: String`, `customLocationSearchResults: CustomLocationSearchState`, `userLocationEnabled: Boolean`, `searchRadius: PlaceSearchRadius`, `lastMapPosition: MapCameraPosition?`, and `error: RequestError?`.
     - Owns the 1-second category toggle debounce, 35-second place fetch timeout (`TimeoutCancellationException -> RequestError.IO`), distance sorting from `activeLocation`, 1-second reverse-geocoding delay on `userLocation` updates, 1-second custom-location search debounce (`limit = 100`), 1-second map camera position save debounce, 4-second `RequestError` auto-clear timer, and automatic `setUserLocationEnabled(false)` when system location services are off while `userLocationEnabled` is true.
   - Keep `@MainActor @Observable NearbyPlacesModel` on iOS as a thin SwiftUI `@Observable` adapter that observes `PlacesCoordinator`'s `StateFlow`s via SKIE `for await` and forwards user intents (`onTogglePlaceCategory`, `setCustomLocationAddress`, `onCustomLocationSearchResultClick`, `cancelCustomLocationSearch`, `setUserLocationEnabled`, `setSearchRadius`, `saveMapPosition`).
   - Rename `NearbyPlacesModel.currentLocation` (and related iOS/shared location references) to `activeLocation` to match `GLOSSARY.md`.
   - Leave Android's `MainViewModel` untouched so Android remains a zero-diff reference baseline.

2. **Active Location Precedence, Persistence, and Category Refresh Semantics**:
   - Follow Android's actual code semantics (`MainViewModel.kt` + `UserPreferencesLocalRepository.kt`): toggling **"Use my location"** ON does **not** clear or overwrite the persisted `CustomLocation` in `UserPreferencesRepository`.
   - Compute **`activeLocation`** as `userLocationEnabled ? userLocation : customLocation`. Toggling **"Use my location"** back OFF immediately restores the previously saved `CustomLocation` and `customLocationAddress`.
   - Editing the custom location search text (`setCustomLocationAddress`) sets the in-memory `customLocation` to `null` (disabling unselected category toggles) until a search result is tapped; tapping `"Cancel"` without picking a result restores the persisted `CustomLocation` and `customLocationAddress`.
   - When `activeLocation` becomes `null` (e.g., turning on "Use my location" before a GPS fix arrives, or editing/clearing the custom location query), clear `places` so stale markers from a previous location are not shown on the AR Camera or Map.
   - When `searchRadius` changes or a new `CustomLocation` is selected while categories are active, automatically re-fetch the active categories for the new radius/location.

3. **Custom Location Search UX inside the HIG Places Sheet**:
   - At the top of the Places `.sheet` root view, render a **Location Header Row**:
     - **When `userLocationEnabled == true`**: A non-editable status field showing a leading `ProgressView` with `"Loading location..."` (while `userLocation == nil`) or `"Loading location address..."` (while `userLocationAddress` is `.loading`), or a leading `location.fill` icon with the reverse-geocoded address (falling back to `"Location address not found."` when empty).
     - **When `userLocationEnabled == false`**: An interactive rounded search text field (`"Enter your location."` placeholder, pre-filled with `customLocationAddress`, with a leading `magnifyingglass` icon and an inline clear `xmark.circle.fill` button while editing).
     - **Trailing controls**: The **"Use my location"** toggle button (`location.fill` / `location`, highlighted with accent fill when enabled), plus a `"Cancel"` button while the search field is focused.
   - **Search Mode Transition & Validation**:
     - Focusing the search field automatically expands `sheetDetent` to `.large` and swaps the sheet body below the header from the 13-category grid to the **Custom Location Search Results list**:
       - `.idle` (0 trimmed chars): centered prompt `"Start typing to search for a location."`
       - `.queryTooShort` (1-2 trimmed chars): centered prompt `"Your query must be at least 3 characters long."`
       - `.loading` (`>= 3` trimmed chars, after 1-second debounce): inline `ProgressView` loading indicator.
       - `.loaded(let results)`: list of `CustomLocation` rows (leading `mappin.and.ellipse` icon + `result.address`).
     - Tapping a `CustomLocation` row dismisses keyboard focus, exits search mode back to the 13-category grid, and persists the selection via `PreferencesFacade.setCustomLocation`. Tapping `"Cancel"` or toggling **"Use my location"** ON also dismisses keyboard focus and exits search mode.

4. **Error Classification & Transient Error Banner**:
   - Map `RequestError` (from `PlacesCoordinator.error`) to Android's exact user-facing strings (`RequestError.IO` -> `"A connection error occurred."`, `RequestError.Http` -> `"A server error occurred - try again later."`, `RequestError.Other` -> `"An unknown error occurred - try again later."`), displayed in the top floating glass error banner and automatically cleared after 4 seconds.
