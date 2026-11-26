# iosApp

SwiftUI host for the shared Kotlin Multiplatform stack.
The `shared` Gradle module builds a static `Shared.xcframework`; this app consumes it through SKIE (Kotlin `suspend` functions become Swift `async`, Kotlin `Flow` becomes Swift `AsyncSequence`).

## Layout

- `project.yml` - XcodeGen manifest; `iosApp.xcodeproj` is generated from it and committed.
- `Sources/` - the SwiftUI app: the root pager and screens, the MapLibre wrapper, the nearby-places model, and the AR camera page with its capture session, Metal renderer and shaders (`ARCameraShaders.metal`), Core Motion feed, and marker overlay.
- `Resources/` - MapLibre `light_style.json` and `dark_style.json`, copied from `core/ui/src/main/res/raw` by `./gradlew :syncIosMapStyles`.
- `Support/Info.plist` - app plist with the `NSLocationWhenInUseUsageDescription` and `NSCameraUsageDescription` purpose strings.
- `UITests/` - the full smoke suite (camera, map, category push and back, location, preferences across a relaunch), the map page test, the location denial test, and their shared launch, toggle, and prompt helpers.

## Build

```sh
./gradlew :shared:assembleSharedXCFramework
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The `Build Shared framework` run script phase invokes `./gradlew :syncIosMapStyles :shared:embedAndSignAppleFrameworkForXcode` and needs `ENABLE_USER_SCRIPT_SANDBOXING: NO`.
The static framework is found through `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
MapLibre Native is resolved through SPM (`exactVersion: 7.0.0`) and embedded as a dynamic framework.

## Tests

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
```

The UI tests cover the full smoke pass: the camera page, the map with places, the category push and back, the location fix, and preferences surviving a relaunch.
Every test launches with `-resetPreferences`, which resets the shared preferences through the Kotlin `UserPreferencesRepository` before the first frame, so no test depends on state left by an earlier run.
The tests reset and answer the system location prompt themselves, because reinstalling the app resets a simulator grant.
Pre-granting and fixing the location still helps scripted runs:

```sh
xcrun simctl privacy booted grant location com.trm.sightline
xcrun simctl location booted set 50.0614,19.9366   # Krakow
```

`-mapDebug` shows the persisted camera under the map (`lat,lon,zoom,bearing,pitch`); the smoke test uses it to assert that a changed camera survives a relaunch.

### Test-only launch arguments and identifiers

These exist for the tests and for scripted simulator runs; a normal launch passes none of them.

- `-resetPreferences` - before the first frame, calls `SightlineShared.resetPreferencesForTesting()`, which writes `UserPreferences()` through the same `UserPreferencesRepository` the app uses.
  Without the argument the reset path is inert.
- `user-location-toggle` - accessibility identifier on the preferences `Toggle`.
  The tests tap the knob at the trailing edge because tapping a `Form` row label does not move the switch.

## Preferences

The gear button opens a preferences sheet backed by the shared `PlacesCoordinator` exposed by `SightlineShared`: search radius and the user-location toggle.
The map's user-location layer follows the toggle, so a clean install does not request location permission until the user enables it; Android gates location collection on the same preference.
Values, including the last map camera, are stored in the shared DataStore file at `<app container>/Library/Application Support/Sightline/user_preferences.json`, the same path the Kotlin `OkioStorage` writes on iOS.

## AR camera debug launch arguments

The camera page reads its options from the process arguments in `ARCameraLaunchOptions`, so the same flags work on a device and on the simulator.
Without any of them the page uses the live camera, Core Motion, the default marker hole fill, and no HUD.

- `-arCamera` - opens the app on the camera page instead of the pager's own default.
- `-arSynthetic` - draws the generated test texture and the two-place Krakow fixture instead of the camera and the real nearby places.
- `-arAttitude <pose>` - replaces Core Motion with a scripted `CMAttitude` pose: `uprightNorth`, `uprightLeft`, `uprightRight`, `tilted`, `flat`, or `sweep`.
  The simulator has no magnetometer, so the scripted poses are how alignment is checked there.
- `-arBlur <strength>` - freezes the blur ramp at a strength in `[0, 1]`, which makes a single blur step reproducible for screenshots.
- `-arHoleFill <style>` - picks how the marker holes are filled: `frosted` (the default, matching the Android GL draw order) or `sharp`.
- `-arForceBlur` - forces full-frame blur during normal viewing, as if the device were tilted past the pitch limit.
- `-arNoBlur` - forces full-frame blur off, including the pitch and sheet triggers.
- `-arDebug` - shows the HUD: feed state, orientation, layout metrics, lod, frame timings, marker count, and the forced-blur toggle.
- `-arUnavailable <reason>` - injects a camera failure the simulator cannot trigger: `permission` or `configuration`.

## Run on a simulator

```sh
xcrun simctl boot "iPhone 17"
xcrun simctl install booted <DerivedData>/Build/Products/Debug-iphonesimulator/Sightline.app
xcrun simctl privacy booted grant location com.trm.sightline
xcrun simctl location booted set 50.0614,19.9366   # Krakow
xcrun simctl launch --console-pty booted com.trm.sightline
```

The app logs fetched place counts to stdout and to the `com.trm.sightline` log subsystem.

## Regenerate the Xcode project

```sh
cd iosApp && xcodegen generate
```

Treat `iosApp.xcodeproj` as generated: change `project.yml`, not the project file.
