Type: research
Status: resolved

## Question

What hosted HTTP POI/places APIs can fetch named places by category within a radius around a point, and how do they compare?

Produce a shortlist with a comparison table covering, per API:

- Free tier generosity and hard daily/rate limits.
- API key required or keyless.
- Category taxonomy: does it express tourist-relevant categories (attractions, food/drink, nightlife, shopping, nature/outdoors, accommodation, transport) and how richly?
- Global coverage and data source (OSM-derived vs proprietary POI database).
- Response latency characteristics.
- Attribution and licensing terms.

Flag which candidates are OSM-data search APIs (e.g. Nominatim, photon) versus proprietary POI databases (e.g. Foursquare, Google Places, HERE, TomTom), since "no Overpass mirrors" does not automatically rule out OSM-data search APIs.

## Answer

Two candidate families are in scope: OSM-data search APIs (Geoapify, photon, LocationIQ, OpenRouteService, Nominatim) and proprietary POI databases (TomTom, Mapbox, Google Places, HERE, Foursquare, Yelp, Azure Maps).

Shortlist for a light-use, tourist-focused, keyless-preferred app:

- Geoapify Places API - richest tourist taxonomy (400+ keys), true radius filter, OSM data that may be cached, 3,000 free req/day, free no-card key.
- photon (komoot) - keyless, OSM, osm_tag category filter, self-hostable fallback, no SLA.
- LocationIQ - 5,000 req/day free, nearby-by-OSM-tag, keyed, link-back attribution.
- TomTom Places Discover - clean proprietary category set, 5,000 calls/month, no card.
- Mapbox Search Box /category - 25k-50k free category searches/month, keyed.

Key caveats: Foursquare cut its free tier to 500 calls/month (June 2026); Yelp has no ongoing free tier; Nominatim is a geocoder capped at 1 req/s, not a category-radius downloader; HERE and Google require a card or billing account.

Full comparison: [research/01-candidate-poi-apis.md](../research/01-candidate-poi-apis.md) (branch `research/places-api-migration`).
