# Networking migration to KMP: `api:photon`

Researched 2026-11-17 against Retrofit, OkHttp, kotlinx.serialization, and Ktor primary sources plus live photon responses.

## Recommendation

The ticket's premise is wrong in one load-bearing way: Retrofit 3.0.0 and OkHttp 5.5.1 are not Kotlin Multiplatform.
They are JVM/Android-only and cannot run in `commonMain` on iOS.

Do not try to keep Retrofit in shared code.
Migrate `api:photon` to Ktor (client) with kotlinx.serialization, which is the first-party multiplatform HTTP stack and the one OkHttp's own changelog points to.

The GeoJSON part of the migration (drop `mapbox-sdk-geojson`, hand-write DTOs, parse dynamic `properties`) is independent of the HTTP-client choice and is the same amount of work either way.

## 1. Retrofit 3.0.0 KMP support

Retrofit 3.0.0 is not multiplatform.

- The repository is now `lysine-dev/retrofit` (formerly `square/retrofit`), and it describes itself as "A type-safe HTTP client for Android and the JVM".
- The README states a minimum of "Java 8+ or Android API 21+"; there is no iOS/Native target.
- The 3.0.0 changelog's only substantive change over 2.x is "Upgrade to OkHttp 4.12 (from 3.14) ... the version of OkHttp that is written in Kotlin". No KMP source sets were added.
- Retrofit's core is built on `java.lang.reflect` (`Proxy`, `Type`, `Method`) and Java annotations, which do not exist on Kotlin/Native, so a `commonMain` port is not a small change.
- The first-party kotlinx.serialization converter (`com.squareup.retrofit2:converter-kotlinx-serialization`) applies the `org.jetbrains.kotlin.jvm` plugin (verified in its `build.gradle`); it is JVM-only too.

There is no official or well-maintained KMP fork of Retrofit to recommend for a solo project.

OkHttp 5.x is also not multiplatform, despite the ticket's assumption.

- OkHttp `5.0.0-alpha.13` (2024-04-16) explicitly records: "Breaking: Drop support for Kotlin Multiplatform. We planned to support multiplatform in OkHttp 5.0, but ... we weren't prepared to build a TLS API for Kotlin/Native. We'd prefer a multiplatform HTTP client API that's backed by OkHttp on Android and JVM, and other engines on other platforms. Ktor does this pretty well today!"
- The stable `5.0.0` release (2025-07-02) is "packaged as separate JVM and Android artifacts" only.

So on iOS there is no Retrofit and no OkHttp to fall back to.
Any Retrofit-based `api:photon` would have to remain a JVM/Android module with a separate iOS implementation via expect/actual, which contradicts the map's "shared in KMP: core and api modules" destination.

Note: the repo currently runs Retrofit 3.0.0 against OkHttp 5.5.1, which works because OkHttp 5.x keeps the `okhttp3` package and the API surface Retrofit needs (`Call.Factory`, `Request`, `ResponseBody`), but that is an Android/JVM convenience, not KMP.

## 2. Replacing `mapbox-sdk-geojson` with kotlinx.serialization DTOs

`mapbox-sdk-geojson` is JVM-only (Jackson-based) and is exactly the dependency that blocks sharing.
Its only use in the response path is `FeatureCollection.fromJson(...)` in `PhotonApi` and `Feature`/`Point` reads in `core/data/FeatureMapper.kt`.

### The photon wire format (verified live)

Forward `/api` and reverse `/reverse` both return:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": { "osm_type": "W", "osm_id": 891309425, "name": "...", "street": "...", "city": "...", "country": "...", "countrycode": "...", "extent": [21.018432, 52.2379293, 21.0199547, 52.2372828] },
      "geometry": { "type": "Point", "coordinates": [21.0193187, 52.2375918] }
    }
  ]
}
```

Key facts:

- Top level is a standard GeoJSON `FeatureCollection` (`type` + `features[]`).
- Each `Feature` is `type`, `geometry`, `properties` (no top-level `id` in photon's output).
- `geometry` is a `Point` with `coordinates` ordered `[longitude, latitude]`.
- `properties` is a dynamic object: `osm_type` (string `N`/`W`/`R`), `osm_id` (JSON integer, fits in `Long`), `osm_key`, `osm_value`, `name`, plus address fields, an `extent` array of four numbers, and on forward search a nested `extra` object (`{"extra":{"admin_level":"4"}}`).
- Some `properties` values are absent, `null`, or blank strings (the mapper already trims and drops blank names).

### Model `properties` as `JsonObject`

Because `properties` has arbitrary keys and can contain nested objects (`extra`) and arrays (`extent`), do not model it as a typed data class.
Model it as `kotlinx.serialization.json.JsonObject`, which is a `Map<String, JsonElement>`, and read the fields the mapper needs via `jsonPrimitive`.

The DTOs belong in `api:photon`'s `commonMain` (they are return types of the API), and `core:data` reads them in `FeatureMapper`.

```kotlin
@Serializable
data class FeatureCollection(
  val type: String = "FeatureCollection",
  val features: List<Feature> = emptyList(),
)

