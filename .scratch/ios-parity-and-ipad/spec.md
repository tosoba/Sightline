# Spec: iOS Feature, UX, and Orientation Parity + Native iPad Support

Consolidates the wayfinder map and its four design tickets into one buildable plan.
Source: `.scratch/ios-parity-and-ipad/map.md`, `.scratch/ios-parity-and-ipad/issues/01-04`.
Supersedes and absorbs the remaining and reworked iOS items from `.scratch/kmp-ios-migration/issues/24-ios-ar-camera.md`, `.scratch/kmp-ios-migration/issues/25-ios-preferences-navigation-smoke.md`, and `.scratch/kmp-ios-migration/issues/26-ios-custom-location.md`.

## Problem Statement

The initial iOS port (`iosApp`) proved the Metal AR renderer, MapLibre map, and shared KMP foundation, but diverges from the Android reference app and Apple Human Interface Guidelines in four areas:

1. **AR Camera blur, orientation, and pagination gaps (Issue 24)**:
   `ARCameraModel` currently enables full-frame blur (`blurEnabled = true`) during normal viewing, locks `Info.plist` to portrait with hardcoded `ScreenRotation.rotation0`, and drops any AR Marker assigned to `slot.page > 0`.
   On Android, the live camera feed stays sharp during normal viewing while each AR Marker card receives a frosted max-LOD blur backing inside its stencil rect; full-frame blur only activates when device pitch exceeds ±45° or an expanded sheet obscures the camera.
   Furthermore, holding an iOS device horizontally (`roll ≈ ±90°`) with a hardcoded `.rotation0` projection maps horizontal bearing into `screenY` (discarded in favor of `slot.y`) and altitude into `screenX`, causing AR Markers to bunch up and pan along the wrong axis.
2. **Screen-space and single-category navigation gaps (Issue 25)**:
   `MainScreen` currently wraps a custom top header, horizontal category strip, and `PreferencesView` gear sheet around the camera and map, only allows one active Place Category at a time, auto-selects a category when tapped, and pushes a text-only list for category drill-down.
   On Android, the camera and map occupy the full screen with floating controls, zero categories are selected on launch (zero automatic network requests on startup), multiple Place Categories can be toggled on or off independently with separate toggle and drill-down actions, and the category detail screen shows a fitted map above interactive place rows with external Maps handoff.
3. **Missing Custom Location search and shared multi-category state machine (Issue 26)**:
   `PreferencesFacade` persists `CustomLocation`, but `AddressRepository` is not exposed through `SightlineShared`, and `iosApp` has no UI to reverse-geocode the live User Location or search and select a Custom Location when `"Use my location"` is disabled.
4. **No native iPad support**:
   `iosApp/project.yml` targets iPhone only (`TARGETED_DEVICE_FAMILY: "1"`), lacks 4-orientation iPad support (`UISupportedInterfaceOrientations~ipad`), and does not adapt sheet presentation or AR Marker card widths for large iPad viewports and multitasking windows.

## Solution

Bring `iosApp` to full feature, UX, and orientation parity with Android while adhering to Apple HIG and supporting iPad natively, sequenced in three chronological phases while keeping Android untouched and green at every step:

1. **Phase 1 - Issue 24 AR camera fixes**:
   Keep the live AR camera feed sharp by default with frosted max-LOD stencil blur inside AR Marker cards, trigger full-frame blur only when pitch exceeds ±45° or a sheet obscures the camera, unlock landscape orientations mapped to `ScreenRotation` and `AVCaptureConnection.videoRotationAngle`, and add adaptive marker geometry, Marker Slot pagination, and tap-to-toggle overlay visibility.
2. **Phase 2 - Shared `PlacesCoordinator`, full-screen layout, HIG Places & Location sheet, and Custom Location flow (Issues 25 & 26)**:
   Introduce a shared reactive `PlacesCoordinator` in `shared/commonMain` with SKIE-friendly non-generic state enums, replace the top header and `PreferencesView` with a full-screen bottom `TabView` (`Camera` and `Map`) plus floating glass controls, and present a native system `.sheet` containing the Location Header Row, inline Custom Location search, 13-category adaptive grid with independent toggle and drill-down zones, and an interactive map+list `CategoryPlacesScreen`.
