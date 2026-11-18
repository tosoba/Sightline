# 23: iOS map (MapLibre)

**Spec:** `.scratch/kmp-ios-migration/specs/map.md`

**What to build:** An iOS user sees nearby places on a MapLibre map (wrapped via `UIViewRepresentable`) with the shared style JSONs, a current-location marker, and bounding-box fit, matching the Android map's features.

**Blocked by:** 22 (shared + iosApp)

**Status:** ready-for-agent

- [ ] The MapLibre map renders with the shared style JSONs.
- [ ] User location, place markers, and bounding-box fit work.
- [ ] MapKit remains the documented fallback if MapLibre proves awkward.
