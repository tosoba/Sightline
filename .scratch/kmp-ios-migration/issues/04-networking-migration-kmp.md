Type: research
Status: resolved

## Question

How should `api:photon` migrate to KMP?

Current: Retrofit 3.0 and OkHttp 5.5 (both KMP-capable), returning `com.mapbox.geojson.FeatureCollection` (JVM-only) via a custom `Converter.Factory` that uses mapbox's Jackson parser (`api/photon/PhotonApi.kt`).
Gson and Moshi are dead dependencies.
The GeoJSON response is mapped to domain models in `core/data/FeatureMapper.kt`.

Research and report:

- Confirm Retrofit 3.0 KMP support and its iOS engine.
- Replace `mapbox-sdk-geojson` with kotlinx.serialization GeoJSON DTOs (FeatureCollection / Feature / Point / Geometry) matching photon's wire format.
- Swap the custom converter for a kotlinx.serialization converter, and note any photon-specific pitfalls (repeated `osm_tag` params, dynamic feature properties).
- Dead-dependency cleanup (Gson, Moshi, kotlinx-coroutines-play-services).

Report concrete migration steps and risks.

## Answer

Premise correction: Retrofit 3.0.0 and OkHttp 5.5.1 are NOT KMP - they are JVM/Android-only.
OkHttp dropped KMP in 5.0.0-alpha.13 and points to Ktor; Retrofit is "for Android and the JVM" and its first-party kotlinx-serialization converter is a `kotlin.jvm` module.

Recommendation: switch `api:photon` to **Ktor + kotlinx.serialization** (the only realistic path to a shared `commonMain` module), with hand-written GeoJSON DTOs instead of a third-party geojson lib.

- Replace `mapbox-sdk-geojson` with `FeatureCollection`/`Feature`/`Geometry` DTOs; model `properties` as `JsonObject` (photon returns `extent` and a nested `extra` object, so a typed class breaks). Use `ignoreUnknownKeys = true`.
- Ktor's `parameter("osm_tag", v)` in a loop reproduces the repeated-param encoding; `radius` stays in km; keep the null/blank-name and non-Point guards in `FeatureMapper`.
- Set `expectSuccess = true` to match Retrofit's throw-on-non-2xx; use `longOrNull` (not `intOrNull`) for `osm_id`.
- Dead deps: `gson`, `retrofit-converter-gson`, `mapbox-geojson` (all in `api:photon`); unused `moshi`/`moshi-adapters`/`retrofit-converter-moshi` catalog entries; `kotlinx-coroutines-play-services` in `core:common`.
- Cross-ticket: `PhotonApiModule`'s injected `OkHttpClient`/`HttpLoggingInterceptor` moves to a Ktor `HttpClient` provider (coordinate with DI ticket 06); port tests from MockWebServer to Ktor `MockEngine`.

Full findings: [research/04-networking-migration-kmp.md](research/04-networking-migration-kmp.md).
