# 18: Convert core:common to KMP (LocationProvider expect + engines)

**Spec:** `.scratch/kmp-ios-migration/specs/location.md`, `.scratch/kmp-ios-migration/specs/module-architecture.md`

**What to build:** `core:common` becomes a multiplatform module with the `LocationProvider` expect declaration and pure utilities in `commonMain`, the Android actual (`FusedLocationProviderClient`) in `androidMain`, and the iOS actual (`CLLocationManager`) in `iosMain`. The HTTP engine is split per platform.

**Blocked by:** 12 (LocationProvider + split), 13 (Metro), 15 (model)

**Status:** resolved

- [x] `LocationProvider` is an expect interface with Android and iOS actuals.
- [x] The iOS actual is a `CLLocationManager` + `callbackFlow` wrapper (distanceFilter 10, best accuracy, throttled flow).
- [x] Pure utilities compile in `commonMain`; Android-only permission/location code stays in `androidMain`.
- [x] Android compiles, tests pass, and location behavior is unchanged.

## Comments

`core:common` is now a KMP module with `android`, `iosArm64`, and `iosSimulatorArm64` targets.
`commonMain` holds `PermissionStatus`, `RequestError` (now mapped from Ktor `ResponseException` and `kotlinx.io.IOException`), the location provider contract (`LocationProvider`, `LocationAuthorization`, `LocationSettingsState`), `NumberExtensions` and `PlaceExtensions`.
`androidMain` holds `AndroidLocationProvider`, the Play Services `locationRequest`, the Metro `LocationModule`, the Android settings/resolvable intent helpers, and the Ktor `HttpClient` provider; `iosMain` holds `IosLocationProvider` and its Metro module.

Decisions taken while converting:

- `LocationProvider` stays the plain common interface introduced in ticket 12 instead of becoming `expect`/`actual`.
  The signatures are identical on both platforms, so `expect` would only add empty `actual interface` declarations; the platform actuals are the implementations in `androidMain`/`iosMain`.
- `NumberExtensions` no longer depends on `java.math.BigDecimal`: `roundToDecimalPlaces` returns a `Double` rounded through `roundToLong`, which prints the same text for every value the UI and `core:ar` feed it.
- The Compose-only `rememberPermissionState` and `rememberBottomSheetScaffoldStateForScreenHeight` helpers moved to `core:ui` (Android UI) rather than dragging the Compose compiler and runtime into every KMP target. `PermissionStatus` stays in `core:common`, and the Android string resources stay in `androidMain` with `androidResources { enable = true }` so `core.common.R` is unchanged.
- The Ktor client is created per platform: OkHttp with the shared 10 MB disk cache and `FLAG_DEBUGGABLE`-driven request logging on Android, Darwin with `Platform.isDebugBinary` logging on iOS. Both share `configureSightlineClient`: `expectSuccess = true`, JSON content negotiation with `ignoreUnknownKeys`, and 40s/30s/40s timeouts.
- `api:photon` still runs on Retrofit, so its `OkHttpClient`/`HttpLoggingInterceptor` bindings live on in a `LegacyNetworkModule` in `androidMain` until ticket 19; both clients share the same cache instance.

Verified `:app:assembleDebug`, `test`, `:core:common:compileAndroidMain`, and `:core:common:compileKotlinIosSimulatorArm64` green.
Host tests cover the `IOException` to `RequestError.IO` mapping, which is what Ktor reports for a dropped connection, and the `formattedDistance`/`roundToDecimalPlaces` output that `core:ar` and the UI depend on.
