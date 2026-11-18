# 19: Convert api:photon to KMP (Ktor + GeoJSON DTOs)

**Spec:** `.scratch/kmp-ios-migration/specs/networking.md`

**What to build:** The photon reverse search runs over Ktor with hand-written `@Serializable` GeoJSON DTOs instead of Retrofit/OkHttp/mapbox-geojson, so the search is shareable. gson and mapbox-geojson are removed. `PhotonApiTest` moves from MockWebServer to Ktor `MockEngine`.

**Blocked by:** 18 (common KMP)

**Status:** ready-for-agent

- [ ] `PhotonApi` is a Ktor `HttpClient` wrapper with `search` and `reverse` in `commonMain`.
- [ ] GeoJSON DTOs are `@Serializable` with `properties` as `JsonObject` and `ignoreUnknownKeys`.
- [ ] Photon encoding matches: repeated `osm_tag` params, `radius` in km, `expectSuccess`, `longOrNull` for `osm_id`.
- [ ] `PhotonApiTest` runs on Ktor `MockEngine`; Android search results are unchanged.
