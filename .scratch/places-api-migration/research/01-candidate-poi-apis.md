# Candidate POI/Places APIs for Sightline

Researched 2026-11-16 against vendor pricing and documentation pages (primary sources).
The goal is to replace the rate-limited public Overpass API with a hosted HTTP API that fetches named places by category within a radius around a point.

## Framing: two families of candidates

The public Overpass API is excluded, and so are Overpass mirrors.
But OSM-derived data is not excluded.
The candidates split into two families.

OSM-data search APIs (in scope, not Overpass mirrors):

- Nominatim (nominatim.openstreetmap.org)
- photon (komoot, photon.komoot.io)
- Geoapify Places API (OSM-backed)
- LocationIQ (OSM/Nominatim-backed)
- OpenRouteService POIs (HeiGIT, OSM-backed)

Proprietary POI databases:

- Foursquare Places API
- Google Places API (New)
- HERE Places (Geocoding & Search)
- TomTom Places Search API
- Mapbox Search Box / Places
- Yelp Places API (formerly Yelp Fusion)
- Azure Maps Search

## Comparison table

| API | Key required | Free tier (2026) | Hard limits | Data source | Category-radius query | Attribution |
| --- | --- | --- | --- | --- | --- | --- |
| Nominatim | No (keyless) | Fully free | Max 1 req/sec absolute | OSM | No radius param; viewbox+bounded only; geocoder, not a category downloader | ODbL, "Data (c) OpenStreetMap contributors" |
| photon (komoot) | No (keyless demo) | Fully free, best-effort | Throttled/banned if heavy; no SLA | OSM (Nominatim import) | osm_tag / include-exclude category + bbox + location bias; reverse+radius+osm_tag gives nearest N | ODbL |
| Geoapify | Yes (free, no card) | 3,000 credits/day | 5 req/sec soft | OSM | categories= + filter=circle:lon,lat,radiusMeters | "Powered by Geoapify" + OSM |
| LocationIQ | Yes (free) | 5,000 req/day | 2 req/sec, 60 req/min | OSM (Nominatim) | Nearby POI endpoint, tag=amenity:restaurant etc | "Search by LocationIQ.com" link-back |
| OpenRouteService | Yes (free) | POIs ~500 to 2,500/day | per-minute limits | OSM (openpoiservice) | POIs endpoint, geometry + category_group_ids/category_ids | OSM + ORS |
| Foursquare | Yes | 500 free Pro calls/month | 50 QPS (sandbox/PayGo) | Proprietary, 100M+ POIs | query + ll&radius + fsq_category_ids | Foursquare logo + "Powered by Foursquare" |
| Google Places (New) | Yes + billing | Nearby Search Pro 5,000/month free | per-SKU quotas | Proprietary (Google) | Nearby Search (New), includedTypes | Google logo + "Powered by Google" |
| HERE Places | Yes + card | 5,000 Places Search/month free (30k geocoding) | ~5 req/sec freemium | Proprietary (HERE) | Browse/Discover endpoints, categories | HERE attribution |
| TomTom Places | Yes (no card) | Discover 5,000 calls/month | per-endpoint quotas | Proprietary (TomTom Orbis) | Discover endpoint, categorySet | TomTom attribution |
| Mapbox | Yes | Search Box /category 25k to 50k/month free | per-API quotas | Proprietary + OSM blend | Search Box /category endpoint | Mapbox + (c) OpenStreetMap |
| Yelp Places | Yes | None (30-day trial only) | daily caps | Proprietary (Yelp) | search + categories + location | Yelp branding + "Powered by Yelp" |
| Azure Maps | Yes + Azure account | Search 5,000 tx/month free | QPS | Proprietary (TomTom + others) | Search Near a Point + category | Microsoft attribution |

## OSM-data search APIs (in scope)

### Nominatim

Nominatim is keyless and free, but it is a geocoder, not a category-radius POI downloader.
Its usage policy caps usage at an absolute maximum of 1 request per second, which is stricter than a daily quota.
Source: https://operations.osmfoundation.org/policies/nominatim/

The search API takes free-form or structured queries and returns the best matches, not all objects of a type in an area.
The docs state explicitly that "special phrases are not suitable to query all objects of a certain type in an area. To download OSM data by object type, use the Overpass API."
Source: https://nominatim.org/release-docs/latest/api/Search/

