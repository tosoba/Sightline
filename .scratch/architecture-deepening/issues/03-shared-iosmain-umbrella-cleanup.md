# 03: Convert :shared into an explicit iosMain umbrella module and remove dead legacy facades

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move the remaining iOS-only umbrella code (`SightlineShared.kt`, `IosAppGraph.kt`) and tests (`SightlineSharedTest.kt`) in `:shared` from `commonMain`/`commonTest` into `iosMain`/`iosTest`, delete the dead `PreferencesFacade` class and unused legacy pass-through methods on `SightlineShared`, and remove the unnecessary `expect`/`actual` split on `userPreferencesDirectory()`.

**Blocked by:** 01, 02

**Status:** ready-for-agent

## Acceptance Criteria

- [ ] **Remove dead legacy facade & pass-throughs**:
  - Delete `shared/src/commonMain/kotlin/com/trm/sightline/shared/PreferencesFacade.kt` and `shared/src/commonTest/kotlin/com/trm/sightline/shared/PreferencesFacadeTest.kt`.
  - Remove the unused methods/properties from `SightlineShared`: `preferences`, `categories`, `nearbyPlaces`, `reverseGeocode`, `searchAddresses`, and `locationUpdates`, keeping `placesCoordinator: PlacesCoordinator` and `locationProvider: LocationProvider` (and its internal constructor for testing).
- [ ] **Move `:shared` sources and tests to `iosMain` and `iosTest`**:
  - Move `SightlineShared.kt` and `IosAppGraph.kt` from `shared/src/commonMain/kotlin/com/trm/sightline/shared/` to `shared/src/iosMain/kotlin/com/trm/sightline/shared/`.
  - Remove `expect internal fun userPreferencesDirectory(): String` from `SightlineShared.kt` and make `internal fun userPreferencesDirectory(): String` in `shared/src/iosMain/kotlin/com/trm/sightline/shared/UserPreferencesDirectory.ios.kt` a plain function.
  - Move `SightlineSharedTest.kt` from `shared/src/commonTest/kotlin/com/trm/sightline/shared/` to `shared/src/iosTest/kotlin/com/trm/sightline/shared/` and update it to test `SightlineShared`'s active `placesCoordinator` surface.
  - Verify `shared/src/commonMain` and `shared/src/commonTest` no longer exist, making it unambiguous that `:shared` is the iOS-only umbrella module.
  - In `shared/build.gradle.kts`, configure dependencies on `iosMain.dependencies` and `iosTest.dependencies`.

## Verification Commands

```sh
./gradlew :shared:iosSimulatorArm64Test :shared:assembleSharedXCFramework --console=plain
./gradlew :app:assembleDebug test --console=plain
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```
