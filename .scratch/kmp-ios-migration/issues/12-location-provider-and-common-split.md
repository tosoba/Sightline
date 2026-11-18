# 12: Introduce LocationProvider and split core:common pure utilities

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`

**What to build:** A `LocationProvider` abstraction over `FusedLocationProviderClient` exposing a fix flow, an enabled-state flow, an authorization state, and a settings check, returning `GeoLocation`. In the same pass, `core:common`'s pure utilities are separated from its Android-only location/network code so the pure part can move to `commonMain` later.

**Blocked by:** 11 (GeoLocation)

**Status:** resolved

- [x] `LocationProvider` exposes the four concerns and returns `GeoLocation`.
- [x] `MainViewModel` consumes `LocationProvider` instead of the Context extension functions directly.
- [x] Pure utilities are organized separately from Android-only code in `core:common`.
- [x] Android compiles, tests pass, and location behavior is unchanged.

## Comments

Added a `core:common/location` package: pure `LocationProvider` interface (fix flow, enabled-state flow, authorization state, settings check, all returning `GeoLocation`) plus `LocationAuthorization` and `LocationSettingsState`, and the Android `AndroidLocationProvider` implementation (fused provider + `LocationManager` wiring moved verbatim from `ContextExtensions`) plus a Metro `LocationModule` binding. `MainViewModel` now injects `LocationProvider` and drops the `Context` extension flows. The settings-resolvable `CheckLocationSettingsResult`/`startAppSettingsActivity` stay in the Android UI layer (`ContextExtensions`), consumed by `MainScreen` as before.

Verified `./gradlew :app:assembleDebug test` green. Commit `e07436f`, merged on the integration branch.