3. **Phase 3 - Native iPad adaptive support**:
   Switch `iosApp` to a universal iPhone/iPad target (`TARGETED_DEVICE_FAMILY: "1,2"`) with all 4 orientations on iPad and full multitasking support, adapt the unified system `.sheet` across `.compact` and `.regular` horizontal size classes, and clamp AR Marker widths for large iPad viewports.

## User Stories

1. As an iOS user, I want the live AR camera feed to remain sharp while each AR Marker card has a frosted blurred backing, so I can see my surroundings clearly while reading marker text.
2. As an iOS user, I want the full camera view to blur and hide markers when I tilt the device past ±45° or open an obscuring sheet, so out-of-range projections and background distractions are hidden.
3. As an iOS user, I want to rotate my iPhone or iPad between portrait and landscape and have the camera preview and AR Markers align accurately with the horizon.
4. As an iOS user, I want to page through overflowing Marker Slots and tap empty camera space to hide or show floating controls, so dense areas remain readable and uncluttered.
5. As an iOS user, I want the app to launch with zero selected Place Categories and make no automatic network requests until I toggle a category on.
6. As an iOS user, I want to toggle multiple Place Categories on and off independently, see per-category loading spinners and loaded counts, and drill into any loaded category's interactive map and place list.
7. As an iOS user, I want to tap a place row in a category detail view to center it on the category map or open it in Apple Maps.
8. As an iOS user, I want to see the reverse-geocoded street address of my live User Location when `"Use my location"` is enabled.
9. As an iOS user, I want to turn off `"Use my location"`, search for an address with validation feedback, and pick a Custom Location as my Active Location without losing my saved Custom Location when I toggle `"Use my location"` on and off.
10. As an iPad user, I want Sightline to run natively across all four device orientations and iPadOS multitasking modes (Split View, Slide Over, Stage Manager) with a centered page sheet and appropriately scaled AR Markers.

## Implementation Decisions

### Phase 1: AR Camera Blur, Landscape Orientation, and Marker Slot Pagination (Issue 24)

#### Full-frame blur trigger and frosted AR Marker cards

- Normal AR viewing starts unblurred (`blurEnabled = false` by default).
- When `blurEnabled == false` and the blur animator is settled (`!frame.isAnimatingLod`), `ARCameraRenderer` draws the sharp camera texture (`arNoBlurFragment`) as the base pass, punches stencil masks (`stencil = 1`) for each AR Marker in `holeRects` using `udRoundBox`, and runs the 8-pass Gaussian pyramid at max LOD (`lod = 2.0`, `contrastingColorMix = 0.025`) to paint frosted blur inside the marker cards (`holeFillStyle = .frosted`).
- Full-frame blur (`blurEnabled = true`, animated over 18 frames via `ArBlurAnimator`) triggers only when:
  1. `pitchOutOfRange` is true (device pitch outside `±45°`).
  2. `sheetObscuresCamera` is true (the Places & Location sheet obscures the camera viewport, including iPhone landscape where the `.medium` detent is inactive and the sheet becomes a full-screen cover).
  3. Debug launch flags force blur (`-arForceBlur` or `-arBlur <0..1>`).
- Debug launch flags override the derived triggers: `-arNoBlur` forces the sharp override and wins over every other trigger, while `-arForceBlur` / `-arBlur <0..1>` force full-frame blur.
- Whenever `pitchOutOfRange || sheetObscuresCamera` is true, `ARCameraModel` clears `placements` and `holeRects` so AR Markers are hidden and `ARCameraRenderer` skips the second (hole-fill) pass.
- During the 18-frame unblur transition (`!blurEnabled && frame.isAnimatingLod`), visible marker holes continue to render frosted so cards never flash transparent mid-ramp.
- Sheet and floating UI backdrops over the camera use native SwiftUI materials (`.ultraThinMaterial` / `.regularMaterial`), so `ARCameraRenderer` only stencil-blurs `holeRects` for AR Marker cards.
- `contrastingColor` is dynamically derived from `UIColor.secondarySystemBackground` resolved for the active SwiftUI `ColorScheme`, easing over 60 frames via `ArBlurAnimator` when appearance changes.

#### Horizontal (landscape) orientation and Screen Rotation mapping

