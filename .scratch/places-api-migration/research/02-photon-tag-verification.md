# photon tag verification

Verified 2026-11-16 against photon's API docs and Nominatim's `presets.lua` (primary sources).
Resolves the spec's "Open items for implementation".

## osm_tag OR/AND semantics

photon's API docs state: "If one or many query parameters named osm_tag are present, photon will attempt to filter results by those tags."
Multiple `osm_tag` params are OR-combined: a place is included if it matches any of the listed tags.
The spec's example (pharmacies and hospitals via two `osm_tag` params) is correct.

`osm_tag` is deprecated in favour of the `include`/`exclude` category filters using the `osm.<key>.<value>` category, but the direct `osm_tag` filter still works today.
The spec uses `osm_tag`; implement with `osm_tag`.

## Principal-tag searchability

photon's `osm_tag` filter only matches principal OSM tags.
The principal-tag list is Nominatim's `MAIN_TAGS` definition in `lib-lua/themes/nominatim/presets.lua`.

Analysed the spec's mapping against `MAIN_TAGS_POIS` (the POI import theme). Key rules:

- `amenity`, `shop`, `craft`, `office`, `club`, `military` -> `'always'`: every value is a principal tag, except a small exclusion list (`amenity`: `parking_space`, `parking_entrance`, `waste_disposal`, `hunting_stand`, `no`). None of the spec's amenity values are excluded.
- `tourism` -> `'always'` except `attraction = 'fallback'` (still principal), `information`, `yes`, `no`. All spec tourism values are principal.
- `historic` -> `'fallback'` for all values except `yes`/`no`. All spec historic values are principal.
- `leisure` -> `'always'` except `nature_reserve`/`swimming_pool`/`garden`/`common` are `'named'` (principal only when the object is named - irrelevant, the app only shows named places). All spec leisure values are principal.
- `natural` -> `'named'` (principal when named). All spec natural values are principal.
- `railway` -> `'named'` except `rail`/`no`/`abandoned`/`disused`/`razed`/`level_crossing`/`switch`/`signal`/`buffer_stop`. The spec's railway values (`station`, `halt`, `tram_stop`, `subway_entrance`) are principal.

Conclusion: every tag in the spec's 13-category mapping is a principal tag searchable by photon.
No tags need to be dropped.

## Geoapify fallback

The Geoapify fallback is documented in the spec as "when the fallback is wired" - out of scope for this migration.
Category keys would be verified against Geoapify's taxonomy at that point.
