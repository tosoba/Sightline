## Destination

Pick a hosted HTTP POI API - a generous free tier or keyless - that serves Sightline's tourist/exploration categories (name, latitude, longitude, id), replacing the rate-limit-plagued public Overpass instance.
Then write a migration spec mapping the redesigned category set to that API's queries.
The map stops at the spec; implementation is a separate effort.

## Notes

- Sightline is a tourist/exploration AR app.
  Categories are designed for that audience, not preserved from the current OSM-tag mapping.
- Current implementation: `PlacesNetworkRepository` queries the public Overpass API with OSM tag queries (node-only, named-only).
  The pain is rate limiting / 429s, not raw response latency.
- Dropped in this effort: the UI subtitle (tourism/leisure tags) and formatted address (addr:* tags).
  `Place.tags` and its extensions go away; `Place` reduces to id, name, latitude, longitude, category.
- API key is a preference, not a gate.
  Hosted HTTP API only - no self-hosted Overpass, no Overpass mirrors, no bulk datasets (e.g. Overture).
- Usage is light personal use; a modest free tier suffices.
- Ordering is product-first: decide the target categories, then choose the API.
- Skills every session should consult: grilling, domain-modeling, research.

## Decisions so far

- [Candidate POI APIs](issues/01-candidate-poi-apis.md): two families in scope - OSM-data search (Geoapify, photon, LocationIQ, OpenRouteService) and proprietary (TomTom, Mapbox, Google, HERE, Foursquare, Yelp, Azure). Shortlist: Geoapify, photon, LocationIQ, TomTom, Mapbox. Foursquare/Yelp free tiers gutted; Nominatim is a geocoder, not a radius downloader.
- [Target categories](issues/02-target-categories.md): 13 categories - 7 discovery (Sights & landmarks, Arts & culture, Food & drink, Nightlife, Shopping, Nature & outdoors, Library) + 6 essentials (Accommodation, Getting around, Road trip, Money & banking, Health, Practical). Nine locals-oriented categories dropped.
- [API choice](issues/03-api-choice.md): photon (public keyless) primary, Geoapify keyed fallback. OSM-backed; the 13 categories still map to raw OSM tags.
- [Migration spec](issues/04-migration-spec.md): spec written at [spec.md](spec.md) - photon query shape, 13-category OSM-tag mapping, model and module changes, Geoapify fallback, attribution.

## Not yet specified

<!-- empty: coverage verification is moot (photon is OSM-backed, same coverage as today) and attribution is now specified (ODbL/OSM, folded into the migration spec). -->

## Out of scope

- Self-hosted Overpass and any Overpass mirror.
- Bulk-data sources like Overture Maps (hosted HTTP API only).
- Preserving the subtitle and address fields.
- Caching, offline-first, and rate-limit mitigation beyond the API choice itself.
- Implementing the migration - this map stops at the spec.
