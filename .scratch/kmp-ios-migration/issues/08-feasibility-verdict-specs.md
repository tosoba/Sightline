Type: task
Status: resolved
Blocked by: 07

## Question

Write the deliverables.

Using tickets 01-07, produce:

- A feasibility verdict (go / no-go / go-with-caveats).
- An obstacle and sacrifice ledger: every obstacle, its severity, and whether it is overcome or a sacrifice avoids it.
- Groundwork spec files: one per workstream (module architecture, AR/blur, map, location, networking, persistence, DI, navigation) under `.scratch/kmp-ios-migration/`.

This is the terminal artifact; the map stops here.

## Answer

### Feasibility verdict

**Go with caveats.**

The migration is feasible: every obstacle has a concrete resolution, none is a showstopper.
The value proposition (AR camera + map) is portable, the shared layers (`core:model`, `core:domain`) are already pure Kotlin, and the Android-only pieces (Hilt, Retrofit, protobuf, Play Services) each have a multiplatform replacement with an acceptable cost.

Caveats, to verify before or early in execution:

- AR orientation axis/sign remapping is the top parity risk; verify with a prototype before committing to the full Metal port.
- Three dependency swaps (Retrofit -> Ktor, protobuf -> JSON, Hilt -> Metro) are breaking changes to Android too, so the standalone refactor's Android-correctness gate is mandatory.
- MapLibre Native for iOS is consumed through its stable `UIViewRepresentable` path, not its pre-1.0 SwiftUI DSL.

### Obstacle and sacrifice ledger

| Obstacle | Severity | Outcome | Note |
| --- | --- | --- | --- |
| OpenGL ES C++ AR renderer is Android/JNI-only | High | Overcome | Hand-port to Metal; Core Image is the fallback |
| Orientation axis/sign remapping (SensorManager -> CMMotionManager) | High | Overcome (verify) | `xMagneticNorthZVertical` is the parity path; prototype first |
| MapLibre Compose is Android-only | Medium | Overcome | MapLibre Native for iOS via `UIViewRepresentable`; style JSONs carry over |
| Play Services Location is Android-only | Medium | Overcome | Hand-rolled expect/actual `LocationProvider` + `GeoLocation` |
| Settings-resolvable location intent has no iOS analog | Low | Sacrifice | Deep-link to `openSettingsURLString`; minor UX difference |
| Retrofit / OkHttp are JVM-only (not KMP) | High | Sacrifice (forced) | Switch to Ktor + kotlinx.serialization |
| `mapbox-geojson` is JVM-only | Medium | Sacrifice | Hand-written GeoJSON DTOs (`properties` as `JsonObject`) |
| protobuf bindings are JVM-only | Medium | Sacrifice (forced) | `@Serializable` JSON `UserPreferences`; one-time `.pb` -> JSON migration |
| Hilt is Android-only | Medium | Sacrifice (chosen) | Metro compile-time KMP DI; manual Android entry points |
| `android.location.Location` leaks into shared code | Low | Overcome | `GeoLocation` neutral type + haversine helpers |
| Dead dependencies (Gson, Moshi, kotlinx-coroutines-play-services) | Low | Overcome | Removed during the refactor |
| Navigation 3 is Android-only | Low | Overcome (no shared nav) | UI stays native; SwiftUI `NavigationStack` on iOS |

Severity scale: High / Medium / Low.

### Groundwork spec files

- [module-architecture.md](specs/module-architecture.md)
- [ar-blur.md](specs/ar-blur.md)
- [map.md](specs/map.md)
- [location.md](specs/location.md)
- [networking.md](specs/networking.md)
- [persistence.md](specs/persistence.md)
- [di.md](specs/di.md)
- [navigation.md](specs/navigation.md)

The map stops here.
