# 19: Convert api:photon to KMP (Ktor + GeoJSON DTOs)

**Spec:** `.scratch/kmp-ios-migration/specs/networking.md`

**What to build:** The photon reverse search runs over Ktor with hand-written `@Serializable` GeoJSON DTOs instead of Retrofit/OkHttp/mapbox-geojson, so the search is shareable. gson and mapbox-geojson are removed. `PhotonApiTest` moves from MockWebServer to Ktor `MockEngine`.

**Blocked by:** 18 (common KMP)

**Status:** resolved

- [x] `PhotonApi` is a Ktor `HttpClient` wrapper with `search` and `reverse` in `commonMain`.
- [x] GeoJSON DTOs are `@Serializable` with `properties` as `JsonObject` and `ignoreUnknownKeys`.
- [x] Photon encoding matches: repeated `osm_tag` params, `radius` in km, `expectSuccess`, `longOrNull` for `osm_id`.
- [x] `PhotonApiTest` runs on Ktor `MockEngine`; Android search results are unchanged.

## Comments

`api:photon` is now a KMP module (`android`, `iosArm64`, `iosSimulatorArm64`) with `PhotonApi` as a thin Ktor wrapper around an injected `HttpClient`, `search` and `reverse` keeping their previous parameter names and defaults.
The `FeatureCollection`/`Feature`/`Geometry` DTOs are `@Serializable`: `properties` is a `JsonObject` and `coordinates` a `JsonElement`, so photon's `extent`/nested `extra` properties and non-Point geometries decode without a typed class.
`expectSuccess` and `ignoreUnknownKeys` come from the shared client configuration in ticket 18; request logging replaced the OkHttp `HttpLoggingInterceptor`.

Encoding is unchanged: repeated `osm_tag` query params, `radius` in kilometres, and the mapper still uses `longOrNull` for `osm_id` and keeps the null/blank-name and non-Point guards.
`PhotonApiModule` is a `commonMain` Metro binding, so both platforms get it.

Removed `gson`, `retrofit`, `retrofit-converter-gson`, `mapbox-sdk-geojson`, `mockwebserver`, the OkHttp logging interceptor, and the transitional `LegacyNetworkModule` from ticket 18.
`FeatureMapperTest` now decodes its fixtures through the new DTOs, and `PhotonApiTest` runs on `MockEngine` with a new `search` encoding case.

Verified `:app:assembleDebug`, `test`, `:api:photon:testAndroidHostTest`, and `:api:photon:compileKotlinIosSimulatorArm64` green.
