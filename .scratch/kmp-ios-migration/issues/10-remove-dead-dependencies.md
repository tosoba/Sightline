# 10: Remove dead dependencies

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** Remove the genuinely-unused dependencies and version-catalog entries so the Android build is lean and the later KMP conversions start from a clean dependency surface. Dependencies that are still used but will be removed by a specific conversion (gson, mapbox-geojson, kotlinx-coroutines-play-services) are left for those conversion tickets.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Unused dependencies and version-catalog entries are removed.
- [ ] Android compiles and all tests pass with no behavior change.
- [ ] No still-used dependency is removed here; doomed dependencies are noted for their conversion tickets.