It has a viewbox (boost) plus bounded (filter) pair, which behaves as a bounding box, not a radius.
Its layer filter (address, poi, railway, natural, manmade) and category/type fields expose raw OSM tags rather than a curated tourist taxonomy.
Attribution is ODbL share-alike with "Data (c) OpenStreetMap contributors, ODbL 1.0".
Source: https://nominatim.org/release-docs/latest/api/Search/

Verdict: in scope but a poor fit for "all named places of category X within radius".
It is more useful as a name/address resolver than as an Overpass replacement.

### photon (komoot)

photon is an open-source geocoder over OSM data, built on a Nominatim import and served by komoot at photon.komoot.io.
The demo server is keyless and free for "reasonable" request volumes, but extensive usage is throttled or banned, and there are no availability guarantees.
Source: https://github.com/komoot/photon

It filters by raw OSM tag and value via the osm_tag parameter, for example osm_tag=tourism:museum, and by bounding box via bbox.
It also supports include/exclude category filters and a location bias (lat/lon/zoom).
The reverse endpoint accepts a radius in kilometers and, combined with osm_tag and limit, returns the nearest N places of a given type.
Source: https://github.com/komoot/photon/blob/master/docs/api-v1.md

Categories are expressed as raw OSM keys/values, not a curated tourist taxonomy, so Sightline would map its own categories onto OSM tags.
The response is GeoJSON with name, osm_key, osm_value, osm_type, and osm_id.
Attribution is ODbL, and the project is Apache-2.0 licensed and self-hostable as a fallback.
Source: https://github.com/komoot/photon

Verdict: the strongest keyless candidate, with OSM data and category filtering, but it needs OSM-tag mapping and has no SLA.

### Geoapify Places API

Geoapify's Places API queries places by category within a radius, and it uses OpenStreetMap as the data source.
The docs state "We use the OpenStreetMap as the data source. Cache/store results with no limits."
Source: https://apidocs.geoapify.com/docs/places/

A key is required, but it is free and needs no credit card.
The free plan is 3,000 credits per day with up to 5 requests per second, and one Places API request costs one credit.
Source: https://www.geoapify.com/pricing/

Commercial use is allowed on the free plan, provided you stay within quota and display the required Geoapify and data-source attribution.
Attribution is a "Powered by Geoapify" follow-link near the map or information.
Source: https://www.geoapify.com/pricing/

The category taxonomy is the richest of any candidate, with 400+ hierarchical keys.
Tourist-relevant top-level groups include accommodation, catering (restaurants, cafes, bars, fast food, with cuisine subtypes), entertainment (museum, theme park, aquarium, zoo, cinema), tourism (attraction, sights.castle, viewpoint), natural (mountain, beach, forest), leisure (park), commercial (shopping, market), national_park, and heritage.
Source: https://apidocs.geoapify.com/docs/places/

The query is categories=<key> plus a spatial control, for example filter=circle:lon,lat,radiusMeters to return all matching places inside a circle, or bias=proximity:lon,lat to rank by distance.
The response is a GeoJSON FeatureCollection whose features carry name, lat/lon, distance, categories, and place_id.
Source: https://apidocs.geoapify.com/docs/places/

Verdict: the best category-and-radius fit among OSM-data APIs, and a strong overall pick if a free key is acceptable.

### LocationIQ

LocationIQ is an OSM/Nominatim-based geocoding and maps platform.
A key is required, and the free plan gives 5,000 requests per day at 2 requests per second.
Source: https://locationiq.com/pricing

The free plan allows commercial use only with a link-back attribution in the form "Search by LocationIQ.com".
Free accounts may cache request-response pairs for up to 48 hours.
The vendor claims responses within about 100ms from US and EU datacenters.
Source: https://locationiq.com/pricing

Its Nearby Points of Interest endpoint filters by OSM tag, such as tag=amenity:restaurant, around a point.
Source: https://docs.locationiq.com/ (Nearby Points of Interest endpoint; direct endpoint URL changed during research)

Verdict: a generous OSM-based daily quota with a simple nearby-by-tag model, but categories are raw OSM tags and commercial free use needs a visible link-back.

