Type: grilling
Status: resolved
Blocked by: 01, 02, 03, 04, 05, 06

## Question

Lock the target KMP module architecture and the pre-migration Android refactor plan.

Inputs: research tickets 01-06 (AR effect, map, location, networking, persistence, DI).

Decide:

- Which modules move to commonMain as-is (core:model, core:domain), which split into commonMain + platform source sets (core:common's location/permissions vs pure utilities; core:data; core:datastore; api:photon), and which stay platform-native (core:ar rendering, core:ui Compose/map, feature:*, app).
- The pre-migration refactor steps: introduce a location abstraction and a platform-neutral location type, split core:common, remove vestigial dependencies and dead deps.
- The accepted sacrifices surfaced by the research.
- Swift interop: SKIE vs KMP-NativeCoroutines for exposing Flow and suspend to Swift.

This is a live HITL conversation; its decisions are written up by ticket 08.

## Answer

Decisions locked:

1. Module disposition:
   - `commonMain`: `core:model` and `core:domain` (as-is); `core:datastore` (`@Serializable` + expect/actual `Storage`); `api:photon` (Ktor + GeoJSON DTOs); `core:data` (repos + mappers); `core:common` (pure utilities); `core:ar` (math/geometry).
   - Split into `commonMain` + platform actuals: `core:common` (androidMain = permissions/location/Ktor engine), `core:data` (androidMain = DI), `core:ar` (androidMain = GL/CameraX/Canvas rendering), `core:datastore` (androidMain `FileStorage` / iosMain `OkioStorage`).
   - Android-only: `core:ui`, `feature:camera`, `feature:map`, `feature:places`, `feature:place-category`, `app`.
   - New: a `shared` aggregator module exporting one XCFramework, plus a SwiftUI `iosApp`.
2. `core:ar` math: shared in `commonMain` as the single source of truth; the Swift/Metal renderer calls it via the framework.
   Port-to-simd is the fallback.
3. Swift interop: SKIE (automatic Flow -> AsyncSequence + suspend -> async).
4. AR effect: Metal hand-port (exact control of the 7-pass animated blur); Core Image is the fallback.
5. Map: MapLibre Native for iOS (stable `UIViewRepresentable` wrap; the pre-1.0 first-party SwiftUI DSL is optional), chosen over MapKit for cross-platform style consistency.
   MapKit is the fallback if MapLibre proves awkward. This overrides ticket 02's MapKit recommendation.
6. Refactor staging: standalone Android decoupling refactor first - dead-dependency cleanup, `GeoLocation`/`LocationProvider` abstraction, `core:common` split, `core:ar` math extraction - with a hard Android-correctness gate: Android keeps compiling, tests stay green, and functionality stays unchanged, verified before any KMP target is introduced.
   Then convert modules to KMP one at a time.

Accepted sacrifices from research: Ktor over Retrofit (forced - Retrofit is JVM-only), `@Serializable` JSON over protobuf (forced - protobuf bindings are JVM-only), Metro over Hilt (chosen), hand-rolled `LocationProvider` (chosen).

