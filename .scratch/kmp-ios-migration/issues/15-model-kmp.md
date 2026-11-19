# 15: Convert core:model to KMP

**Spec:** `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:model` becomes a multiplatform module (Android + iOS) with its domain types in `commonMain`, and the Android app consumes it unchanged. This is the foundation every later conversion builds on.

**Blocked by:** 11 (GeoLocation)

**Status:** resolved

- [x] `core:model` declares Android and iOS targets and its types compile in `commonMain`.
- [x] The Android app consumes the model through the Android target with no source changes.
- [x] Android compiles and tests pass.

## Comments

Converted `core:model` to `kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` with `android`, `iosArm64`, and `iosSimulatorArm64` targets, sources in `src/commonMain/kotlin`, and tests in `src/commonTest` on `kotlin.test`.
`Place` no longer uses `java.util.Objects`; it hashes the id directly.
A `jvm()` target is declared temporarily because `core:datastore` and `core:domain` are still JVM-only modules until tickets 16 and 17 land; it is removed once they are KMP.

Verified `:app:assembleDebug`, `test`, `:core:model:testAndroidHostTest`, and `:core:model:compileKotlinIosSimulatorArm64` green.