### OpenRouteService POIs (HeiGIT)

OpenRouteService is a routing and geospatial API by HeiGIT, built on OpenStreetMap data.
A free key is required, with no credit card.
The free Standard plan gives a daily and per-minute quota per endpoint; the POIs endpoint is about 500 to 2,500 requests per day depending on the current plan revision.
Source: https://openrouteservice.org/plans/ and https://account.heigit.org/info/plans

The POIs endpoint (openpoiservice) returns points of interest inside a geometry such as a bounding box, polygon, or buffered point, filtered by category_group_ids and category_ids.
Categories are a curated OSM-derived taxonomy.
Source: https://openrouteservice.org/dev/ (POIs endpoint)

Attribution to OSM and OpenRouteService is required.
Verdict: viable OSM-based POI search by category and geometry, but it is a routing-first platform with a modest POI quota.

## Proprietary POI databases

### Foursquare Places API

Foursquare's Places API sits on a proprietary database of 100M+ POIs across 200+ countries.
Source: https://foursquare.com/products/places-api

Effective June 1, 2026, the free allowance was cut to 500 free Pro calls per month, down from the earlier 10,000, with Pro endpoints at $15.00 per 1,000 calls after that.
Premium endpoints (tips, photos) have no free tier and start at $18.75 per 1,000.
Source: https://docs.foursquare.com/developer/reference/upcoming-changes and https://location.foursquare.com/pricing/

The Place Search endpoint filters by query, ll and radius, and fsq_category_ids using Foursquare's rich category taxonomy.
Source: https://docs.foursquare.com/fsq-developers-places/reference/place-search

Rate limits are 50 queries per second for Pay As You Go and sandbox accounts.
Foursquare's terms bar using the service to build or improve any point-of-interest or venue dataset or algorithm.
Source: Foursquare developer terms and rate-limit docs.

Verdict: rich data and taxonomy, but the free tier is now far too small for even light use, and premium fields are paid.

### Google Places API (New)

Google Places requires an API key and a billing-enabled Google Cloud project.
The Nearby Search (New) Pro SKU has a free usage cap of 5,000 per month, then costs $32.00 per 1,000 calls.
Place Details Pro costs $17.00 per 1,000, and Essentials (ID-only) searches are unlimited.
Source: https://developers.google.com/maps/billing-and-pricing/pricing

Nearby Search (New) filters by includedTypes and location, and field masks control which fields are billed.
Attribution requires the Google logo and a "Powered by Google" notice.
Source: https://developers.google.com/maps/documentation/places/web-service/overview and usage-and-billing pages.

Verdict: the most comprehensive proprietary data, but it requires a billing account and per-call pricing that does not fit a keyless, light-use preference.

### HERE Places (Geocoding & Search)

HERE requires an API key plus a payment method on file for its Base plan.
The Base plan includes about 5,000 free Places Search (Discover) transactions per month, with 30,000 free geocoding transactions, and a freemium rate limit around 5 requests per second.
Source: https://developer.here.com/pricing and https://freetier.wiki/services/here/ (transcribed from here.com/get-started/pricing)

The Places Browse/Discover endpoints filter by category and radius around a point.
HERE data is proprietary, around 120M places, and standard plans prohibit permanent storage of results without an enterprise caching license.
Source: HERE pricing and Geocoding & Search documentation.

Verdict: solid proprietary coverage and a real free allowance, but the card requirement and no-storage terms are friction for a free app.

### TomTom Places Search API

TomTom requires a key but no credit card for its free tier.
The Places Search API Discover endpoint has 5,000 free calls per month, with volume tiers above that.
Source: https://developer.tomtom.com/store/maps-api

The Discover endpoint returns places with attributes and supports a category set, using TomTom's proprietary Orbis Maps data.
Attribution to TomTom is required.
Source: https://developer.tomtom.com/places-search-api/

Verdict: a clean proprietary category search with a no-card free tier, though 5,000 calls per month is modest.

### Mapbox Search Box / Places

Mapbox requires a key.
The Search Box API /category endpoint (category POI search around a point) is free for the first 25,000 to 50,000 requests per month, then about $1.00 to $1.70 per 1,000 depending on the pricing stage.
Source: https://www.mapbox.com/pricing