- Unlock `UIInterfaceOrientationPortrait`, `UIInterfaceOrientationLandscapeLeft`, and `UIInterfaceOrientationLandscapeRight` in `iosApp/Support/Info.plist` for iPhone.
- Observe the active window scene's `UIInterfaceOrientation` (plus `NotificationCenter` orientation/window geometry updates) and map it consistently to both `ScreenRotation` (passed to `ArProjection.updateCamera`) and `AVCaptureConnection.videoRotationAngle`:
  - `.portrait` -> `ScreenRotation.rotation0`, `videoRotationAngle = 90`
  - `.landscapeRight` (device rotated 90° counter-clockwise, top on left) -> `ScreenRotation.rotation90`, `videoRotationAngle = 0`
  - `.landscapeLeft` (device rotated 90° clockwise, top on right) -> `ScreenRotation.rotation270`, `videoRotationAngle = 180`
  - `.portraitUpsideDown` -> `ScreenRotation.rotation180`, `videoRotationAngle = 270`
- Include `screenRotation` and the usable viewport geometry in `cachedSlots` invalidation keys so rotating the device immediately recomputes Marker Slots for the new viewport dimensions.
- Offset scripted simulator pose roll in `ARCameraMotion` by `ScreenRotation.rollOffsetRadians` (`0` for `.rotation0`, `-π/2` for `.rotation90`, `+π/2` for `.rotation270`, `π` for `.rotation180`) so simulator landscape verification exercises the exact same `Math3D.convert3dTo2d` projection path as physical hardware.
- In `ARCameraRenderer.updateTexTransform`, apply the 90° UV rotation fallback only when source pixel buffer and drawable aspect orientations disagree (`(sourceWidth > sourceHeight) != (drawableWidth > drawableHeight)`).

#### Adaptive AR Marker sizing, Marker Slot pagination, and tap-to-toggle overlay

- Compute `MarkerSlotGeometry` (`baseY`, `rows`, `markerWidth`, `markerHeight`) from the usable unobscured camera region:
  - `usableHeight = max(0, viewport.height - topInset - bottomInset)`
  - `markerHeight = (usableHeight / 6.0).clamped(to: 72...88)`
  - `rows = max(1, min(6, Int((usableHeight + verticalSpacing) / (markerHeight + verticalSpacing))))`
  - `divisor = Int(usableWidth / 160).clamped(to: 2...5)` on iPhone (extended to `2...6` with a `150...220 pt` width clamp in Phase 3 for iPad)
  - `markerWidth = (usableWidth / CGFloat(divisor)).clamped(to: 150...220)`
  - `baseY = topInset + 12 + markerHeight * 0.5`
- Track `currentPage: Int` (0-based) and `totalPages: Int` (`maxPage + 1`) in `ARCameraModel`:
  - When slots are reassigned, if previously drawn marker IDs now live on a different page, follow the page containing the highest match count; otherwise clamp `currentPage` to `0...maxPage`.
  - When no Active Location or orientation is available, clear placements and set `totalPages = 1` (so no picker shows) while preserving `currentPage` and the last drawn marker IDs, so the next assignment follows the page the user was on and clamps to `0...maxPage`.
  - Filter displayed `placements` and `holeRects` to markers where `slot.page == currentPage`.
