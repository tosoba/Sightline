Type: task
Status: resolved
Blocked by: 02, 03

## Question

Write the migration spec.

Map the agreed target categories to the chosen API's queries.
Specify the resulting model and repository changes: the redesigned `PlaceCategory` enum, `Place` reduced to id/name/latitude/longitude/category (tags removed), the new id type, and removal of the Overpass module.
The chosen API is photon (keyless, raw OSM tags), with Geoapify as the keyed fallback (see API choice).
Include attribution requirements (ODbL / OpenStreetMap contributors for photon; "Powered by Geoapify" for the fallback).

## Answer

Resolved 2026-11-16.

The migration spec is written at [spec.md](../spec.md).

It covers: the photon reverse query shape; the 13-category -> OSM tag mapping; model changes (13-value `PlaceCategory`, tag-less `Place` with a String id, removed extensions); repository changes (one photon request per category, nearest-N semantics); module changes (delete `api/overpass`, add `api/photon`); Geoapify fallback equivalents; attribution; and open verification items (osm_tag OR-vs-AND semantics, principal-tag searchability, exact Geoapify keys).
