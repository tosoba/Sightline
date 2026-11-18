# 11: Introduce GeoLocation and replace android.location.Location

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** A platform-neutral `GeoLocation` type in `core:model`, with shared-facing code reading and writing `GeoLocation` instead of `android.location.Location`, so shared code stops leaking an Android type. Expand-contract: add the new type, migrate call sites, then remove the leak. Android behavior and rendering stay unchanged.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] `GeoLocation` (lat/lng/altitude/accuracy/timestamp plus haversine `distanceTo`/`bearingTo`) exists in `core:model`.
- [x] Shared-facing code (marker projection, feature screens) uses `GeoLocation`; no `android.location.Location` leaks into shared-facing APIs.
- [x] Haversine `distanceTo`/`bearingTo` have unit tests.
- [x] Android compiles, tests pass, and location-dependent behavior is unchanged.

## Comments

Added `GeoLocation` in `core:model` (haversine `distanceTo` in meters, initial-bearing `bearingTo` in degrees) with 6 JUnit4 tests. Replaced `android.location.Location` across `PlaceExtensions`, `MainViewModel`, the feature screens, `MapPreview`, and `core:ar` (`ARMarker`, `Math3D`, `ARView`, `ARMarkerRenderer`). The only remaining `android.location.Location` usage is `core:common/util/ContextExtensions.kt`, left for ticket 12. `Math3D` keeps `android.view.Surface` rotation constants (ticket 14).

Verified `./gradlew :app:assembleDebug test` green. Commit `d0e76d1`, merged as `90887ae`.
