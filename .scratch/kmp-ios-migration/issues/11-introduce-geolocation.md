# 11: Introduce GeoLocation and replace android.location.Location

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** A platform-neutral `GeoLocation` type in `core:model`, with shared-facing code reading and writing `GeoLocation` instead of `android.location.Location`, so shared code stops leaking an Android type. Expand-contract: add the new type, migrate call sites, then remove the leak. Android behavior and rendering stay unchanged.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `GeoLocation` (lat/lng/altitude/accuracy/timestamp plus haversine `distanceTo`/`bearingTo`) exists in `core:model`.
- [ ] Shared-facing code (marker projection, feature screens) uses `GeoLocation`; no `android.location.Location` leaks into shared-facing APIs.
- [ ] Haversine `distanceTo`/`bearingTo` have unit tests.
- [ ] Android compiles, tests pass, and location-dependent behavior is unchanged.
