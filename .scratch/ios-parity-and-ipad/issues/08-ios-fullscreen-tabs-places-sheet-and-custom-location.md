# 08: iOS full-screen tabs, HIG places sheet, and custom location UI

**Spec:** `.scratch/ios-parity-and-ipad/spec.md` (Phase 2), `.scratch/ios-parity-and-ipad/issues/02-fullscreen-layout-and-hig-places-sheet.md`, `.scratch/ios-parity-and-ipad/issues/03-shared-facade-and-custom-location-flow.md`

**What to build:** Replace the top header, horizontal category strip, and `PreferencesView` on `MainScreen` with a full-screen bottom `TabView` (`Camera` and `Map`), floating bottom-leading Search Radius `Menu` and Places & Location trigger pills (paired with the bottom-trailing AR Marker Page Picker from Ticket 06), a top floating transient error banner, a native system `.sheet` (`[.medium, .large]`) housing the Location Header Row, inline Custom Location search, and 13-category adaptive grid with separate toggle and drill-down zones, and an interactive map+list `CategoryPlacesScreen` with Apple Maps handoff, covered by updated iOS UI smoke tests.

**Blocked by:** 06, 07

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **Full-screen `TabView` & floating controls**:
  - `MainScreen` uses a bottom `TabView` with **Camera** (`camera.viewfinder`) and **Map** (`map`) tabs, retiring the top navigation bar, horizontal category strip, `PreferencesView.swift`, and `LocationAccessBanner.swift`.
  - Floating bottom-leading controls render a **Search Radius `Menu` pill** (`500 m`, `1 km`, `2 km`, `5 km` with active checkmark) and a **Places & Location trigger pill** (opening the Places `.sheet`), opposite the bottom-trailing **AR Marker Page Picker** on the Camera tab.
  - Tapping empty space on the AR camera preview toggles `toolbarsVisible` (fading the floating controls in/out), and switching tabs resets `toolbarsVisible = true`.
  - A top floating glass error banner displays `NearbyPlacesModel.errorMessage` (`"A connection error occurred."`, `"A server error occurred - try again later."`, `"An unknown error occurred - try again later."`) and disappears automatically when cleared after 4 seconds.
- [ ] **Places & Location `.sheet` with Location Header Row & Custom Location Search**:
  - Presented with `.presentationDetents([.medium, .large], selection: $sheetDetent)`, `.presentationDragIndicator(.visible)`, and `.presentationBackgroundInteraction(.enabled(upThrough: .medium))`, updating `ARCameraModel.sheetObscuresCamera` when at `.large`.
  - **Location Header Row**:
    - When `userLocationEnabled == true`: shows `"Loading location..."`, `"Loading location address..."`, or the reverse-geocoded address (`"Location address not found."` fallback) alongside the highlighted **"Use my location"** toggle button (`location.fill`).
    - When `userLocationEnabled == false`: shows an interactive rounded search field (`"Enter your location."`, pre-filled with `customLocationAddress`, clear button while editing, and trailing `"Cancel"` button while focused) alongside the inactive **"Use my location"** toggle button (`location`).
    - Attempting to enable `"Use my location"` when location permission is denied/restricted or location services are off presents a native `.alert` with `"Open Settings"` (`UIApplication.openSettingsURLString`) and `"Cancel"`.
  - **Custom Location Search Mode**:
    - Focusing the search field expands `sheetDetent` to `.large` and shows the search state view (`"Start typing to search for a location."` for `.idle`, `"Your query must be at least 3 characters long."` for `.queryTooShort`, a `ProgressView` for `.loading`, and selectable `CustomLocation` rows for `.loaded`).
    - Selecting a `CustomLocation` row persists it via `onCustomLocationSearchResultClick`, dismisses keyboard focus, and returns to the 13-category grid; tapping `"Cancel"` calls `cancelCustomLocationSearch()` and restores the previous saved `CustomLocation`.
- [ ] **13-Category Adaptive Grid & `CategoryPlacesScreen` drill-down**:
  - 2+ column `LazyVGrid` (`GridItem(.adaptive(minimum: 160), spacing: 12)`) showing all 13 `PlaceCategory` cards and the `"© OpenStreetMap contributors"` attribution footer link.
  - Each category card separates its leading 44x44 pt toggle `Button` (SF Symbol or `ProgressView` while `.loading`, disabled when inactive and `activeLocation == nil`) from its main card body (title, `"Not selected"` / `"Loading..."` / `"N places"`, and trailing `chevron.right` when `.loaded`).
  - Tapping a `.loaded` card body expands `sheetDetent` to `.large` and pushes `CategoryPlacesScreen` onto the sheet's `NavigationStack`.
  - `CategoryPlacesScreen` renders a top `MapLibreMapView` fitted to the category's places (centering on a place when its row is tapped, zoom `>= 15`), a category summary header (icon badge, title, `"N places"` count), and interactive place rows (2-letter initials badge, place name, formatted distance from `activeLocation`, and a trailing button opening the place in Apple Maps via `MKMapItem`).
- [ ] **Updated iOS UI Smoke Suite (`iosAppUITests`)**:
  - `MapPageUITests` and `SmokeUITests` pass from a clean `-resetPreferences` launch, verifying:
    1. Zero places fetched on launch before any category is toggled.
    2. Custom Location search flow when `"Use my location"` is off (query validation, picking a result, toggling a category on, and verifying places load around the Custom Location).
    3. Enabling `"Use my location"`, changing Search Radius to `2 km`, toggling a Place Category on, and drilling into `CategoryPlacesScreen` (verifying map + place rows and back navigation).
    4. Map camera and preference persistence across app termination and relaunch.
    5. Location permission denial presenting the Settings alert when tapping `"Use my location"`.

## Verification Commands

```sh
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
./gradlew :app:assembleDebug test --console=plain
cd iosApp && xcodegen generate && cd ..
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
```
