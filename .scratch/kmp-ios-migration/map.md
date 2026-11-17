## Destination

Determine whether Sightline can migrate from an Android-only app to a Kotlin Multiplatform (KMP) app targeting Android and iOS only, and lay the groundwork for planning that migration.
The map produces a feasibility verdict (go / no-go / go-with-caveats), an obstacle and sacrifice ledger, and groundwork spec files.
No migration or refactor code, no sequencing, no effort estimates - those are later efforts.

## Notes

- Sightline is a location-based AR Android app (camera + map previews).
  Preserve all Android functionality on Android and replicate it fully on iOS.
- Parity bar: feature and value parity, not pixel parity.
  Platform-idiomatic implementations are welcome (e.g. the AR blur need not use the same mechanism).
- Shared in KMP: core and api modules.
  UI stays native: Jetpack Compose on Android, SwiftUI (HIG) on iOS.
  Compose Multiplatform for shared UI is out of scope.
- A pre-migration architectural refactor of the Android app is expected and acceptable.
  This map specifies the refactor; it does not execute it.
- Personal/solo project; prefer free and open-source libraries.
  Swapping a dependency for a lighter alternative is an acceptable sacrifice.
- DI: Metro is the leading candidate (compile-time KMP DI with a Hilt/Dagger migration path), evaluated against Koin, Kotlin-Inject, and manual injection.
- Current stack: CameraX, OpenGL ES 3.0 / JNI C++ (core:ar), MapLibre Compose, Hilt, Retrofit + mapbox-geojson, DataStore + Protobuf, Play Services Location, Navigation 3, Jetpack Compose.
  Already pure Kotlin: core:model and core:domain (kotlinx.serialization + coroutines).
- Skills every session should consult: grilling, domain-modeling, research.

## Decisions so far

- [AR camera effect on iOS](issues/01-ar-camera-effect-ios.md): feasible via hand-ported Metal (AVCaptureSession -> CVMetalTextureCache -> MTKView); Core Image fallback; math mostly reusable; orientation axis/sign remapping is the top risk.
- [Map rendering on iOS](issues/02-map-rendering-ios.md): Apple MapKit for SwiftUI (free, no key), map UI fully platform-native, no KMP map abstraction; MapLibre iOS the open-source fallback if style/cluster parity becomes a hard requirement.
- [Location provider for KMP](issues/03-location-provider-kmp.md): hand-rolled expect/actual LocationProvider over a library; add a platform-neutral GeoLocation type; reuse FusedLocationProviderClient on Android, ~60-line CLLocationManager actual on iOS.
- [Preferences persistence in KMP](issues/05-preferences-persistence-kmp.md): keep AndroidX DataStore 1.3.0 (now multiplatform); the blocker is protobuf (JVM-only), so UserPreferences becomes @Serializable JSON with expect/actual Storage (FileStorage/OkioStorage).
- [Networking migration to KMP](issues/04-networking-migration-kmp.md): Retrofit 3.0/OkHttp 5.x are NOT KMP (JVM-only), so api:photon switches to Ktor + kotlinx.serialization with hand-written ~30-line GeoJSON DTOs (properties as JsonObject).
- [Dependency injection in KMP](issues/06-dependency-injection-kmp.md): Metro (compile-time KMP, Hilt-like annotations, ~1:1 migration) over Koin/Kotlin-Inject; a Kotlin compiler plugin, no Hilt bytecode transform, so Android entry points re-wire manually.
- [Target shared architecture & pre-migration refactor](issues/07-target-shared-architecture.md): core:model/domain/datastore/api:photon/core:data/core:common/core:ar go KMP (math + pure utils split to commonMain); core:ui/feature:*/app stay Android; one `shared` XCFramework + SwiftUI iosApp; SKIE interop; Metal AR port; MapLibre Native for iOS (UIViewRepresentable, MapKit fallback); standalone refactor first with an Android-correctness gate.
- [Feasibility verdict & groundwork specs](issues/08-feasibility-verdict-specs.md): go-with-caveats (AR orientation remapping is the top risk); ledger of 12 obstacles (overcome vs sacrifice); eight spec files under `specs/` (module architecture, AR/blur, map, location, networking, persistence, DI, navigation).

## Not yet specified

<!-- empty: the interop choice (SKIE) and all research obstacles are now decided in the architecture ticket; nothing remains un-ticketable. -->


## Out of scope

- Executing the migration or the refactor (specified here, done later).
- Detailed migration sequencing, milestones, and effort estimates (a later planning effort).
- Compose Multiplatform for shared UI (native UI on both platforms).
- Platforms other than Android and iOS.
- iOS screen-by-screen HIG UI design.
