# 25: iOS preferences, category navigation, and full smoke gate

**Spec:** `.scratch/kmp-ios-migration/specs/persistence.md`, `.scratch/kmp-ios-migration/specs/navigation.md`

**What to build:** An iOS user's preferences persist across launches, the category screen pushes/pops through a SwiftUI `NavigationStack`, and a build-and-run smoke pass confirms camera, map, category, location, and preferences all work on a simulator or device.

**Blocked by:** 22, 23, 24

**Status:** resolved

- [x] Preferences persist across launches via the shared `UserPreferencesLocalRepository` and `OkioStorage`.
- [x] The category screen pushes and pops with standard back navigation carrying `category` and `places`.
- [x] The iOS app passes a full build-and-run smoke pass covering camera, map, category, location, and preferences.

## Comments

### Shared facade

`PreferencesFacade` gained only the map camera pair:

```kotlin
val lastMapPosition: Flow<MapCameraPosition?>
suspend fun setLastMapPosition(position: MapCameraPosition)
```

They delegate to the existing `UserPreferencesRepository`, so the iOS path is the same
`UserPreferencesLocalRepository` -> multiplatform DataStore -> `OkioStorage` chain that ticket 17 built.
No second Metro graph, no Swift-side storage, and no second persistence mechanism was added.
`shared/src/commonTest/.../PreferencesFacadeTest.kt` covers the camera and toggle round trips with an
in-memory repository; the shared simulator target now runs 29 tests.

### Swift wiring

Added:

- `iosApp/Sources/CategoryRoute.swift` - the typed `Hashable` route carrying `category` and `places`, plus the
  pushed screen that reuses `NearbyPlacesView` as content.
- `iosApp/Sources/PreferencesView.swift` - a medium sheet with a `PlaceSearchRadius` segmented picker and the
  user-location toggle; both bindings call back into `NearbyPlacesModel`, which writes through the facade.
- `iosApp/Sources/LocationAccessBanner.swift` - the "location access is off" row and the
  `UIApplication.openSettingsURLString` action shared by the denied and off states.
- `iosApp/UITests/SmokeUITests.swift` - the full smoke pass and the denial test.

Changed:

- `MainScreen` owns the single root `NavigationStack(path:)`; tapping a category chip selects it, awaits the
  places fetch, and appends `CategoryRoute(category:places:)` to the path.
  Standard back and swipe back pop it, and the selected category survives because the model owns the selection.
  The category chips replace the old menu picker: one tap both selects and opens, which matches the ticket's
  "tapping a category pushes" interaction.
  The toolbar gear opens the preferences sheet; the gear is the only new navigation-bar item.
  The sheet owns a modal `NavigationStack` only for its Done button and has no path, so the app's single
  navigation graph is still the root stack.
  The single observation `.task` now runs `observeAuthorization` and `observePreferences`; there is no second
  model owner or second observation task.
- `NearbyPlacesModel` observes the three preference flows.
  Location collection now starts and stops with `userLocationEnabled`, matching Android's gating; turning it
  off clears the fix, places and error state.
  The places fetch uses `Float(searchRadius.meters)` instead of the hardcoded 1000.
  The last map camera is debounced by one second like Android's `MainViewModel`, then written through the facade.
- `NearbyPlacesView` is content-only: it takes `[Place]` and renders the list or the empty state, with no model,
  picker or navigation.
- `MapLibreMapView` restores the persisted camera once per map instance, logs the restore, and reports
  `regionDidChangeAnimated` as a `MapCameraPosition`.
  The first automatic bounds fit is skipped when a restored camera exists so the fetch does not overwrite the
  restore; later category changes fit again as before.
  Styles, annotations, callouts, padding and the `:syncIosMapStyles` pre-build step are unchanged.
- `MapPageUITests` is rewritten from a fixed `sleep(12)` into condition waits on the map's accessibility value.
- `iosApp/README.md` documents the preferences path and the smoke-test commands.

The app's user-location default is still the shared `false`, exactly like Android's fresh install.
The root banner therefore offers "Turn on" until the user enables it, and the category row pushes an empty
list if location is off; this is Android's behaviour with no custom location set.
There is no custom-location picker on iOS yet, so with the toggle off there is no alternative location source.

### Map camera debug affordance

`-mapDebug` shows a small monospaced HUD under the map with `lat,lon,zoom,bearing,pitch` and the accessibility
identifier `map-camera`.
It mirrors the existing `-arDebug` pattern and is the seam the UI test uses to assert that a changed camera is
restored after a relaunch, because the map itself is not introspectable.
The logs also record the restored and saved camera through the `com.trm.sightline` `map` and `places` categories.

### Persistence storage

The shared facade writes to `<app container>/Library/Application Support/Sightline/user_preferences.json`.
On the simulator that resolves through:

```sh
xcrun simctl get_app_container booted com.trm.sightline data
```