The Temporary Geocoding API has a separate 100,000 free requests per month, but that is name/address geocoding, not category-radius POI search.
Temporary geocoding results may not be cached.
Source: https://www.mapbox.com/pricing

Mapbox blends proprietary and OSM-derived place data, and attribution requires the Mapbox logo plus "(c) Mapbox (c) OpenStreetMap".
Source: https://docs.mapbox.com/api/search/

Verdict: a generous free category-search allowance with a proprietary-plus-OSM taxonomy, but keyed and with caching restrictions on some products.

### Yelp Places API (formerly Yelp Fusion)

Yelp's developer API has moved from the formerly free Fusion program to paid Yelp Places API plans.
There is no ongoing free tier now, only a 30-day trial with 5,000 free calls.
Paid plans start at $229 per month (Base), rising to $643 per month (Premium).
Source: https://fusion.yelp.com/ (redirects to business.yelp.com Places API pricing)

Coverage is strong in the US and weaker internationally, which is a poor fit for a global tourist app.
Attribution requires Yelp branding and a "Powered by Yelp" notice.
Source: https://docs.developer.yelp.com/

Verdict: excluded for this use case; no free tier and US-centric coverage.

### Azure Maps Search

Azure Maps requires an Azure account and key.
The Search service has 5,000 free transactions per month, then per-1,000 pricing set by agreement (contact sales for the exact rate).
Source: https://azure.microsoft.com/en-us/pricing/details/azure-maps/

Its Search Near a Point endpoint filters by category, using a proprietary dataset that combines TomTom and other sources.
Source: https://learn.microsoft.com/en-us/rest/api/maps/search

Verdict: viable if the team already has an Azure account, but it adds subscription overhead and opaque per-1,000 pricing for a free app.

## Shortlist for a light-use, tourist-focused, keyless-preferred Android app

1. Geoapify Places API - the best category-and-radius fit, with a 400+ key tourist taxonomy, OSM data you may cache, a free 3,000-requests-per-day tier, and a free no-card key. Pick this if a free API key is acceptable (the map treats a key as a preference, not a gate).

2. photon (komoot) - the best keyless option, OSM-backed with osm_tag and category filters, self-hostable as a fallback, but raw OSM tags require mapping and there is no SLA.

3. LocationIQ - the most generous daily free tier at 5,000 requests per day with a simple nearby-by-OSM-tag model, keyed, with link-back attribution for commercial free use.

4. TomTom Places Search Discover - the strongest proprietary fallback with a clean category set and a no-card 5,000-calls-per-month free tier.

5. Mapbox Search Box /category - a generous 25k to 50k free category searches per month with a proprietary-plus-OSM taxonomy, keyed.

Nominatim is in scope but is a geocoder, not a category-radius downloader, so it is not a fit for the primary query pattern.
Google Places, Foursquare, HERE, Yelp, and Azure Maps are all usable but carry billing, card, or storage restrictions that conflict with a keyless-preferred free app.

## Sources

- Nominatim usage policy: https://operations.osmfoundation.org/policies/nominatim/
- Nominatim search API docs: https://nominatim.org/release-docs/latest/api/Search/
- photon README and API docs: https://github.com/komoot/photon and https://github.com/komoot/photon/blob/master/docs/api-v1.md
- Geoapify pricing: https://www.geoapify.com/pricing/
- Geoapify Places API: https://apidocs.geoapify.com/docs/places/
- LocationIQ pricing: https://locationiq.com/pricing
- OpenRouteService plans: https://openrouteservice.org/plans/ and https://account.heigit.org/info/plans
- Foursquare upcoming changes and pricing: https://docs.foursquare.com/developer/reference/upcoming-changes and https://location.foursquare.com/pricing/
- Foursquare Place Search: https://docs.foursquare.com/fsq-developers-places/reference/place-search
- Google Places pricing: https://developers.google.com/maps/billing-and-pricing/pricing
- HERE pricing: https://developer.here.com/pricing
- TomTom pricing: https://developer.tomtom.com/store/maps-api
- Mapbox pricing: https://www.mapbox.com/pricing
- Yelp Places API pricing: https://fusion.yelp.com/
- Azure Maps pricing: https://azure.microsoft.com/en-us/pricing/details/azure-maps/
