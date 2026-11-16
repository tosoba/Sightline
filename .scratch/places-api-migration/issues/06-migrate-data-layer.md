Type: task
Status: resolved
Blocked by: 05

## Goal

Switch Sightline's place search from Overpass to photon, end to end: model, repository, module wiring, and UI consumers.
This is an atomic change - the model, repository, and consumers change together, and the overpass module is removed.

## Context pointers

- Spec: `.scratch/places-api-migration/spec.md` (authoritative; sections "Model changes", "Repository changes", "Module changes", "Category -> OSM tag mapping")
- Verification: `.scratch/places-api-migration/research/02-photon-tag-verification.md`
- Prior work: `PhotonApi.reverse` now accepts `radius` + `osm_tag` (ticket 05)

## Model changes (core/model, core/common)

1. `PlaceCategory`: replace the 31 values with these 13, in this order (order matters for the UI pager):
   `SightsLandmarks`, `ArtsCulture`, `FoodDrink`, `Nightlife`, `Shopping`, `NatureOutdoors`, `Library`, `Accommodation`, `GettingAround`, `RoadTrip`, `MoneyBanking`, `Health`, `Practical`.
   Keep the existing `label` getter unchanged.
2. `Place`: change `id` to `String`, remove the `tags` field. Resulting fields: `id`, `name`, `latitude`, `longitude`, `category`. Keep `@Serializable` and the `equals`/`hashCode` keyed on `id`.
3. `PlaceExtensions` (core/common): remove `tourismOrLeisure` and `formattedAddress`. Keep `Place.location`.

## UI consumers of the model

4. `core/ui/PlaceCategoryExtensions.kt`: rewrite the `icon` mapping for the 13 new categories, one Material icon per category (e.g. SightsLandmarks -> `Place`, ArtsCulture -> `Museum` or `TheaterComedy`, FoodDrink -> `Restaurant`, Nightlife -> `Nightlife`, Shopping -> `Storefront`, NatureOutdoors -> `Park` or `Terrain`, Library -> `LocalLibrary`, Accommodation -> `Hotel`, GettingAround -> `DirectionsBus`, RoadTrip -> `DirectionsCar`, MoneyBanking -> `AccountBalance`, Health -> `LocalHospital`, Practical -> `Wc`). Remove now-unused icon imports.
5. `feature/places/PlacesContent.kt`: update the `showInRowLayout` extension for the new categories. Pick a sensible 3-4 category subset for the compact row layout (e.g. SightsLandmarks, FoodDrink, Accommodation, Shopping).
6. `feature/place-category/PlaceCategoryScreen.kt`: remove the `tourismOrLeisure` subtitle suffix and the `formattedAddress` address line. The place name is now shown alone. Remove the two now-unused imports.
7. `core/ar/view/ARMarkerRenderer.kt`: remove the `tourismOrLeisure` subtitle suffix (display name is just the place name). `place.id` is now a `String`, so the `pagedMarkers` / `lastDrawnMarkerIds` / `renderedMarkerIds` collections must switch from `Long` keys to `String` keys. Remove the `tourismOrLeisure` import.

## Repository changes (core/data)

8. `core/data/PlacesNetworkRepository.kt`: reimplement `fetchPlaces` against `PhotonApi` (inject `PhotonApi` instead of `OverpassApi`):
   - Build one photon `reverse` request per category using the category's OSM tag list (mapping below).
   - Convert `radiusMeters` to kilometres by dividing by 1000.
   - Use `limit = 100` (nearest-N within radius semantics).
   - Map each returned feature to a `Place`:
     - `id = "${osm_type}:${osm_id}"` (`osm_type` is `N`/`W`/`R`, `osm_id` is a number).
     - `name = properties.name`.
     - `latitude = geometry.coordinates[1]`, `longitude = geometry.coordinates[0]`.
     - `category` = the requested category.
   - Filter out features with a blank name.
   - Deduplicate by `id` (one request uses multiple alternative tags, so an object matching several tags can be returned more than once).
9. Add the category -> OSM tag mapping in `core/data` (not on the domain model). Use the exact tag lists from the table below; do not invent or drop tags (verification confirmed all are principal tags).
10. Add the photon feature -> `Place` mapping, either in `FeatureMapper.kt` or a new mapper in `core/data`.

