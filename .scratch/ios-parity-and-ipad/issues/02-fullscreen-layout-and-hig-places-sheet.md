Type: grilling
Status: resolved

## Question

Lock the Apple HIG-aligned design for the full-screen canvas, floating controls, and multi-category places & location sheet on iPhone (Portrait & Landscape):

1. **Full-screen Camera & Map canvas + Floating Controls**:
   - Replace the top `NavigationStack` header/toolbar on `MainScreen` so the AR Camera and Map stretch edge-to-edge across the entire screen.
   - Design the HIG-idiomatic floating controls for switching between **Camera** and **Map**, changing **Search Radius** (`500 m`, `1 km`, `2 km`, `5 km`), and paging AR markers (if enabled in [AR camera blur and landscape orientation](01-ar-camera-blur-and-landscape-orientation.md)), including their placement relative to safe areas and the collapsed sheet.

2. **HIG-aligned Persistent Places & Location Sheet (reworking Issue 25)**:
   - On Android, a `BottomSheetScaffold` (or trailing side sheet in landscape `isCompactHeight`) stays present over the Camera/Map with:
     - Top bar: Location display / Custom location search field + "Use my location" toggle button.
     - Collapsed state (`Row`): 4 primary categories (`Sights & Landmarks`, `Food & Drink`, `Accommodation`, `Shopping`) as quick toggles.
     - Expanded state (`Grid`): All 13 categories where each item has a toggle action (to start/stop fetching that category, showing a spinner while loading and `N places` when loaded) and a tap/drill-down action when loaded to push `CategoryPlacesScreen`.
   - Evaluate where Android's Material 3 dual-action card (toggle button nested inside a clickable card) or non-modal bottom sheet needs adaptation to feel native to iOS HIG (e.g., iOS 16+ non-modal `.sheet` detents with `.presentationBackgroundInteraction(.enabled(upThrough:))` vs. a custom glass overlay sheet so pushing `CategoryPlacesScreen` onto `NavigationStack` works cleanly without sheet dismissal conflicts; clear HIG affordance separating category toggle vs. opening the category's place list).

3. **Startup & Category State Lifecycle**:
   - Confirm that no categories are selected on app launch (zero automatic network requests on startup), multiple categories can be toggled independently, and toggling off a category immediately removes its markers from AR and Map.

## Answer

1. **Full-screen Camera & Map `TabView` and Floating Controls**:
   - Replace the top `NavigationStack` header, horizontal category strip, and `PreferencesView` gear sheet on `MainScreen` with a standard bottom `TabView` (`Camera` and `Map` tabs) where each tab's canvas stretches edge-to-edge across the screen.
   - Above the tab bar, show floating glass controls (hidden with a fade animation when `!toolbarsVisible` on tap of empty AR camera space, and reset to `true` on tab switch):
     - **Bottom-leading**: A floating **Search Radius** `Menu` pill (`500 m`, `1 km`, `2 km`, `5 km` with a checkmark on the active radius) paired with a floating **Places & Location** trigger pill/button (showing current location summary / active category count) that opens the Places system `.sheet`.
     - **Bottom-trailing** (on the Camera tab when `totalPages > 1`): The floating **AR Marker Page Picker** (`1`, `2`, `3`...).

2. **Native System `.sheet` for Places, Location, and Category Drill-down**:
   - Present the Places & Location UI via a dismissible native system `.sheet` with `.presentationDetents([.medium, .large], selection: $sheetDetent)`, `.presentationDragIndicator(.visible)`, and `.presentationBackgroundInteraction(.enabled(upThrough: .medium))`.
   - Drive `ARCameraModel.sheetObscuresCamera` from the sheet presentation state (blurring the camera feed when obscured by the sheet at `.large`).
   - Wrap the sheet content in its own `NavigationStack`:
     - Root view shows the location bar ("Use my location" toggle button + active location address / custom location search trigger), the 13-category adaptive grid (2+ columns, `GridItem(.adaptive(minimum: 160))`), and the OpenStreetMap attribution footer.
     - Each category card in the grid has two explicit zones: a leading square toggle `Button` (showing the category SF Symbol or a `ProgressView` spinner while `Loading`, tinted with accent fill when active) to toggle fetching on/off, and the main card body (category title, `N places` subtitle, and trailing `chevron.right` when `Loaded`) that pushes `CategoryPlacesScreen` onto the sheet's `NavigationStack` and expands `sheetDetent` to `.large`.
   - Upgrade `CategoryPlacesScreen` to match Android's category detail screen: a Map fitted to the category's places, a category header (icon badge, title, `N places` count), and interactive place rows (initials badge, name, formatted distance from active location, tap-to-center on the category map, and a trailing button to open the place in Apple Maps).

3. **Startup & Category State Lifecycle**:
   - Start with zero active categories on launch (`activeCategories` empty, zero automatic network requests on startup).
   - Support independent multi-category toggling (`Map<PlaceCategory, LoadingState<[Place]>>`), immediately cancelling in-flight requests and removing a category's places from AR and Map when toggled off.
   - Disable unselected category toggles when neither a live `UserLocation` (with `userLocationEnabled == true`) nor a `CustomLocation` (with `userLocationEnabled == false`) is available; already-selected categories remain toggleable off.
   - Surface transient fetch errors via a top floating glass error banner, and show a native `.alert` offering "Open Settings" and "Cancel" when the user attempts to enable "Use my location" while location permission or location services are denied/disabled.
