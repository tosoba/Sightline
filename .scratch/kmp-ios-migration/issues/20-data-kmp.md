# 20: Convert core:data to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** The repositories and mappers (`PlacesNetworkRepository`, `AddressNetworkRepository`, `FeatureMapper`, `UserPreferencesLocalRepository`) build in `commonMain` on top of model/domain/datastore/photon, and the existing data-layer tests run from `commonTest`.

**Blocked by:** 16 (domain), 17 (datastore), 19 (photon)

**Status:** resolved

- [x] Repositories and mappers compile in `commonMain`.
- [x] The existing mapper/tag/radius tests move to `commonTest` and stay green.
- [x] Android consumes `core:data` unchanged; compiles and tests pass.

## Comments

`core:data` is now a KMP module (`android`, `iosArm64`, `iosSimulatorArm64`): the two network repositories, `FeatureMapper`, `PlaceCategoryOsmTags`, and `UserPreferencesLocalRepository` live in `commonMain`, and only the Metro `DataModule` (Android `Context`, `filesDir`, and the legacy `.pb` path) stays in `androidMain`.
The data-layer tests moved to `commonTest` on `kotlin.test`; the fixtures are unchanged apart from the argument order `kotlin.test` expects.

This completes the module conversion for tickets 15-20: all six shared modules now build for Android and both iOS targets, `./gradlew :app:assembleDebug test` is green, every KMP host test suite passes, and `compileKotlinIosSimulatorArm64`/`compileKotlinIosArm64` succeed for all of them.