@Serializable
data class Feature(
  val type: String = "Feature",
  val geometry: Geometry? = null,
  val properties: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class Geometry(
  val type: String,
  val coordinates: JsonArray = JsonArray(emptyList()),
)
```

A small helper extracts `[lat, lon]` for `Point` geometries and returns `null` for anything else, preserving the existing "non-Point geometry is ignored" behaviour:

```kotlin
internal fun Geometry?.point(): Pair<Double, Double>? {
  val g = this ?: return null
  if (g.type != "Point" || g.coordinates.size < 2) return null
  val lon = g.coordinates[0].jsonPrimitive.doubleOrNull ?: return null
  val lat = g.coordinates[1].jsonPrimitive.doubleOrNull ?: return null
  return lat to lon
}
```

The rewritten mapper reads properties as:

```kotlin
val name = feature.properties["name"]?.jsonPrimitive?.contentOrNull
val osmType = feature.properties["osm_type"]?.jsonPrimitive?.contentOrNull
val osmId = feature.properties["osm_id"]?.jsonPrimitive?.longOrNull
```

The existing `id = "$osmType:${osmId}"` logic is unchanged.

Configure the `Json` instance with `ignoreUnknownKeys = true` (mandatory, given `extent` and `extra`), and optionally `coerceInputValues = true` to be forgiving of `null`/missing scalar fields.

### Hand-written model vs a third-party multiplatform GeoJSON library

Hand-written is the right call here:

- Only `Point` geometry and a properties map are consumed; the full GeoJSON spec (LineString, Polygon, MultiGeometry, `bbox`, `id`) is not needed.
- It is about 30 lines and removes a dependency rather than adding one.
- A third-party `kotlinx.serialization` GeoJSON library exists but buys nothing for two endpoints and adds an external API surface to track.

## 3. The converter

The `retrofit2-kotlinx-serialization-converter` artifact is not KMP-ready.

- The original Jake Wharton repo is archived and deprecated; it moved into Retrofit as `com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0`.
- That first-party module is built with `org.jetbrains.kotlin.jvm` (JVM-only), and it only makes sense attached to Retrofit, which is also JVM-only.

So the "register a kotlinx converter to replace the custom `Converter.Factory`" step only applies on Android/JVM.
In a KMP shared module it is replaced by Ktor's ContentNegotiation + `ktor-serialization-kotlinx-json` (see section 5).

The one converter caveat worth carrying over (from the Jake Wharton README) is generic, not photon-specific: the kotlinx converter assumes it can handle any type, so if it coexists with another converter it must be registered last.
With Ktor this does not apply because ContentNegotiation is the single pipeline.

Photon-specific pitfalls that carry into any of these stacks:

- Repeated `osm_tag` query params: Retrofit's `@Query("osm_tag") osmTag: List<String>` expands to one `osm_tag` per element; Ktor's `parameter("osm_tag", value)` appends, so a `forEach { parameter("osm_tag", it) }` loop reproduces the same wire output. Keep the MockWebServer/MockEngine assertion that each value arrives as a distinct `osm_tag`.
- `radius` is in kilometres (the repo already converts metres to km in `core:data`); nothing changes.
- `properties` must stay a map (section 2); typed properties would break on `extra` and `extent`.
- Null/blank names: the existing `trim` + `isNullOrBlank` guard must be preserved in the mapper.

## 4. Dead-dependency cleanup

Current dependency graph, from `gradle/libs.versions.toml` and module build files:

- `api/photon` declares `libs.gson` and `libs.retrofit.converter.gson`; both are dead (the custom converter uses mapbox's Jackson parser, not Gson).
- `api/photon` declares `api(libs.mapbox.geojson)`; removed entirely by this migration.
- `libs.moshi`, `libs.moshi.adapters`, and `libs.retrofit.converter.moshi` exist only in the version catalog; no module depends on them. They are dead and can be deleted from the catalog.
- `core/common` declares `libs.kotlinx.coroutines.play.services`, which is dead there (its `play-services-location` dependency is the actual Play Services integration, and `kotlinx-coroutines-play-services` is only needed for `Task.await` on Play Services tasks; it is not referenced in source). Remove it.
- `libs.retrofit` and `libs.okhttp`/`okhttp.logging.interceptor` remain used on Android (and OkHttp is reused by Ktor's OkHttp engine), so they are not dead, but `api:photon` itself stops importing Retrofit directly once it moves to Ktor.

Concrete cleanup list:

1. Remove `implementation(libs.gson)` from `api:photon`.
2. Remove `implementation(libs.retrofit.converter.gson)` from `api:photon`.
3. Remove `api(libs.mapbox.geojson)` from `api:photon`.
4. Remove `gson`, `moshi`, `moshi-adapters`, `retrofit-converter-gson`, `retrofit-converter-moshi` entries from `gradle/libs.versions.toml` (and their `[versions]` rows if no longer referenced).
5. Remove `implementation(libs.kotlinx.coroutines.play.services)` from `core/common` and its catalog entry.
6. Optionally drop `retrofit`/`retrofit-converter-*` from the catalog once `api:photon` is the last Retrofit consumer (check `core/common` and `app` first).

## 5. Ktor vs staying on Retrofit

Staying on Retrofit in shared code is not possible: Retrofit and OkHttp are JVM/Android-only (section 1).

Ktor is the serious alternative and, in practice, the only realistic path to a shared `api:photon`.

- Ktor's HTTP client is multiplatform: JVM, Android, Kotlin/JS, Kotlin/Native (iOS/macOS).
- On iOS it uses the Darwin engine (`NSURLSession`), and on Android it can reuse the existing OkHttp stack via the OkHttp engine (or the Android engine, or CIO).
- It has a first-party kotlinx.serialization pipeline (`ktor-client-content-negotiation` + `ktor-serialization-kotlinx-json`), which aligns with the repo's existing kotlinx.serialization models in `core:model`.
- OkHttp's own changelog recommends exactly this: "We'd prefer a multiplatform HTTP client API ... Ktor does this pretty well today!"
- Ktor 3.6.x is current (docs dated 2026-10-30).

The trade-offs:

- The interface style changes from Retrofit's annotation-driven `interface` to Ktor's `client.get { ... }.body<T>()` DSL. For two `@GET` endpoints this is a small, mechanical rewrite.
- The Hilt wiring in `PhotonApiModule` (injected `OkHttpClient` + `HttpLoggingInterceptor`) changes to building an `HttpClient` in `commonMain`, with engine selection via `expect`/`actual` or `ktor-client-engine-defaults`. This is shared with the DI ticket (06); flag the dependency.
- Error handling must be made explicit: Retrofit throws `HttpException` on non-2xx by default; Ktor needs `expectSuccess = true` (or explicit status checks) to match.
- Logging swaps `HttpLoggingInterceptor` for Ktor's `Logging` plugin (or, on Android only, keep the OkHttp interceptor via the OkHttp engine's `config`).

## Concrete migration steps

1. Create a KMP module for `api:photon` (or convert it) with `androidTarget` + iOS targets, applying `kotlin.serialization`; move `PhotonApi`, `PhotonApiModule`-adjacent wiring, and the new DTOs into `commonMain`.
2. Replace `com.mapbox.geojson.FeatureCollection` with the hand-written `FeatureCollection`/`Feature`/`Geometry` DTOs in `commonMain` (section 2).
3. Delete the custom Retrofit `Converter.Factory` from `PhotonApi`; rewrite the two endpoints as Ktor `get` calls with `parameter(...)` and repeated `osm_tag` via `forEach`.
4. Install `ContentNegotiation` with `json(Json { ignoreUnknownKeys = true; coerceInputValues = true })` on the shared `HttpClient`.
5. Add engine dependencies: `ktor-client-engine-defaults` in `commonMain`, or `ktor-client-okhttp` in `androidMain` + `ktor-client-darwin` in `iosMain`.
6. Rewrite `core:data/FeatureMapper.kt` to read `JsonObject` properties and `Geometry` coordinates instead of mapbox `Feature`/`Point`.
7. Port `PhotonApiTest` from MockWebServer to Ktor `MockEngine` (or keep MockWebServer on the JVM/android unit test source set), preserving the `radius` + repeated `osm_tag` assertion; rewrite `FeatureMapperTest` fixtures to build DTOs or parse via kotlinx `Json`.
8. Apply the dead-dependency cleanup (section 4).
9. Reconcile DI: the injected `OkHttpClient`/`HttpLoggingInterceptor` in `PhotonApiModule` moves to a Ktor `HttpClient` provider (coordinate with ticket 06).

## Risks

- Engine parity: Darwin (NSURLSession) vs OkHttp/Android differ subtly in redirects, caching, timeouts, and TLS behaviour. For two read-only keyless GET endpoints the risk is low, but the reverse/search test should be re-verified on-device on iOS.
- Error semantics: forgetting `expectSuccess = true` silently returns non-2xx bodies where Retrofit used to throw. Add it explicitly and test a 4xx/5xx case.
- `properties` modelling: if `properties` is ever serialized as an explicit `null` (not just missing keys), a non-nullable `JsonObject` field will throw. Photon always emits an object, but declaring `properties: JsonObject?` with a null-safe read is the defensive option.
- `osm_id` overflow: use `longOrNull`, not `intOrNull`; OSM ids exceed `Int` range.
- The DTOs become part of `api:photon`'s public API surface (they are return types). Keep them minimal and treat `core:data` as the only mapping layer to `core:model`.