A run of the smoke test leaves, for example:

```json
{"userLocationEnabled":true,"lastMapLatitude":50.06149049576846,"lastMapLongitude":19.93658783933722,"lastMapZoom":15.369722443266948,"lastMapBearing":332.77474965359943,"lastMapPitch":0.0,"searchRadiusMeters":1000}
```

### What the UI tests cover

`SmokeUITests.testFullSmokePass`:

1. Launches on the AR camera page and asserts the synthetic feed badge (simulators have no camera).
2. Opens Preferences, enables "Use my location" (answering the system prompt), and selects a 2 km radius.
3. Switches to Map, waits for the map accessibility value to report a non-zero place count, and captures it.
4. Taps the Sights category, asserts the pushed navigation bar and place rows, then taps back and asserts the
   pager and selected category are still there.
5. Pinches the map, waits for the debounced camera HUD value to settle, terminates and relaunches the app,
   and asserts the restored HUD value equals the saved one.
6. Asserts the 2 km radius and the toggle survived the relaunch, then resets the radius to 1 km.

`SmokeUITests.testLocationDenialOffersSettings` resets and denies the location permission, then asserts the
root banner's `Settings` button exists.
`MapPageUITests.testMapPageRendersPlaces` keeps the focused map screenshot test, now waiting for a real place
count instead of sleeping.

The tests call `resetAuthorizationStatus(for: .location)` and answer the SpringBoard prompt, because
`xcodebuild test` reinstalls the app and the simulator prompt otherwise auto-dismisses as "Don't Allow".

### Verification

```text
$ ./gradlew :app:assembleDebug test --console=plain
BUILD SUCCESSFUL in 11s
283 actionable tasks: 283 executed

$ ./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
BUILD SUCCESSFUL in 1m 59s
iosSimulatorArm64Test...ArBlurAnimatorTest tests="8" failures="0"
iosSimulatorArm64Test...ArOrientationTrackerTest tests="11" failures="0"
iosSimulatorArm64Test...ArProjectionTest tests="4" failures="0"
iosSimulatorArm64Test...PreferencesFacadeTest tests="2" failures="0"
iosSimulatorArm64Test...SightlineSharedTest tests="4" failures="0"

$ cd iosApp && xcodegen generate
# no diff in iosApp.xcodeproj after the generated project was committed

$ xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 17' build
** BUILD SUCCEEDED **

$ xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 17' test
Test Case '-[iosAppUITests.MapPageUITests testMapPageRendersPlaces]' passed
SIGHTLINE_TEST map camera fitted=50.0616,19.9365,17.56,252.1,0.0 saved=50.0616,19.9365,18.29,224.9,0.0
Test Case '-[iosAppUITests.SmokeUITests testFullSmokePass]' passed
Test Case '-[iosAppUITests.SmokeUITests testLocationDenialOffersSettings]' passed
Executed 3 tests, with 0 failures
** TEST SUCCEEDED **
```

The relaunch proof appears in the unified log:

```text
22:49:13  [com.trm.sightline:places] saved map camera 50.061490, 19.936588, zoom 15.369722
22:49:24  [com.trm.sightline:map] restored map camera 50.061490, 19.936588, zoom 15.369722, bearing 332.774750, pitch 0.000000
```

Location and radius logs from the same pass:

```text
[com.trm.sightline:places] user location enabled: true
[com.trm.sightline:places] location fix 50.061400, 19.936600
[com.trm.sightline:places] fetched 50 places for SightsLandmarks within 1000 m
[com.trm.sightline:places] fetched 50 places for SightsLandmarks within 2000 m
```

Screenshots in `screenshots/`:

- `ios_smoke_camera.png` - camera page on the simulator's synthetic feed.
- `ios_smoke_map.png` - map page with the fetched places and the user-location dot.
- `ios_smoke_category.png` - the pushed Sights destination with real place rows.
- `ios_preferences_restored.png` - the preferences sheet after a relaunch, showing 2 km still selected.
- `ios_map_restored.png` - the map after the relaunch, with the debug HUD showing the restored camera.
- `ios_map_panned_debug.png` - the changed camera before the relaunch.
- `ios_location_denied.png` - the denied banner with the Settings deep link.

### Reproduce

```sh
# shared facade + tests
./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain

# Android stays green
./gradlew :app:assembleDebug test --console=plain

# project file is generated
cd iosApp && xcodegen generate && cd ..

# the tests reset the permission themselves; this is belt-and-suspenders
xcrun simctl boot "iPhone 17"
xcrun simctl privacy booted grant location com.trm.sightline
xcrun simctl location booted set 50.0614,19.9366

xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
```

The simulator used for this run was an iPhone 17 on iOS 27.2 with Xcode 27.2.

### Device-only

- The live camera feed and Core Motion alignment remain ticket 24's device-only items.
- The permission prompt copy and Settings round trip use the same APIs on device; only the simulator prompt
  handling is exercised here.
