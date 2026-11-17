# Module architecture

Groundwork spec for the KMP target architecture and the pre-migration Android refactor.

Source: issue 07-target-shared-architecture.md.

## Target

Sightline becomes a Kotlin Multiplatform project targeting Android and iOS only, sharing the domain, data, and pure logic layers while keeping UI native per platform.
A single `shared` framework plus a SwiftUI `iosApp` deliver the iOS app.

## Module disposition

### Move to commonMain (already pure Kotlin)

- `core:model` - domain value types (`Place`, `PlaceCategory`, `CustomLocation`, `MapCameraPosition`, `LoadingState`, `PlaceSearchRadius`) plus the new `GeoLocation` and `@Serializable UserPreferences`.
- `core:domain` - repository interfaces (`PlacesRepository`, `AddressRepository`, `UserPreferencesRepository`).

### Convert to KMP, split commonMain + platform source sets

- `core:common` - commonMain: pure utilities (`NumberExtensions`, `PlaceExtensions`, `PermissionState`, `RequestError`) plus the `LocationProvider` expect declaration.
  androidMain: permissions, FusedLocationProviderClient location, Ktor OkHttp engine.
  iosMain: Core Location, Ktor Darwin engine.
- `core:data` - commonMain: repositories (`PlacesNetworkRepository`, `AddressNetworkRepository`), `FeatureMapper`, `UserPreferencesLocalRepository`.
  androidMain: DI wiring.
- `core:datastore` - commonMain: the DataStore wrapper and JSON serializer.
  androidMain: `FileStorage`.
  iosMain: `OkioStorage`.
- `core:ar` - commonMain: the math/geometry layer (`Vector2`/`Vector3`, `Trig1`/`Trig3`, `Math3D`, `RoundedRectF`, marker projection).
  androidMain: `OpenGLRenderer`, camera surfaces, Canvas `ARView`/`ARMarkerRenderer`.
  iosMain: Metal renderer and SwiftUI overlay (new).
- `api:photon` - commonMain: `PhotonApi` (Ktor) and GeoJSON DTOs.
  androidMain/iosMain: engine selection only.

### Stay Android-only

- `core:ui` - Compose theme, `MapPreview`, map style resources.
- `feature:camera`, `feature:map`, `feature:places`, `feature:place-category`.
- `app`.

### New

- `shared` - aggregator module that exports the KMP modules as one XCFramework for the iOS app.
- `iosApp` - SwiftUI app consuming the framework through SKIE.

## Refactor staging

The migration is staged so Android stays correct at every step.

1. Standalone Android decoupling refactor, with no KMP targets yet.
   - Remove dead dependencies (Gson, Moshi, mapbox-geojson, kotlinx-coroutines-play-services).
   - Introduce `GeoLocation` and replace `android.location.Location` in shared-facing code.
   - Introduce the `LocationProvider` abstraction.
   - Split `core:common` pure utilities from Android-only code.
   - Extract `core:ar` math into its own package so it can move to commonMain.
   - Gate: Android compiles, tests pass, and behavior is unchanged.
2. Convert modules to KMP one at a time (model and domain first, then datastore, photon, data, common, ar).
3. Add the `shared` aggregator and the SwiftUI `iosApp`.
4. Port the platform renderers (Metal, MapLibre) and wire iOS.

## Cross-cutting decisions

- Swift interop: SKIE (automatic Flow -> AsyncSequence and suspend -> async).
- DI: Metro (see specs/di.md).
- UI stays native: Jetpack Compose on Android, SwiftUI (HIG) on iOS.
- No Compose Multiplatform for shared UI.

## Out of scope

- Executing this refactor or migration.
- Sequencing, milestones, and effort estimates.
- Platforms other than Android and iOS.
