# iosApp

SwiftUI host for the shared Kotlin Multiplatform stack.
The `shared` Gradle module builds a static `Shared.xcframework`; this app consumes it through SKIE
(Kotlin `suspend` functions become Swift `async`, Kotlin `Flow` becomes Swift `AsyncSequence`).

## Layout

- `project.yml` - XcodeGen manifest; `iosApp.xcodeproj` is generated from it and committed.
- `Sources/` - SwiftUI app, view model, and category titles.
- `Support/Info.plist` - app plist with `NSLocationWhenInUseUsageDescription`.

## Build

```sh
./gradlew :shared:assembleSharedXCFramework
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The `Build Shared framework` run script phase invokes
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` and needs `ENABLE_USER_SCRIPT_SANDBOXING: NO`.
The static framework is found through `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

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
