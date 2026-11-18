# 18: Convert core:common to KMP (LocationProvider expect + engines)

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:common` becomes a multiplatform module with the `LocationProvider` expect declaration and pure utilities in `commonMain`, the Android actual (`FusedLocationProviderClient`) in `androidMain`, and the iOS actual (`CLLocationManager`) in `iosMain`. The HTTP engine is split per platform.

**Blocked by:** 12 (LocationProvider + split), 13 (Metro), 15 (model)

**Status:** ready-for-agent

- [ ] `LocationProvider` is an expect interface with Android and iOS actuals.
- [ ] The iOS actual is a `CLLocationManager` + `callbackFlow` wrapper (distanceFilter 10, best accuracy, throttled flow).
- [ ] Pure utilities compile in `commonMain`; Android-only permission/location code stays in `androidMain`.
- [ ] Android compiles, tests pass, and location behavior is unchanged.
