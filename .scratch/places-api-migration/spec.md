# Migration spec: Overpass -> photon

## Chosen API

- Primary: photon, public instance at `https://photon.komoot.io`, keyless.
- Fallback: Geoapify Places API, keyed free tier.

## photon query shape

- Endpoint: `GET https://photon.komoot.io/reverse`.
- Mandatory params: `lat`, `lon`.
- Optional params: `radius` (kilometres, float, 0 to 5000), `limit` (max results), and a category filter.
- Category filter: the `osm_tag` param is deprecated in favour of the `include`/`exclude` category filters, but both work today.
- `osm_tag` syntax is `key:value`; repeat the param to express alternatives.
- Example (pharmacies and hospitals near a point): `GET /reverse?lon=21.017532&lat=52.237049&radius=1&limit=100&osm_tag=amenity:pharmacy&osm_tag=amenity:hospital`.

Example response fields:

- `features[].properties.name` - place name.
- `features[].properties.osm_type` - `N`, `W`, or `R`.
- `features[].properties.osm_id` - OSM id (number).
- `features[].geometry.coordinates` - `[longitude, latitude]`.

## photon response mapping

- id: `"${osm_type}:${osm_id}"` as a String, unique across nodes, ways, and relations.
- name: `properties.name`.
- latitude: `geometry.coordinates[1]`.
- longitude: `geometry.coordinates[0]`.
- category: the requested category.
- Ignore `properties.extent`, address fields, `osm_key`, and `osm_value`.

## Category -> OSM tag mapping

Each category is one photon request with a list of alternative tags.

| Category | OSM tags (osm_tag params) |
| --- | --- |
| Sights & landmarks | `tourism:attraction`, `tourism:viewpoint`, `tourism:artwork`, `tourism:aquarium`, `tourism:theme_park`, `tourism:zoo`, `historic:monument`, `historic:memorial`, `historic:castle`, `historic:fort`, `historic:ruins`, `historic:archaeological_site`, `historic:city_gate`, `historic:tower` |
| Arts & culture | `tourism:museum`, `tourism:gallery`, `amenity:theatre`, `amenity:cinema` |
| Food & drink | `amenity:restaurant`, `amenity:cafe`, `amenity:bar`, `amenity:biergarten`, `amenity:pub`, `amenity:fast_food`, `amenity:food_court`, `amenity:ice_cream` |
| Nightlife | `amenity:nightclub`, `amenity:casino` |
| Shopping | `shop:mall`, `shop:department_store`, `shop:supermarket`, `shop:general`, `shop:kiosk`, `shop:wholesale`, `amenity:marketplace` |
| Nature & outdoors | `leisure:park`, `leisure:garden`, `leisure:nature_reserve`, `leisure:playground`, `leisure:pitch`, `leisure:sports_centre`, `leisure:swimming_pool`, `leisure:golf_course`, `leisure:marina`, `natural:beach`, `natural:peak`, `natural:volcano`, `natural:cliff`, `natural:valley`, `natural:water`, `tourism:alpine_hut`, `tourism:wilderness_hut` |
| Library | `amenity:library` |
| Accommodation | `tourism:hotel`, `tourism:hostel`, `tourism:guest_house`, `tourism:motel`, `tourism:apartment`, `tourism:chalet`, `tourism:alpine_hut` |
| Getting around | `railway:station`, `railway:halt`, `railway:tram_stop`, `railway:subway_entrance`, `amenity:bus_station`, `amenity:taxi`, `amenity:bicycle_rental` |
| Road trip | `amenity:fuel`, `amenity:charging_station`, `amenity:parking`, `amenity:car_rental`, `amenity:car_sharing` |
| Money & banking | `amenity:atm`, `amenity:bank`, `amenity:bureau_de_change` |
| Health | `amenity:pharmacy`, `amenity:hospital`, `amenity:clinic` |
| Practical | `amenity:toilets` |

## Model changes

- Replace the 31-value `PlaceCategory` enum with 13 values: `SightsLandmarks`, `ArtsCulture`, `FoodDrink`, `Nightlife`, `Shopping`, `NatureOutdoors`, `Library`, `Accommodation`, `GettingAround`, `RoadTrip`, `MoneyBanking`, `Health`, `Practical`.
- `Place`: id becomes String; fields are `id`, `name`, `latitude`, `longitude`, `category`; remove `tags`.
- Remove `PlaceExtensions.tourismOrLeisure` and `PlaceExtensions.formattedAddress`.
- Update UI consumers that read tags: `PlaceCategoryScreen` (subtitle and address lines) and `ARMarkerRenderer` (subtitle line).

## Repository changes

- `PlacesRepository.fetchPlaces(category, latitude, longitude, radiusMeters)` signature is unchanged.
- Reimplement the repository to build one photon reverse request per category and map the response to `List<Place>`.
- Convert `radiusMeters` to kilometres (divide by 1000).
- Use a `limit` (suggested 100) so results are nearest-N within the radius, a slight semantic change from "all within radius".

## Module changes

- Delete the `api/overpass` module: `OverpassApi`, `OverpassApiModule`, the Java query-builder models, and the `overpass.email` BuildConfig field.
- Add an `api/photon` module using the existing stack (Retrofit + Moshi + OkHttp) or kotlinx.serialization (already present in `core/model`), with a Hilt module providing the API.
- Update `core/data` dependency from `api/overpass` to `api/photon`.

## Fallback (Geoapify)

- Endpoint: `GET https://api.geoapify.com/v2/places?categories=...&filter=circle:lon,lat,radiusMeters&apiKey=...`.
- id is `place_id` (String).
- Category equivalents are approximate and must be verified against Geoapify's taxonomy when the fallback is wired: tourism and heritage keys for Sights, entertainment for Arts/Nightlife, catering for Food & drink, commercial for Shopping, natural and leisure and national_park for Nature & outdoors, accommodation for Accommodation, healthcare for Health, financial for Money & banking.

## Attribution

- photon: ODbL, "Data (c) OpenStreetMap contributors".
- Geoapify fallback: "Powered by Geoapify" plus OSM attribution.

## Open items for implementation

- Verify whether multiple `osm_tag`/`include` params combine with OR or AND semantics.
- Verify each tag value in the mapping is a principal tag searchable by photon; drop any that are not.
- Verify exact Geoapify category keys for the fallback.