- Map camera restore across a true process kill is covered by `terminate`/`launch`; an OS-initiated eviction
  uses the same DataStore read and adds no new path.

### Smoke reproducibility follow-up (ticket 25b)

The original gate only passed when an earlier run had left the toggle ON.
It is now reproducible from a clean install.

The user-location `Toggle` carries the `user-location-toggle` accessibility identifier.
The tests tap the knob at the trailing edge instead of the `Form` row label, and they wait for
`value == "1"` before continuing, so a missed tap fails instead of silently skipping the flow.

Every UI test launches with `-resetPreferences`.
`SightlineApp` waits for `SightlineShared.resetPreferencesForTesting()` before it builds `MainScreen`.
That calls `UserPreferencesRepository.reset()`, which writes `UserPreferences()` through the same
multiplatform DataStore the app uses; there is no Swift-side file surgery.
Without the launch argument the reset path is inert.
The smoke test asserts that the toggle is `0` and the radius is `1 km` before it changes anything.
The argument and the identifier are documented in `iosApp/README.md`.

`MLNMapView` overwrites `accessibilityValue` with `Zoom 1x`, which is why the old
`value != "0 places"` predicate was tautological and passed against an empty map.
The map now reports the count in `accessibilityLabel` as `Nearby places map, N places`, which MapLibre
does not overwrite.
`MapPageUITests` waits for `Nearby places map, [1-9][0-9]* places?`, asserts the parsed count is greater
than zero, and asserts that neither the app nor SpringBoard has an alert on screen before and after the
count assertion.

`MapLibreMapView` starts with `showsUserLocation = false` and applies `model.userLocationEnabled` from
`updateUIView`, so a clean install does not request location until the user enables the toggle.
The UI tests still grant permission and use a fix after they turn the toggle on.

Reproducing the clean run exposed a separate iOS defect: `CLLocationManager.delegate` is weak, and
`IosLocationProvider` assigned an anonymous delegate without retaining it.
The delegate could be collected, so the denial callback and location fixes never arrived.
The provider now owns the delegate in a property, which makes the denial banner and the place count
deterministic.

Clean-install evidence (app uninstalled, location privacy reset, no simulator location scenario):

```text
$ xcrun simctl uninstall <device> com.trm.sightline
$ xcrun simctl privacy <device> reset location com.trm.sightline
$ xcrun simctl location <device> clear
$ xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
    -destination 'platform=iOS Simulator,id=<device>' test
Test Case '-[iosAppUITests.SmokeUITests testLocationDenialOffersSettings]' passed
Test Suite 'SmokeUITests' passed
	 Executed 2 tests, with 0 failures (0 unexpected) in 74.135 (74.138) seconds
Test Suite 'All tests' passed
	 Executed 3 tests, with 0 failures (0 unexpected) in 98.067 (98.074) seconds
** TEST SUCCEEDED **
```

A second run immediately afterwards also passed with 3 tests and 0 failures.
Before that second run the persisted state was deliberately left with `userLocationEnabled: true`,
which proves that a previous ON state no longer changes the result:

```json
{"userLocationEnabled":true,"customLocationLatitude":null,"customLocationLongitude":null,"customLocationAddress":null,"customLocationTimestampMillis":null,"lastMapLatitude":null,"lastMapLongitude":null,"lastMapZoom":null,"lastMapBearing":null,"lastMapPitch":null,"searchRadiusMeters":null}
```

The map test was proven to fail when the count does not meet the assertion.
With the predicate temporarily raised to `Nearby places map, 9[0-9]{3,} places?`, the run failed at the
map count with `its label is Nearby places map, 50 places`.
The predicate was restored and the test passed again.

```text
$ ./gradlew :app:assembleDebug test --console=plain
BUILD SUCCESSFUL in 1s

$ ./gradlew :shared:assembleSharedXCFramework :shared:iosSimulatorArm64Test --console=plain
BUILD SUCCESSFUL in 1m 53s
iosSimulatorArm64Test...SightlineSharedTest tests="5" failures="0"

$ cd iosApp && xcodegen generate
# no diff after a second generate
```

### Review fix: location services disabled state

The root banner gained the third state the shared location API already supported: when location services are off while "Use my location" is on, `MainScreen` shows the same `LocationAccessBanner` with the Settings deep link instead of staying silent.
`NearbyPlacesModel.refreshLocationServices()` starts the shared `locationEnabled` observation on launch and restarts it on every `scenePhase == .active`, because `CLLocationManager.locationServicesEnabled()` has no change notification; restarting the collection is the re-check.
`IosLocationProvider` now caches the reading in a `StateFlow` and refreshes it from the delegate's `didFailWithError` when the error is `CLError.denied`, so a running observation also sees the change without a foreground round trip.
The simulator always reports location services as enabled, so this branch is device-only; the three UI tests are unaffected and still pass.
