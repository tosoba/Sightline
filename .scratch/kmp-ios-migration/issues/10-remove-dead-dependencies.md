# 10: Remove dead dependencies

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** Remove the genuinely-unused dependencies and version-catalog entries so the Android build is lean and the later KMP conversions start from a clean dependency surface. Dependencies that are still used but will be removed by a specific conversion (gson, mapbox-geojson, kotlinx-coroutines-play-services) are left for those conversion tickets.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] Unused dependencies and version-catalog entries are removed.
- [x] Android compiles and all tests pass with no behavior change.
- [x] No still-used dependency is removed here; doomed dependencies are noted for their conversion tickets.

## Comments

Removed the only genuinely-unused catalog entries: `moshi`, `moshi-adapters`, `retrofit-converter-moshi` (plus the `moshi` version). None were referenced by any `build.gradle.kts` or imported in source.

Left for their conversion tickets as doomed-but-still-present: `gson` + `retrofit-converter-gson` (networking ticket 19), `mapbox-geojson` (networking ticket 19), `kotlinx-coroutines-play-services` (location tickets 12/18). Verified via `grep` that each is still declared or imported; none removed here.

Verified `./gradlew :app:assembleDebug test` is green after removal.
