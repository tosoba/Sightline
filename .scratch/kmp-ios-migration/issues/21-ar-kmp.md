# 21: Convert core:ar to KMP (math to commonMain)

**Spec:** `.scratch/kmp-ios-migration/specs/ar-blur.md`

**What to build:** The `core:ar` math/geometry builds in `commonMain` and is consumed by the Android OpenGL/CameraX/Canvas renderer unchanged; the Android-only rendering stays in `androidMain`.

**Blocked by:** 14 (extract ar math), 18 (common KMP)

**Status:** resolved

- [x] The math/geometry compiles in `commonMain`.
- [x] The Android renderer stays in `androidMain` and consumes the shared math with unchanged output.
- [x] Android compiles, tests pass, and AR rendering behavior is unchanged.

## Comments

`core:ar` is now a KMP module on the same convention as `core:common`/`core:data`: `android` (namespace, compileSdk 37, minSdk 24, JVM 11, `withHostTest {}`, `androidResources { enable = true }`), `iosArm64()`, and `iosSimulatorArm64()`.
There is no `iosMain` yet; ticket 24 adds the Metal renderer.

Source layout:

- `src/commonMain/kotlin/com/trm/sightline/core/ar/math/` - `Math3D`, `Vector1/2/3`, `Trig1/3`, `ScreenRotation`, `RoundedRectF`, `MarkerProjection`/`ProjectedMarker`, `MarkerSlotAssignment` and its `MarkerSlot`/`MarkerSlotTarget`/`MarkerSlotGeometry` inputs.
- `src/commonMain/kotlin/com/trm/sightline/core/ar/orientation/Orientation.kt` - moved too, because the math depends on it; `Math.PI` became `kotlin.math.PI`, the same double.
- `src/androidMain/kotlin/` - `OpenGLRenderer`, the CameraX surfaces/executor, `ARView`, `ARMarkerRenderer`, `OrientationManager`, `ARMarker`, the Canvas/Context extensions, and the XML layouts under `src/androidMain/res`.
- `src/commonTest/kotlin/` - the 31 characterization tests from ticket 14, unchanged.

The native build moved to a new `:core:ar-native` module, a plain `com.android.library` with the same CMake 4.1.2, `-std=c++17`, and `CMAKE_VERBOSE_MAKEFILE` settings holding `src/main/cpp`.
The `com.android.kotlin.multiplatform.library` plugin does not support `externalNativeBuild`, and Android's documented workaround is exactly this split; `core:ar` consumes it as an `androidMain` dependency.
Nothing in the Kotlin or JNI surface changed: the `Java_com_trm_sightline_core_ar_camera_OpenGLRenderer_*` symbols and `System.loadLibrary("opengl_renderer_jni")` are untouched.
`unzip -l app/build/outputs/apk/debug/app-debug.apk` still lists `lib/{arm64-v8a,armeabi-v7a,x86,x86_64}/libopengl_renderer_jni.so`, so the library is packaged from the new module.

Visibility changed only where the iOS side needs to reach the math from Swift: the classes and objects in `core.ar.math` and `Orientation`/`pitchWithinLimit` are now public, and nothing else was touched.
The renderer keeps its `internal` members, including `ARView.SavedState`.
`commonMain` depends on `core:model` (`api`, since `GeoLocation` appears in the public signatures) and nothing else; `kotlinx-coroutines` and the Compose/CameraX dependencies stay in `androidMain`.

The Compose compiler plugin is restricted to the Android target with `composeCompiler { targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm)) }`, since the iOS compilations have no Compose runtime and would otherwise fail with `IncompatibleComposeRuntimeVersionException`.
`kotlin-parcelize` works as-is on the Android compilation, so `ARView.SavedState` keeps its generated `Parcelable` implementation.

Tests: 31 in `testAndroidHostTest` and 31 in `iosSimulatorArm64Test`, all green.
The AGP KMP plugin registers `testAndroid`/`testAndroidHostTest` rather than `test`, so `:core:ar:test` is ambiguous; `:core:ar:testAndroid` and `:core:ar:check` (which runs `allTests`) both cover the Android host suite.
`compileKotlinIosArm64` and `compileKotlinIosSimulatorArm64` succeed.

`./gradlew :app:assembleDebug test` and `./gradlew :core:ar:check` are green; `app`, `feature:camera`, `feature:map`, `feature:places`, and `feature:place-category` still import the same `com.trm.sightline.core.ar.*` classes with no changes.

Notes for ticket 24:

- `MarkerProjection` keeps mutable scratch vectors, so one instance must be used from one thread at a time; the new instance per renderer is the safe pattern.
- `ScreenRotation` is a neutral enum; the iOS side must map `UIInterfaceOrientation` to it, mirroring what `Context.screenRotation` does on Android.
- The public API is raw `Float`/`Double` value types with mutable fields; Kotlin/Native exposes them directly, and the `pitchWithinLimit` extension appears as a top-level function in Swift.
- `ARView`, `ARMarkerRenderer`, `OpenGLRenderer`, and `OrientationManager` are Android-only and are not part of the shared framework.
