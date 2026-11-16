Type: grilling
Status: resolved
Blocked by: 01, 02

## Question

Which candidate POI API replaces Overpass?

Evaluate the research shortlist against the agreed target categories and criteria (free tier, keyless preferred, rate limits, coverage, taxonomy fit, latency, attribution), and pick one.

## Answer

Resolved 2026-11-16.

Chosen: photon (public photon.komoot.io, keyless) as the primary POI API, with Geoapify Places API as the documented keyed fallback.

photon is a keyless OSM-backed search service.
It filters by raw OSM tag via osm_tag (e.g. osm_tag=tourism:museum) and returns places around a point via the reverse endpoint (radius in km plus limit = nearest N).
Data is OSM (Nominatim import), so coverage equals today's Overpass data.
Attribution is ODbL / OpenStreetMap contributors.

Geoapify fallback: 400+ key curated tourist taxonomy, true filter=circle radius, 3,000 req/day free, free no-card key, "Powered by Geoapify" attribution.

Decisions that shaped this: accept a free no-card key; prefer OSM-derived data over proprietary (cacheable, no storage restrictions); keyless preferred, so photon is primary; Geoapify documented as the fallback because photon has no SLA and throttles heavy use.

Implication: photon returns raw OSM tags, so the 13 categories still map to OSM tag lists (amenity/tourism/leisure/shop) in the app - the same pattern as today, different transport.
The migration spec must map the 13 categories to photon osm_tags and note Geoapify taxonomy equivalents.
