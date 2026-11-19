# 16: Convert core:domain to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:domain` (repository interfaces) becomes a multiplatform module in `commonMain` on top of `core:model`, with the Android app consuming it unchanged.

**Blocked by:** 15 (model KMP)

**Status:** resolved

- [x] `core:domain` interfaces compile in `commonMain` against `core:model`.
- [x] Android consumes `core:domain` unchanged.
- [x] Android compiles and tests pass.

## Comments

Replaced `java-library` + `kotlin-jvm` with `kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`, moved the three repository interfaces to `src/commonMain/kotlin`, and declared `android`, `iosArm64`, and `iosSimulatorArm64` targets.
The `jvm()` target stays until `core:datastore` (ticket 17) is KMP for the same reason as `core:model`.

Verified `:app:assembleDebug`, `test`, and `:core:domain:compileKotlinIosSimulatorArm64` green.
