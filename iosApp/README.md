# iosApp

SwiftUI host for the shared Kotlin Multiplatform stack.
The `shared` Gradle module builds a static `Shared.xcframework`; this app consumes it through SKIE
(Kotlin `suspend` functions become Swift `async`, Kotlin `Flow` becomes Swift `AsyncSequence`).

## Layout

- `project.yml` - XcodeGen manifest; `iosApp.xcodeproj` is generated from it and committed.
- `Sources/` - SwiftUI app, root pager, MapLibre map wrapper, and view model.
- `Resources/` - MapLibre `light_style.json` and `dark_style.json`, copied from `core/ui/src/main/res/raw` by `./gradlew :syncIosMapStyles`.
- `Support/Info.plist` - app plist with `NSLocationWhenInUseUsageDescription`.
- `UITests/` - UI test that switches to the Map page and captures a screenshot.

## Build

```sh
./gradlew :shared:assembleSharedXCFramework
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The `Build Shared framework` run script phase invokes
`./gradlew :syncIosMapStyles :shared:embedAndSignAppleFrameworkForXcode` and needs `ENABLE_USER_SCRIPT_SANDBOXING: NO`.
The static framework is found through `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
MapLibre Native is resolved through SPM (`exactVersion: 7.0.0`) and embedded as a dynamic framework.

## Tests

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' test
```

The UI tests cover the full smoke pass: the camera page, the map with places, the category push and back,
the location fix, and preferences surviving a relaunch.
They reset and answer the system location prompt themselves, because reinstalling the app resets a simulator grant.
Pre-granting and fixing the location still helps scripted runs:

```sh
xcrun simctl privacy booted grant location com.trm.sightline
xcrun simctl location booted set 50.0614,19.9366   # Krakow
```

`-mapDebug` shows the persisted camera under the map (`lat,lon,zoom,bearing,pitch`); the smoke test uses it to
assert that a changed camera survives a relaunch.

## Preferences

The gear button opens a preferences sheet backed by the shared `PreferencesFacade`: search radius and the
user-location toggle.
Values, including the last map camera, are stored in the shared DataStore file at
`<app container>/Library/Application Support/Sightline/user_preferences.json`, the same path the Kotlin
`OkioStorage` writes on iOS.

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
