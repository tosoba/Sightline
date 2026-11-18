# 15: Convert core:model to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:model` becomes a multiplatform module (Android + iOS) with its domain types in `commonMain`, and the Android app consumes it unchanged. This is the foundation every later conversion builds on.

**Blocked by:** 11 (GeoLocation)

**Status:** ready-for-agent

- [ ] `core:model` declares Android and iOS targets and its types compile in `commonMain`.
- [ ] The Android app consumes the model through the Android target with no source changes.
- [ ] Android compiles and tests pass.
