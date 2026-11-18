# Spec: Android-only -> Kotlin Multiplatform (Android + iOS)

Consolidates the wayfinder map and its eight groundwork specs into one buildable plan.
Source: `.scratch/kmp-ios-migration/map.md`, `.scratch/kmp-ios-migration/issues/01-08`, `.scratch/kmp-ios-migration/specs/*.md`.

## Problem Statement

Sightline is an Android-only app: a location-based AR camera and map for discovering places by category.
The owner wants the same app on iOS, with feature and value parity, while keeping both platforms correct.
The current codebase is tied to Android-only dependencies (Hilt, Retrofit/OkHttp, protobuf DataStore, Play Services Location, OpenGL ES C++ rendering, Navigation 3), so a naive port is not possible.
The owner also wants a single shared codebase for the domain, data, and logic layers, rather than two divergent copies.

## Solution

Migrate Sightline to a Kotlin Multiplatform project targeting Android and iOS only.
Share the domain, data, and pure-logic layers in `commonMain`; keep UI fully native (Jetpack Compose on Android, SwiftUI on iOS).
Deliver iOS as one `shared` XCFramework consumed by a SwiftUI `iosApp`.
Stage the work so Android stays correct at every step, with a hard correctness gate after each stage.

## User Stories

1. As an iOS user, I want to point my camera at my surroundings and see nearby places overlaid in AR, so I can discover what is around me.
2. As an iOS user, I want the camera view to blur the background while keeping place markers crisp, so the markers stay legible against a busy scene.
3. As an iOS user, I want to switch to a map view, so I can see places in a 2D spatial context.
4. As an iOS user, I want to see my current location on the map, so I know where I am relative to nearby places.
5. As an iOS user, I want to pan, zoom, and rotate the map, so I can explore an area freely.
6. As an iOS user, I want to pick a place category, so I can narrow results to what I am interested in.
7. As an iOS user, I want search results near my current location, so the results are actually reachable.
8. As an iOS user, I want to tap a place and see its name and category, so I can decide whether it is worth visiting.
9. As an iOS user, I want to grant location permission, so the app can find places near me.
10. As an iOS user, I want to be deep-linked to Settings if I denied location, so I can re-enable it without digging through the app.
11. As an iOS user, I want my preferences to persist across launches, so I do not reconfigure the app every time.
12. As an iOS user, I want a map style consistent with the Android app, so the app feels like the same product.
13. As an iOS user, I want standard back navigation from the category screen, so I can return to the camera/map.
14. As an existing Android user, I want no change in behavior or appearance, so the migration does not regress what I already use.
15. As the maintainer, I want the domain, data, and logic in one shared module, so I do not maintain two copies of the same code.
16. As the maintainer, I want a staged refactor with Android green at every step, so a regression is caught immediately rather than at the end.

## Implementation Decisions

### Feasibility verdict

Go with caveats.
Every obstacle has a concrete resolution; none is a showstopper.

Caveats to verify before or early in execution:

- AR orientation axis/sign remapping is the top parity risk; verify with a prototype before committing to the full Metal port.
- Three dependency swaps (Retrofit to Ktor, protobuf to JSON, Hilt to Metro) are breaking changes to Android too, so the Android-correctness gate is mandatory.
- MapLibre Native for iOS is consumed through its stable `UIViewRepresentable` path, not its pre-1.0 SwiftUI DSL.

### Target module disposition

Move to `commonMain` as-is:

- `core:model` - domain value types plus the new `GeoLocation` and `@Serializable UserPreferences`.
- `core:domain` - repository interfaces.

Convert to KMP (commonMain plus platform source sets):

- `core:common` - pure utilities and the `LocationProvider` expect declaration; platform actuals for permissions, location, and the HTTP engine.
- `core:data` - repositories and mappers in commonMain; DI wiring on Android.
- `core:datastore` - the DataStore wrapper and JSON serializer in commonMain; `FileStorage` on Android, `OkioStorage` on iOS.
- `core:ar` - math and geometry in commonMain; OpenGL/CameraX/Canvas rendering on Android, Metal rendering on iOS.
- `api:photon` - Ktor client and GeoJSON DTOs in commonMain; engine selection per platform.

Stay Android-only:

- `core:ui`, `feature:camera`, `feature:map`, `feature:places`, `feature:place-category`, `app`.

New:

- `shared` - aggregator module exporting one XCFramework.
- `iosApp` - SwiftUI app consuming the framework through SKIE.

### Per-workstream decisions

AR camera effect:

- Hand-port the OpenGL ES blur to Metal: `AVCaptureSession` -> `CVMetalTextureCache` -> `MTKView`.
- The Gaussian shaders and rounded-rect SDF port cleanly to Metal Shading Language.
- The animated-lod blur maps to intermediate `MTLTexture` targets; the hole-punch maps to `MTLStencilDescriptor` plus `setScissorRect`.
- Extract the pure math into `core:ar` commonMain as the single source of truth, called from the Metal renderer through the shared framework; porting to simd is the fallback.
- The orientation path is `CMMotionManager` `.xMagneticNorthZVertical`; axis/sign remapping must be prototype-verified first.
- Core Image graph is the fallback if exact animated-blur control is not achievable cheaply; GPUImage3 is ruled out.
- Parity bar is visual equivalence, not pixel identity.

Map rendering:

- MapLibre Native for iOS wrapped in a stable `UIViewRepresentable`, chosen over MapKit for cross-platform style consistency (the existing style JSONs carry over).
- Map UI stays fully platform-native on each side; no shared KMP map abstraction.
- The shared value is the domain data already in `core:model` plus the pure bounding-box math moved to commonMain.
- Apple MapKit for SwiftUI is the fallback; it loses custom style JSON and cluster bubbles, which is acceptable under the parity bar.

