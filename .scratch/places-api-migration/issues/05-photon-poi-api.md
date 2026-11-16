Type: task
Status: resolved

## Goal

Extend the existing photon API module so it can issue category-radius POI queries, as the transport for the Overpass -> photon migration.

## Context pointers

- Spec: `.scratch/places-api-migration/spec.md` (sections "photon query shape" and "photon response mapping")
- Verification: `.scratch/places-api-migration/research/02-photon-tag-verification.md`

## Background

`api/photon` already talks to `https://photon.komoot.io/` via Retrofit and returns a mapbox `FeatureCollection`.
It is currently used for reverse geocoding and forward search (`core/data/AddressNetworkRepository`).
This ticket only adds query parameters for category-radius POI queries. It does NOT touch the model or the repository.

## Requirements

1. Extend `PhotonApi.reverse` to accept:
   - `radius`: `Double` in kilometres, optional (photon docs: 0..5000 km).
   - `osm_tag`: a list of `key:value` strings, optional. Retrofit must repeat the `osm_tag` query parameter once per element (OR semantics - see verification note).
   Keep the existing `lat`, `lon`, `limit`, `lang` params and their defaults so current callers (`AddressNetworkRepository`) are unaffected.
2. Keep the return type as `FeatureCollection`. Mapping to `Place` is a `core/data` concern handled by a later ticket.
3. Add a unit test that asserts the reverse request encodes `radius` and repeats `osm_tag` once per element. Use OkHttp MockWebServer (add the `mockwebserver` dependency matching the okhttp version to `gradle/libs.versions.toml` and the module) or an equivalent interceptor-based assertion. Do not make live network calls in the test.

## Out of scope

- Model changes, repository changes, deleting the overpass module, attribution.
- Mapping the response to `Place`.

## Resolution

Resolved 2026-11-16. Commit `1806ebe` on `integration/places-api-migration`.
Added `radius` (km) and `osmTag: List<String>` params to `PhotonApi.reverse`, plus a MockWebServer test asserting request encoding.
