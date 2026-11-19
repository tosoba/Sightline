# 21: Convert core:ar to KMP (math to commonMain)

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** The `core:ar` math/geometry builds in `commonMain` and is consumed by the Android OpenGL/CameraX/Canvas renderer unchanged; the Android-only rendering stays in `androidMain`.

**Blocked by:** 14 (extract ar math), 18 (common KMP)

**Status:** claimed

- [ ] The math/geometry compiles in `commonMain`.
- [ ] The Android renderer stays in `androidMain` and consumes the shared math with unchanged output.
- [ ] Android compiles, tests pass, and AR rendering behavior is unchanged.