Location:

- Hand-roll a small expect/actual `LocationProvider` and a platform-neutral `GeoLocation` type; no third-party KMP location library.
- `GeoLocation` lives in `core:model` and replaces `android.location.Location` in shared code, carrying haversine `distanceTo`/`bearingTo`.
- `LocationProvider` exposes a fix flow, an enabled-state flow, an authorization state, and a settings check.
- Android actual reuses `FusedLocationProviderClient`; move the `ResolvableApiException`/`IntentSenderRequest` handling up into the Android UI layer.
- iOS actual is a `CLLocationManager` + `callbackFlow` wrapper with no fixed interval (throttle in the Flow).
- The settings-resolvable intent has no iOS equivalent; deep-link via `UIApplication.openSettingsURLString`.

Networking:

- Switch `api:photon` from Retrofit/OkHttp to Ktor with hand-written `@Serializable` GeoJSON DTOs; Retrofit and OkHttp are JVM-only.
- Model `properties` as `JsonObject` with `ignoreUnknownKeys = true`.
- Reproduce photon encoding: repeated `osm_tag` params, `radius` in km, `expectSuccess = true`, `longOrNull` for `osm_id`.
- Keep the existing null/blank-name and non-Point guards in the mapper.
- Remove dead dependencies: Gson, Moshi, `mapbox-geojson`, `kotlinx-coroutines-play-services`.

Preferences persistence:

- Keep AndroidX DataStore (now multiplatform) and swap the two JVM-only pieces: protobuf serializer becomes `@Serializable` JSON, storage becomes expect/actual.
- `UserPreferences` becomes `@Serializable` in `core:model`; the 11 fields map 1:1 with `optional` to nullable and `bool` to `Boolean = false`.
- `UserPreferencesLocalRepository` swaps builder/has-check calls for `copy()` and null checks.
- One-time Android-only migration reads the old `.pb` into the JSON store on first launch after upgrade; iOS is greenfield.

Dependency injection:

- Use Metro, the only compile-time, first-class-KMP option, built as the Hilt/Dagger migration path.
- A Metro `AppGraph` assembles the graph; the current 3-module / 6-provides / 2-binds / 3-inject graph maps almost 1:1 onto Metro's annotations.
- Android entry points re-wire manually since Metro has no Hilt bytecode transform: the application becomes a plain `Application` holding `createGraph<AppGraph>()`, the activity drops `@AndroidEntryPoint`, and view models use `metrox-viewmodel-compose`.

Navigation:

- Navigation stays platform-native; there is no shared navigation abstraction.
- Android keeps Navigation 3; iOS uses SwiftUI `NavigationStack`.
- The view-model state behind navigation becomes shared Kotlin exposed to Swift via SKIE (Flow to AsyncSequence, suspend to async); navigation itself stays a pure-UI concern in SwiftUI.

### Refactor staging

The migration is staged so Android stays correct at every step.

1. Prerequisite prototype: verify AR orientation remapping with a throwaway Metal + Core Motion spike before committing to the full port.
2. Standalone Android decoupling refactor, with no KMP targets yet.
   Remove dead dependencies, introduce `GeoLocation` and `LocationProvider`, split `core:common` pure utilities from Android-only code, and extract `core:ar` math into its own package.
   Gate: Android compiles, tests pass, and behavior is unchanged.
3. Convert modules to KMP one at a time, `core:model` and `core:domain` first, then `core:datastore`, `api:photon`, `core:data`, `core:common`, `core:ar`.
   Gate: Android stays green after each conversion.
4. Add the `shared` aggregator and the SwiftUI `iosApp` consuming it through SKIE.
5. Port the platform renderers (Metal AR, MapLibre map) and wire iOS features end to end.

## Testing Decisions

The dominant seam is the Android-correctness gate: after the standalone refactor and after every KMP conversion, Android must compile, its tests stay green, and user-visible behavior is unchanged.
This is the single gate that every stage must pass before the next begins.

Unit seams:

- Move the existing `core:data` tests (mapper, category-tag mapping, radius conversion) to `commonTest` and keep them green across the conversion.
- Port `PhotonApiTest` from MockWebServer to a Ktor `MockEngine` so the network contract stays covered without a JVM-only harness.
- Add pure-Kotlin tests for the new shared logic: `GeoLocation` haversine distance/bearing, `core:ar` math (vector/trig/marker projection), and a `UserPreferences` JSON round-trip against the serializer's corruption mapping.

iOS seam:

- Verify iOS with a build-and-run smoke gate at the end: the `shared` XCFramework exports, the `iosApp` builds, and the camera, map, category, and preferences features each work on a simulator or device.

Test philosophy:

- Test external behavior, not implementation details.
- Prefer the highest seam available; do not add a new seam where the Android gate or an existing unit test already covers the behavior.

## Out of Scope

- Executing the migration or the refactor (specified here, done later by tickets).
- Detailed sequencing below the staging above, milestones, and effort estimates.
- Compose Multiplatform for shared UI; UI is native on both platforms.
- Platforms other than Android and iOS.
- iOS screen-by-screen HIG UI design.
- A shared KMP map abstraction (map UI is native per platform).
- A shared KMP navigation abstraction (navigation is native per platform).

## Further Notes

- Solo project; prefer free and open-source libraries.
- Swapping a dependency for a lighter alternative is an acceptable sacrifice.
- The obstacle and sacrifice ledger lives in the wayfinder map's ticket 08; it is not duplicated here.
- The groundwork specs under `specs/` remain the detailed per-workstream references; this file is the buildable plan that sequences them.
