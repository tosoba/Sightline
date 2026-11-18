# 14: Extract core:ar math into its own package

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** The pure math/geometry (vectors, trig, matrix/3D, rounded-rect, marker projection) is extracted into a self-contained package free of Android types, so it can move to `commonMain` without the renderer. Android rendering is unchanged.

**Blocked by:** 11 (GeoLocation)

**Status:** ready-for-agent

- [ ] The math/geometry lives in a dedicated package with no Android type dependencies.
- [ ] The OpenGL/CameraX/Canvas renderer consumes the extracted math and renders identically.
- [ ] Android compiles, tests pass, and AR rendering behavior is unchanged.
