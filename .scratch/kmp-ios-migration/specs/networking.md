# Networking

Groundwork spec for migrating `api:photon` to Ktor + kotlinx.serialization.

Source: issue 04-networking-migration-kmp.md.

## Decision

Switch `api:photon` from Retrofit/OkHttp to Ktor with hand-written GeoJSON DTOs.
Retrofit 3.0 and OkHttp 5.5 are JVM-only, so they cannot be shared.

## Current

`api/photon/PhotonApi.kt` uses Retrofit 3 + OkHttp 5, returning `com.mapbox.geojson.FeatureCollection` (JVM-only) through a custom converter that calls mapbox's Jackson parser.
`api/photon/PhotonApiModule.kt` provides the `OkHttpClient` + `HttpLoggingInterceptor` via Hilt.
The response is mapped to domain models in `core/data/FeatureMapper.kt`.

## Target design

- `PhotonApi` becomes a Ktor `HttpClient` wrapper in commonMain with `search` and `reverse` methods.
- Replace `mapbox-sdk-geojson` with `@Serializable` DTOs: `FeatureCollection`, `Feature`, `Geometry`, `Point`.
- Model `properties` as `JsonObject` (photon returns `extent` and a nested `extra`, so a typed class breaks); use `ignoreUnknownKeys = true`.
- Reproduce photon encoding: `parameter("osm_tag", v)` in a loop for repeated params, `radius` in km, `expectSuccess = true` to match Retrofit's throw-on-non-2xx, `longOrNull` (not `intOrNull`) for `osm_id`.
- Keep the null/blank-name and non-Point guards in `FeatureMapper`.
- Engine per platform: OkHttp on Android, Darwin on iOS.

## Dependency cleanup

Remove `gson`, `retrofit-converter-gson`, `mapbox-geojson` (in `api:photon`); unused `moshi`/`moshi-adapters`/`retrofit-converter-moshi` catalog entries; `kotlinx-coroutines-play-services` in `core:common`.

## Testing

Port `PhotonApiTest` from MockWebServer to Ktor `MockEngine`.

## Cross-ticket

The injected `OkHttpClient`/`HttpLoggingInterceptor` moves to a Ktor `HttpClient` provider (see specs/di.md).
