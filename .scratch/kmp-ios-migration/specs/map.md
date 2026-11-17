# Map rendering (iOS)

Groundwork spec for the iOS map.

Source: issue 02-map-rendering-ios.md (overridden by 07), issue 07-target-shared-architecture.md.

## Decision

MapLibre Native for iOS, wrapped in SwiftUI via a stable `UIViewRepresentable` around `MLNMapView`.
Chosen over Apple MapKit for cross-platform style consistency: the existing MapLibre style JSONs (`core/ui/src/main/res/raw/dark_style.json`, `light_style.json`) carry over to iOS.
The pre-1.0 first-party MapLibre SwiftUI DSL is optional and not required.

## Fallback

Apple MapKit for SwiftUI, if MapLibre Native proves awkward in practice.
MapKit is free and key-less but loses custom style JSON and cluster bubbles, which is acceptable under the feature/value parity bar.

## Current

Android uses MapLibre Compose (`org.maplibre.compose:maplibre-compose` + Vulkan runtime), surfaced through `core/ui/MapPreview.kt` with the two style JSON resources.

## Target design

- Map UI stays fully platform-native on each side; no KMP map abstraction.
- The shared value is the domain data already in pure-Kotlin `core:model`: `Place`, `MapCameraPosition`, and the bounding-box math (move the pure part of `core/ui/MapPlacesBoundingBox.kt` to commonMain).
- iOS maps the Android features: user location, place markers, bounding-box fit, and center/zoom/bearing/tilt camera.
- Bundle the two style JSONs in `iosApp` and load them via MapLibre's style URL/file.

## Notes

- Clustering, if it becomes a hard requirement, needs the lower-level MapLibre cluster layer or `MKMapView` (not the SwiftUI `Map`).
- Offline tiles are not app-controlled on iOS; acceptable for a light personal-use app.

## Out of scope

- A KMP map abstraction (map UI is native per platform).
- Mapbox Maps SDK (proprietary ToS, account + token + MAU billing).