## Module changes

11. Delete the `api/overpass` module entirely: `OverpassApi`, `OverpassApiModule`, the Java query-builder models, the response models, the test, the `overpass.email` BuildConfig field in its `build.gradle.kts`, and its `.gitignore`.
12. `settings.gradle.kts`: remove `include(":api:overpass")`.
13. `core/data/build.gradle.kts`: remove `implementation(project(":api:overpass"))`. `implementation(project(":api:photon"))` is already present.
14. `app/build.gradle.kts`: remove `implementation(project(":api:overpass"))`. Do not add `:api:photon` to the app module (it is not listed today; Hilt discovers `PhotonApiModule` transitively via `core:data`).
15. `README.md`: remove the `overpass.email` setup instructions (the "Setup" paragraph and the `overpass.email=...` line), and update the "Used technologies" Retrofit bullet so it no longer references Overpass (Moshi can be dropped from the list only if no module still uses it).

## Category -> OSM tag mapping

Each category is one photon `reverse` request with a list of alternative `osm_tag` values.

| Category | OSM tags (osm_tag params) |
| --- | --- |
| SightsLandmarks | `tourism:attraction`, `tourism:viewpoint`, `tourism:artwork`, `tourism:aquarium`, `tourism:theme_park`, `tourism:zoo`, `historic:monument`, `historic:memorial`, `historic:castle`, `historic:fort`, `historic:ruins`, `historic:archaeological_site`, `historic:city_gate`, `historic:tower` |
| ArtsCulture | `tourism:museum`, `tourism:gallery`, `amenity:theatre`, `amenity:cinema` |
| FoodDrink | `amenity:restaurant`, `amenity:cafe`, `amenity:bar`, `amenity:biergarten`, `amenity:pub`, `amenity:fast_food`, `amenity:food_court`, `amenity:ice_cream` |
| Nightlife | `amenity:nightclub`, `amenity:casino` |
| Shopping | `shop:mall`, `shop:department_store`, `shop:supermarket`, `shop:general`, `shop:kiosk`, `shop:wholesale`, `amenity:marketplace` |
| NatureOutdoors | `leisure:park`, `leisure:garden`, `leisure:nature_reserve`, `leisure:playground`, `leisure:pitch`, `leisure:sports_centre`, `leisure:swimming_pool`, `leisure:golf_course`, `leisure:marina`, `natural:beach`, `natural:peak`, `natural:volcano`, `natural:cliff`, `natural:valley`, `natural:water`, `tourism:alpine_hut`, `tourism:wilderness_hut` |
| Library | `amenity:library` |
| Accommodation | `tourism:hotel`, `tourism:hostel`, `tourism:guest_house`, `tourism:motel`, `tourism:apartment`, `tourism:chalet`, `tourism:alpine_hut` |
| GettingAround | `railway:station`, `railway:halt`, `railway:tram_stop`, `railway:subway_entrance`, `amenity:bus_station`, `amenity:taxi`, `amenity:bicycle_rental` |
| RoadTrip | `amenity:fuel`, `amenity:charging_station`, `amenity:parking`, `amenity:car_rental`, `amenity:car_sharing` |
| MoneyBanking | `amenity:atm`, `amenity:bank`, `amenity:bureau_de_change` |
| Health | `amenity:pharmacy`, `amenity:hospital`, `amenity:clinic` |
| Practical | `amenity:toilets` |

## Tests

- A unit test for the photon feature -> `Place` mapping (given feature properties/geometry, produce the expected `Place`, including the `${osm_type}:${osm_id}` id).
- A unit test for the category -> OSM tag mapping (all 13 categories map to a non-empty list matching the table).
- A unit test for the radius conversion (radiusMeters -> kilometres).

## Definition of done

- `:app:assembleDebug` succeeds.
- Unit tests pass.
- No references remain to `api.overpass`, `OverpassApi`, `tourismOrLeisure`, `formattedAddress`, or the old `PlaceCategory` values.

## Resolution

Resolved 2026-11-16. Commit `e296f2e` on `integration/places-api-migration`.
Model, repository, module deletion, and UI consumers migrated from Overpass to photon.
