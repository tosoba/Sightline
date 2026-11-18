# 12: Introduce LocationProvider and split core:common pure utilities

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`

**What to build:** A `LocationProvider` abstraction over `FusedLocationProviderClient` exposing a fix flow, an enabled-state flow, an authorization state, and a settings check, returning `GeoLocation`. In the same pass, `core:common`'s pure utilities are separated from its Android-only location/network code so the pure part can move to `commonMain` later.

**Blocked by:** 11 (GeoLocation)

**Status:** ready-for-agent

- [ ] `LocationProvider` exposes the four concerns and returns `GeoLocation`.
- [ ] `MainViewModel` consumes `LocationProvider` instead of the Context extension functions directly.
- [ ] Pure utilities are organized separately from Android-only code in `core:common`.
- [ ] Android compiles, tests pass, and location behavior is unchanged.
