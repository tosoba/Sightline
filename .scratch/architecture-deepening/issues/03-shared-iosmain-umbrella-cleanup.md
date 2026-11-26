# 03: Convert :shared into an explicit iosMain umbrella module and remove dead legacy facades

**Spec:** `.scratch/architecture-deepening/spec.md`

**What to build:** Move the remaining iOS-only umbrella code (`SightlineShared.kt`, `IosAppGraph.kt`) and tests (`SightlineSharedTest.kt`) in `:shared` from `commonMain`/`commonTest` into `iosMain`/`iosTest`, delete the dead `PreferencesFacade` class and unused legacy pass-through methods on `SightlineShared`, and remove the unnecessary `expect`/`actual` split on `userPreferencesDirectory()`.

**Blocked by:** 01, 02

**Status:** resolved

## Acceptance Criteria

- [x] **Remove dead legacy facade & pass-throughs**:
  - Delete `shared/src/commonMain/kotlin/com/trm/sightline/shared/PreferencesFacade.kt` and `shared/src/commonTest/kotlin/com/trm/sightline/shared/PreferencesFacadeTest.kt`.
  - Remove the unused methods/properties from `SightlineShared`: `preferences`, `categories`, `nearbyPlaces`, `reverseGeocode`, `searchAddresses`, and `locationUpdates`, keeping `placesCoordinator: PlacesCoordinator` and `locationProvider: LocationProvider` (and its internal constructor for testing).
- [x] **Move `:shared` sources and tests to `iosMain` and `iosTest`**:
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

## Answer

Implemented on `ticket/03-shared-iosmain` as commit `26cc77f` and merged into `integration/architecture-deepening` as merge commit `af7f494`.

- `PreferencesFacade.kt` and `PreferencesFacadeTest.kt` are deleted, along with every dead pass-through on `SightlineShared`: `preferences`, `categories`, `nearbyPlaces`, `reverseGeocode`, `searchAddresses`, and `locationUpdates`.
- `SightlineShared` keeps `placesCoordinator` as the single lazy `PlacesCoordinator`, plus `authorizationState`, `locationEnabled`, `resetPreferencesForTesting()`, and the internal four-repository constructor used by tests.
- `SightlineShared.kt` and `IosAppGraph.kt` moved from `shared/src/commonMain` to `shared/src/iosMain`, and `shared/src/commonMain` no longer exists.
- `SightlineSharedTest.kt` moved from `shared/src/commonTest` to `shared/src/iosTest` and was rewritten around the active coordinator surface; `shared/src/commonTest` no longer exists, so `:shared` now reads unambiguously as the iOS-only umbrella module.
- `shared/build.gradle.kts` now hangs the umbrella dependencies off `iosMain.dependencies` and `iosTest.dependencies` instead of `commonMain` and `commonTest`.
- The `expect` declaration is gone and `userPreferencesDirectory()` in `UserPreferencesDirectory.ios.kt` is a plain `internal fun` returning `UserPreferencesDirectory`.
- The rewritten `SightlineSharedTest` runs 6 tests: debounced category fetch from the active custom location, active location following user location while tracking is enabled, single injected coordinator instance, consolidated `PlacesUiState` contents, `authorizationState` and `locationEnabled` exposure, and `resetPreferencesForTesting()` restoring every default.
- The test-only setup installs `Dispatchers.setMain(StandardTestDispatcher(testScheduler))` per test and resets it in `@AfterTest`, because `PlacesCoordinator` defaults to `Dispatchers.Main` for its scope.
- `iosApp/README.md` drops the deleted facade and now describes the preferences sheet as backed by the shared `PlacesCoordinator` exposed by `SightlineShared`.

Verification: `:shared:iosSimulatorArm64Test` runs 6 `SightlineSharedTest` tests with zero failures and zero skips, `:shared:assembleSharedXCFramework` reports `BUILD SUCCESSFUL`, `:app:assembleDebug test` reports `BUILD SUCCESSFUL`, and `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' build` reports `BUILD SUCCEEDED`, confirming the iOS host still compiles against the slimmed `SightlineShared`.
On the merge commit `./gradlew :shared:iosSimulatorArm64Test :app:assembleDebug test --console=plain` reports `BUILD SUCCESSFUL` in 9s across 325 actionable tasks with the same 6 iosTest tests passing, and `:shared:assembleSharedXCFramework` reports `BUILD SUCCESSFUL` as well.
