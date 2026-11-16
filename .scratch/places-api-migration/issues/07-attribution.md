Type: task
Status: resolved
Blocked by: 06

## Goal

Display the required OpenStreetMap/photon attribution in the UI.

## Context pointers

- Spec: `.scratch/places-api-migration/spec.md` (section "Attribution")

## Requirements

photon data is ODbL-licensed; attribution must read "Data (c) OpenStreetMap contributors" and reference ODbL.

1. Add a small, unobtrusive attribution line where place data is shown. The places list (`feature/places` `PlacesContent`) or the category screen is the natural home.
2. Use the exact required text. Do not use an em dash; use a plain dash if a dash is needed.
3. Keep it visually minimal (body/label style, muted colour).

## Out of scope

- The Geoapify fallback attribution ("Powered by Geoapify") - only needed if and when the fallback is wired.

## Resolution

Resolved 2026-11-16. Commit `2857d11` on `integration/places-api-migration`.
Added the OSM attribution line to the places sheet.
