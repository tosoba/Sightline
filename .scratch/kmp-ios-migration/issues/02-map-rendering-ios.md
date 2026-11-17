Type: research
Status: resolved

## Question

Which map-rendering library should iOS use, under the free/open-source preference?

Android uses MapLibre Compose (`org.maplibre.compose:maplibre-compose` plus the Vulkan runtime), surfaced through `core/ui/MapPreview.kt`.
That library is Android-only.

Research and report:

- MapLibre Native for iOS: maintenance status, license, SwiftUI integration, offline and style capabilities.
- Apple MapKit: native, free, no API key, but style control and terms-of-service differences.
- Mapbox Maps SDK for iOS: capability-rich but commercial.
- Whether a KMP expect/actual map abstraction is worth building, or whether map UI should stay platform-native on each side.

Recommend one, with the trade-offs that matter for a light personal-use AR app.

## Answer

Recommend **Apple MapKit for SwiftUI**; keep map UI fully platform-native, no KMP expect/actual map abstraction.

- MapKit wins for this app: free, no API key, no billing/telemetry, and a first-class SwiftUI API covering user location (`UserAnnotation`), place markers (`Marker`/`Annotation`), bounding-box fit (`MapCameraUpdate`), and center/zoom/bearing/tilt (`MapCamera` + `MapCameraPosition`).
- Trade-off: fixed styles (`.standard`/`.imagery`/`.hybrid`), no custom style JSON, so Android's dark/light style JSON and cluster bubbles do not carry over - acceptable under the feature/value-parity bar.
  No app-controlled offline tiles; clustering needs `MKMapView` (not the SwiftUI `Map`) if it becomes necessary.
- MapLibre Native for iOS is the open-source fallback if style/cluster/offline parity becomes a hard requirement - actively maintained (BSD-2-Clause, iOS v7.0.0, 2026-10-28), official SwiftUI DSL but pre-1.0.
- Mapbox Maps SDK is ruled out: proprietary ToS, requires account + access token + telemetry opt-out, MAU billing.
- No KMP map abstraction: rendering is a per-platform UI concern; the shared value is the domain data (`Place`, `MapCameraPosition`, bounding-box math) already in pure-Kotlin `core:model`.

Full findings: [research/02-map-rendering-ios.md](research/02-map-rendering-ios.md).