- Render a floating **AR Marker Page Picker** at the bottom-trailing corner of the Camera tab when `totalPages > 1`, the toolbars are visible, and the frame is not fully blurred (displaying up to 3 page numbers at a time in 1-based numbering, mirroring Android's `CameraPreviewPagePicker` sliding window and its visibility condition).
- Tapping empty space on the AR camera preview toggles `toolbarsVisible` with a fade animation, hiding or showing the floating bottom controls while keeping the system tab bar visible.
- Switching tabs resets `toolbarsVisible = true`.

### Phase 2: Shared `PlacesCoordinator`, Full-Screen Layout, HIG Places Sheet, and Custom Location (Issues 25 & 26)

#### Shared KMP `PlacesCoordinator` and SKIE-friendly state models

- Add `val addressRepository: AddressRepository` to `IosAppGraph` and wire it into `SightlineShared` alongside direct helper methods `suspend fun reverseGeocode(location: GeoLocation): String?` and `suspend fun searchAddresses(query: String, limit: Int = 100): List<CustomLocation>`.
- Because Objective-C erases type parameters on generic interfaces like `LoadingState<out T : Any>`, define concrete, non-generic sealed types in `shared/src/commonMain/kotlin/com/trm/sightline/shared/` so SKIE bridges them to exhaustive Swift `enum`s with strongly typed payloads:
  - `CategoryPlacesState`: `data object Loading` | `data class Loaded(val places: List<Place>)`
  - `LocationAddressState`: `data class Loading(val previousAddress: String = "")` | `data class Loaded(val address: String)`
  - `CustomLocationSearchState`: `data object Idle` | `data object QueryTooShort` | `data object Loading` | `data class Loaded(val results: List<CustomLocation>)`
- Implement `PlacesCoordinator` in `shared/src/commonMain/kotlin/com/trm/sightline/shared/PlacesCoordinator.kt`:
  - Exposed on `SightlineShared` (`val placesCoordinator: PlacesCoordinator`).
  - Accepts an injectable `CoroutineScope` in its internal constructor for deterministic `runTest` + `advanceTimeBy` unit tests in `shared/commonTest`, defaulting to `CoroutineScope(SupervisorJob() + Dispatchers.Main)` in production.
  - Exposes `StateFlow`s:
    - `places: StateFlow<Map<PlaceCategory, CategoryPlacesState>>` (initially `emptyMap()`)
    - `allPlaces: StateFlow<List<Place>>` (flattened from all `CategoryPlacesState.Loaded` entries)
    - `activeLocation: StateFlow<GeoLocation?>` (`userLocation` when `userLocationEnabled` is true, else `customLocation`)
    - `userLocation: StateFlow<GeoLocation?>`
    - `userLocationAddress: StateFlow<LocationAddressState>`
    - `customLocation: StateFlow<GeoLocation?>`
    - `customLocationAddress: StateFlow<String>`
    - `customLocationSearchResults: StateFlow<CustomLocationSearchState>`
    - `userLocationEnabled: StateFlow<Boolean>`
    - `searchRadius: StateFlow<PlaceSearchRadius>`
    - `lastMapPosition: StateFlow<MapCameraPosition?>`
    - `error: StateFlow<RequestError?>`
  - Encapsulates Android `MainViewModel`'s exact reactive behaviors:
    - Zero active categories on startup (`places = emptyMap()`).
    - 1-second debounce per `PlaceCategory` on `onTogglePlaceCategory(category)`, immediately emitting `CategoryPlacesState.Loading` on toggle-on and removing the category key (and cancelling its in-flight fetch) on toggle-off.
    - 35-second timeout (`withTimeout(35.seconds)`) per category place fetch, sorting fetched places by distance from `activeLocation`, and mapping `TimeoutCancellationException` / network exceptions to `RequestError` (`IO`, `Http`, `Other`).
    - Automatic 4-second clear timer on `error: StateFlow<RequestError?>`.
    - When `activeLocation` becomes `null`, clears `places` so stale markers do not linger.
    - When `activeLocation` or `searchRadius` changes while categories are active, re-fetches all active categories for the new origin/radius.
    - 1-second delay before calling `addressRepository.getAddress(latitude, longitude)` when `userLocation` updates while `userLocationEnabled` is true (`LocationAddressState.Loading(previousAddress)` -> `LocationAddressState.Loaded(address)`).
    - When `locationProvider.isLocationEnabled()` emits `false` while `userLocationEnabled` is `true`, automatically persists `setUserLocationEnabled(false)`.
    - Toggling `"Use my location"` ON (`setUserLocationEnabled(true)`) does **not** clear the persisted `CustomLocation`; toggling it back OFF immediately restores the persisted `CustomLocation` and `customLocationAddress` as the `activeLocation`.
    - Editing the custom location search field (`setCustomLocationAddress(query)`) clears the in-memory `customLocation` to `null` until a search result is picked, validates trimmed query length (`0` -> `Idle`, `1..2` -> `QueryTooShort`, `>= 3` -> `Loading` followed by a 1-second debounced `addressRepository.search(query.trim(), limit = 100)`), and persists a selected `CustomLocation` via `onCustomLocationSearchResultClick(location)`.
    - `cancelCustomLocationSearch()` restores the in-memory `customLocation` and `customLocationAddress` from the persisted `UserPreferencesRepository.getCustomLocation()` value.
    - 1-second debounce on `saveMapPosition(position: MapCameraPosition)`.
- Keep `@MainActor @Observable NearbyPlacesModel` in `iosApp/Sources/NearbyPlacesModel.swift` as a thin SwiftUI adapter that collects `PlacesCoordinator` flows via SKIE `for await`, renames `currentLocation` to `activeLocation`, and forwards user intents.

#### Full-screen `TabView` and floating glass controls

- Replace the top `NavigationStack` header, horizontal category strip, and `PreferencesView` gear sheet on `MainScreen` (and delete `PreferencesView.swift` and `LocationAccessBanner.swift`) with a full-screen bottom `TabView` containing two tabs:
  - **Camera** (`camera.viewfinder` SF Symbol)
  - **Map** (`map` SF Symbol)
- Above the tab bar, render floating controls with `.regularMaterial` capsules (fading out when `!toolbarsVisible` on the Camera tab):
  - **Bottom-leading**:
    - **Search Radius `Menu` pill** (`scope` / radius label showing `500 m`, `1 km`, `2 km`, or `5 km`, opening a native SwiftUI `Menu` with checkmarks).
    - **Places & Location trigger pill** (showing a location status icon, active category count badge or current location summary, and opening the Places & Location `.sheet`).
  - **Bottom-trailing** (Camera tab only, when `totalPages > 1`):
    - **AR Marker Page Picker** pill (`1`, `2`, `3`...).
- At the top of the screen (below the safe area), display a floating transient error banner whenever `NearbyPlacesModel.errorMessage` is non-nil:
  - `RequestError.IO` -> `"A connection error occurred."`
  - `RequestError.Http` -> `"A server error occurred - try again later."`
  - `RequestError.Other` -> `"An unknown error occurred - try again later."`

#### Native system `.sheet` for Places, Location, and Category Drill-down

- Present the Places & Location UI via a native SwiftUI `.sheet(isPresented: $showsPlacesSheet)` hosting its own `NavigationStack(path: $categoryNavigationPath)`.
- Inside the sheet's root view:
  1. **Location Header Row**:
     - When `userLocationEnabled == true`: displays a non-editable rounded status field showing a `ProgressView` with `"Loading location..."` (while `userLocation == nil`), a `ProgressView` with `"Loading location address..."` (while `userLocationAddress` is `.loading`), or a `location.fill` icon with the reverse-geocoded address (or `"Location address not found."` if empty).
     - When `userLocationEnabled == false`: displays an interactive rounded search `TextField` (`"Enter your location."` placeholder, bound to `customLocationAddress`, with a leading `magnifyingglass` icon and an inline clear `xmark.circle.fill` button while editing).
     - Trailing controls: a **"Use my location"** toggle button (`location.fill` when enabled, `location` when disabled, tinted with accent fill when enabled), plus a `"Cancel"` button whenever the custom location search field is focused.
     - If the user taps **"Use my location"** to enable it while `authorization == .denied || authorization == .restricted || !isLocationServicesEnabled`, present a native `.alert` with `"Open Settings"` (`UIApplication.openSettingsURLString`) and `"Cancel"`.
     - If `authorization == .notDetermined`, request location authorization and enable `userLocationEnabled` once authorized.
  2. **Custom Location Search Mode (when `isSearchingCustomLocation && !userLocationEnabled`)**:
     - Focusing the search field expands `sheetDetent` to `.large` and replaces the category grid below the header with the Custom Location search state view:
       - `.idle`: centered prompt `"Start typing to search for a location."`
       - `.queryTooShort`: centered prompt `"Your query must be at least 3 characters long."`
       - `.loading`: centered `ProgressView`
       - `.loaded(let results)`: `List` of `CustomLocation` rows (leading `mappin.and.ellipse` icon + `result.address`, or `"No locations found."` when `results.isEmpty`).
     - Tapping a `CustomLocation` row calls `onCustomLocationSearchResultClick`, dismisses focus, and returns to the 13-category grid.
     - Tapping `"Cancel"` calls `cancelCustomLocationSearch()`, dismisses focus, and returns to the 13-category grid.
  3. **13-Category Adaptive Grid (when not in Custom Location Search Mode)**:
     - Uses `LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: 12)], spacing: 12)` over all 13 `PlaceCategory` entries, followed by the `"© OpenStreetMap contributors"` attribution footer link (`https://www.openstreetmap.org/copyright`).
     - Each category card has two explicit hit zones:
       - **Leading toggle `Button` (44x44 pt minimum hit target)**: displays the category's SF Symbol (or a `ProgressView` while `.loading`), styled with prominent accent fill when active and secondary fill when inactive. Disabled when the category is inactive and `activeLocation == nil`; active categories can always be toggled off.
       - **Main card body**: displays the category title, status subtitle (`"Not selected"`, `"Loading..."`, or `"N places"`), and a trailing `chevron.right` when `.loaded`. Tapping the card body when `.loaded` expands `sheetDetent` to `.large` and pushes `CategoryRoute(category: category)` onto the sheet's `NavigationStack`.
  4. **Upgraded `CategoryPlacesScreen`**:
     - Reads the live `places` for the pushed `PlaceCategory` from `NearbyPlacesModel` (and pops automatically if the category is no longer loaded).
     - Renders a split layout matching Android's `PlaceCategoryScreen`:
       - Top half: a `MapLibreMapView` fitted to the category's places (plus the user location dot when `userLocationEnabled` is true), supporting programmatic camera centering on a selected place (`selectedPlaceCoordinate`, zoom `>= 15`).
       - Category summary header: category icon badge, category title, and `"N places"` count.
       - Bottom half: `List` of `Place` rows (or empty state `"No places found in this category"`), each showing a 2-letter initials avatar badge, place name, formatted distance from `activeLocation` (`"120 m"` / `"1.4 km"`), tap-to-center on the category map, and a trailing button (`arrow.triangle.turn.up.right.diamond.fill`) that opens the place in Apple Maps (`MKMapItem`).

### Phase 3: Native iPadOS Adaptive Layout and 4-Orientation AR Scaling

#### Universal target, 4 orientations, and multitasking

- Set `TARGETED_DEVICE_FAMILY: "1,2"` in `iosApp/project.yml` and regenerate `iosApp/iosApp.xcodeproj` via `xcodegen generate`.
- In `iosApp/Support/Info.plist`:
  - Keep `UISupportedInterfaceOrientations` with the 3 iPhone orientations (`UIInterfaceOrientationPortrait`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`).
  - Add `UISupportedInterfaceOrientations~ipad` with all 4 orientations (`UIInterfaceOrientationPortrait`, `UIInterfaceOrientationPortraitUpsideDown`, `UIInterfaceOrientationLandscapeLeft`, `UIInterfaceOrientationLandscapeRight`).
  - Do not set `UIRequiresFullScreen`, enabling native iPadOS Split View, Slide Over, and Stage Manager resizable windows alongside `UILaunchScreen`.

#### Unified adaptive `.sheet` across compact and regular size classes

- Read `@Environment(\.horizontalSizeClass) private var horizontalSizeClass` in `MainScreen`.
- Reuse the single native `.sheet(isPresented: $showsPlacesSheet)` across iPhone and iPad with one unconditional presentation setup:
  - Apply `.presentationDetents([.medium, .large], selection: $sheetDetent)`, `.presentationDragIndicator(.visible)`, `.presentationBackgroundInteraction(.enabled(upThrough: .medium))`, and a top-trailing **"Done"** toolbar button on the sheet's root `NavigationStack` view once, for both size classes.
  - In `.compact` width (iPhone and narrow iPad Split View / Slide Over windows), the resizable `.medium`/`.large` bottom sheet, drag indicator, and Done button all apply.
  - In `.regular` width (full-screen and wide iPad windows), iPadOS automatically presents the sheet as a centered page sheet over a dimmed backdrop and ignores the detents; the Done button remains the explicit one-tap and pointer dismissal.
- Set `ARCameraModel.sheetObscuresCamera = showsPlacesSheet && (horizontalSizeClass == .regular || verticalSizeClass == .compact || sheetDetent == .large)` so the live AR camera feed blurs whenever the centered iPad page sheet, the `.large` compact sheet, or the iPhone landscape full-screen cover obscures the center of the camera viewport.

#### iPad AR Marker scaling and top-level controls

- In `ARCameraModel.MarkerSlotGeometry`, compute `divisor = Int(usableWidth / 160).clamped(to: 2...6)` and clamp `markerWidth = (usableWidth / CGFloat(divisor)).clamped(to: 150...220)` while keeping `markerHeight` in `72...88 pt` and `rows` in `1...6`.
- Keep the standard 2-tab `TabView` (`Camera` and `Map`) and the bottom-leading floating **Search Radius** `Menu` + **Places & Location** trigger pill and bottom-trailing **AR Marker Page Picker** across both iPhone and iPad.

## Testing Decisions

1. **Android correctness gate**:
   `./gradlew :app:assembleDebug test --console=plain` must remain green with zero changes to Android behavior or UI after every ticket.
2. **Shared KMP unit tests (`:shared:iosSimulatorArm64Test`)**:
   - Extend `ArProjectionTest` / `ArOrientationTrackerTest` for all four `ScreenRotation` values (`.rotation0`, `.rotation90`, `.rotation180`, `.rotation270`), including the four `ScreenRotation.rollOffsetRadians` scripted pose offsets.
   - Add `PlacesCoordinatorTest` using `kotlinx.coroutines.test.runTest` and `advanceTimeBy` covering:
     - Zero initial categories and zero startup network calls, plus Loading on a toggle-on without an Active Location and Loading on a location or radius refetch of an already-loaded category.
     - 1-second debounced multi-category toggling, immediate cancellation on toggle-off, and distance sorting from `Active Location`.
     - 35-second fetch timeout mapping to `RequestError.IO` and 4-second auto-clear of `RequestError`.
     - `Active Location` precedence (`userLocationEnabled ? userLocation : customLocation`), preserving persisted `CustomLocation` across `"Use my location"` toggle switches, and clearing `places` when `activeLocation` becomes `null`.
     - 1-second delayed reverse geocoding of `userLocation` into `LocationAddressState`.
     - 1-second debounced custom location search (`Idle`, `QueryTooShort`, `Loading`, `Loaded`) and `cancelCustomLocationSearch()` restoration.
     - Auto-disabling `userLocationEnabled` when `locationProvider.isLocationEnabled()` emits `false`.
3. **iOS UI & Simulator tests (`iosAppUITests`)**:
   - Update `MapPageUITests` and `SmokeUITests` for the full-screen `TabView`, floating Search Radius `Menu`, Places & Location `.sheet`, independent multi-category toggles (verifying 0 places on launch before toggling a category), `CategoryPlacesScreen` drill-down with interactive map and place rows, Custom Location search flow (when `"Use my location"` is off), and map camera + preference persistence across relaunch.
   - Verify iPad simulator build and layout (`iPad Pro 13-inch (M4)` or available iPad simulator destination) in both portrait and landscape orientations, including the debug HUD marker width (`150...220 pt`) and divisor cap (`6`).

## Sequenced Implementation Tickets

1. [06: iOS AR camera blur, landscape orientation, and slot pagination](issues/06-ios-ar-camera-blur-orientation-pagination.md) (Phase 1 - Issue 24 fixes)
2. [07: Shared PlacesCoordinator and address facade](issues/07-shared-places-coordinator-and-address-facade.md) (Phase 2a - Shared KMP multi-category & custom location state coordinator)
3. [08: iOS full-screen tabs, HIG places sheet, and custom location UI](issues/08-ios-fullscreen-tabs-places-sheet-and-custom-location.md) (Phase 2b - Issues 25 & 26 UI rework + UI smoke tests; blocked by 06, 07)
4. [09: Native iPad adaptive support and 4-orientation AR scaling](issues/09-ipad-adaptive-support-and-ar-scaling.md) (Phase 3 - Universal iPhone/iPad target, adaptive `.sheet`, 4-orientation iPad AR scaling; blocked by 06, 08)

## Out of Scope

- Any changes to Android behavior, dependencies, or UI.
- Compose Multiplatform for shared UI (iOS UI remains 100% native SwiftUI / UIKit / Metal).
- Apple MapKit migration for the embedded maps (MapLibre Native for iOS remains the embedded map engine; Apple Maps is only used as the external handoff target when the user taps the open-in-maps button on a place row).
