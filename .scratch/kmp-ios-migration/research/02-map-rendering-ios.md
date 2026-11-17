# Research: map rendering on iOS

Ticket: `.scratch/kmp-ios-migration/issues/02-map-rendering-ios.md`

## Recommendation (TL;DR)

Use Apple MapKit for SwiftUI on iOS.
Keep the map UI fully platform-native (SwiftUI Map on iOS, Compose MapLibre on Android).
Do not build a KMP expect/actual map-rendering abstraction.

MapKit is free with no API key, no usage metering, and a first-class SwiftUI API.
It covers every requirement the map has today (user location, place markers, bounding-box camera fit, and center/zoom/bearing/tilt camera control).
The only real sacrifice is style control (no custom vector style JSON like the Android side's `dark_style`/`light_style`), which is acceptable under the parity bar (feature and value parity, not pixel parity).

MapLibre Native for iOS is the correct open-source fallback if matching the Android map's look or its clustering/offline behavior becomes a hard requirement.
It is actively maintained (BSD-2-Clause) and now has an official SwiftUI DSL.
It costs a dependency and a pre-1.0 DSL API for features MapKit already gives away for free.

Mapbox Maps SDK for iOS is ruled out.
It is proprietary (Mapbox ToS), requires an account and access token, and adds telemetry/attribution obligations for no capability this app needs.

---

## Context: what the iOS map must reproduce

The Android side renders maps through `core/ui/MapPreview.kt`, built on MapLibre Compose (`org.maplibre.compose:maplibre-compose` 0.20.0 plus `maplibre-compose-runtime-vulkan-android`).
The concrete requirements, from `MapPreview.kt` and `feature/map/MapScreen.kt`, are:

- Show the user's current location.
- Show a set of places as markers (with clustering).
- Show a place's bounding box and fit the camera to it.
- Adjust camera position: center, zoom, bearing, and pitch/tilt.
- Restore a saved camera position on resume.
- A local style JSON (`R.raw.dark_style` / `R.raw.light_style`) drives the base style.

Only the last two items (custom style JSON, and exact marker/cluster styling) are hard to reproduce pixel-for-pixel on MapKit.
Everything else is a one-to-one MapKit SwiftUI feature.

---

## 1. MapLibre Native for iOS

### Maintenance status

Actively maintained, and a "core project" of the MapLibre Organization (the two core platforms are Android and iOS).
Release cadence is steady through 2025 and 2026.
The iOS line moved to a new major version in late 2026:

- `ios-v7.0.0` released 2026-10-28.
- Pre-release globe-projection builds (`ios-v2026.11.2-globe`) followed in early November 2026.
- Android is on a separate per-platform line (13.x, e.g. 13.7.1).

iOS 7.0.0 breaking changes relevant to a new integration:

- Minimum supported iOS raised to 15.5.
- The `Mapbox.h` umbrella header was replaced with `MapLibre.h`; `MapLibre.xcframework` no longer ships `Mapbox.h` (a migration guide is provided).
- An experimental C plugin API for custom style layer types was added (separate `maplibre-ios-with-plugin-api` package).

Rendering on iOS uses Metal (the project originally supported OpenGL only; Metal landed first in 2023, and iOS has no Vulkan/OpenGL path, unlike Android which supports both Vulkan and OpenGL).

### License

BSD 2-Clause "Simplified" License, inherited from its fork of Mapbox GL Native.
This is permissive and is the key difference from Mapbox, whose native SDK switched to a proprietary license in December 2020.

### Renaming history (MapLibre GL Native → MapLibre Native)

The "GL" naming has been dropped across the project because rendering is no longer OpenGL-only.
Concretely:

- The GitHub repo was renamed from `maplibre/maplibre-gl-native` to `maplibre/maplibre-native`.
- The Objective-C/Swift class prefix changed from `MGL` to `MLN` (`MLNMapView`, `MLNStyle`, etc.).
- The Android resource prefix changed from `mapbox_` to `maplibre_`.
- The Swift module is imported as `import MapLibre` (the `Mapbox.h` header dropped entirely in iOS 7.0.0).
- The Swift Package Manager distribution still lives under the legacy repo name `maplibre/maplibre-gl-native-distribution`, so that URL remains the correct package URL for now.

### SwiftUI integration

The core iOS SDK is UIKit (`MLNMapView`).
You can wrap it manually with `UIViewRepresentable`, but there is now an official, MapLibre-hosted SwiftUI package:

- `maplibre/swiftui-dsl` ("MapLibreSwiftUI"), originally built by Stadia Maps and migrated into the MapLibre organization.
- Provides `MapLibreSwiftDSL` and `MapLibreSwiftUI` products.
- Declarative SwiftUI API for style layers, sources, markers, clustering, camera control, gestures, and animation.
- License: BSD-2-Clause.
- Status: pre-1.0, so the API is not yet committed to stability (breaking changes are announced in release notes).
- iOS-only at present (the macOS framework does not yet include it).

### Offline and style capabilities

- Offline: full support via `MLNOfflineStorage` (offline packs, ambient cache, and `MLNMapSnapshotter`).
- Style: complete MapLibre Style Spec JSON support.
  The same `dark_style`/`light_style` JSON files used by the Android side would load unchanged on iOS.
- Custom tile sources, custom layers, and data-driven styling all work.

### Assessment for this app

MapLibre Native for iOS is the closest functional and visual match to the Android side, and it is free/open-source.
Its cost is a dependency plus a pre-1.0 SwiftUI DSL (or a hand-written `UIViewRepresentable`), and the burden of sourcing and hosting a vector tile style (as the Android side already does).

---

## 2. Apple MapKit for SwiftUI

### Capabilities

MapKit for SwiftUI (`Map`, `MapContentBuilder`, `MapCameraPosition`, etc., introduced at WWDC23) covers every map requirement:

- Map view: `Map(position: $position) { ... }`.
- Markers: `Marker("label", coordinate:)` (platform pin look, VoiceOver label) and `Annotation` for fully custom SwiftUI content.
- User location: `UserAnnotation` plus the `MapUserLocationButton` control.
- Shapes/regions: `MapCircle`, `MapPolygon`, `MapPolyline` (with `StrokeStyle`).
- Camera: `MapCameraPosition` (`.camera`, `.region`, `.rect`, `.item`, `.userLocation`, `.automatic`) and `MapCamera` with `centerCoordinate`, `distance`, `heading`, and `pitch`.
  This maps directly onto the app's center/zoom/bearing/tilt requirement.
- Camera fit/animate: `MapCameraUpdate`, `withAnimation`-driven position changes, and `.onMapCameraChange(frequency:)` to observe the camera for save/restore.
- Search: `MKLocalSearch` and `MKLocalSearchCompleter` for place search and autocomplete (the app's Photon search already lives in shared code, but MapKit search is available if needed).
- Directions/routing: `MKDirections` (with a new `MKDirections.Request` API in iOS 26).

### Cost

Free.
Native MapKit requires no API key, no account, and no usage metering or MAU billing.
This is unlike MapKit JS (which has a free tier with daily limits) and unlike Mapbox.
There is no paid tier for the native SDK.

### Style control limits

MapKit exposes a small set of built-in styles via `.mapStyle`:

- `.standard`, `.standard(elevation:)`
- `.imagery`, `.imagery(elevation:)`
- `.hybrid`, `.hybrid(elevation:)`

It does not support arbitrary MapLibre/MVT style JSON, custom vector tile sources, or full data-driven layer styling.
Custom raster content is possible through `MKTileOverlay`, and markers/annotations can carry arbitrary SwiftUI content, but the base map look is fixed to Apple's styles.
The Android side's custom `dark_style`/`light_style` JSON has no direct MapKit equivalent.
The closest proxy is picking `.standard` vs `.imagery` and tinting markers with the app's color scheme.

Clustering is not built into the SwiftUI `Map` API; it exists on the underlying `MKMapView` (via `MKAnnotationView.clusteringIdentifier`).
For a light personal-use map this is a non-issue, but note that pixel-identical cluster bubbles would need an `MKMapView` wrapper or an explicit cluster implementation.

### Terms of service constraints

MapKit use is governed by the Apple Developer Program License Agreement and the MapKit terms.
Key constraints for this app:

- Attribution: the Apple logo and a "Legal" link are rendered by the map automatically and must remain visible; the developer cannot remove them.
- No caching/pre-fetching/storing of map data other than transient caching for performance (MapKit terms, the "map data may not be cached, pre-fetched, or stored" clause).
  This effectively rules out bundling or bulk-downloading offline Apple map tiles.
- Turn-by-turn navigation and fleet/asset-management use require a specific entitlement/license and are out of scope here.
- Free for apps distributed on Apple platforms through normal channels.

For a personal, light-use AR app these constraints are all satisfied with zero cost.

### Assessment for this app

MapKit is the lowest-cost option: free, no key, native SwiftUI, and a direct match for the four map requirements.
The trade-offs are loss of custom style JSON, no built-in clustering on the SwiftUI API, and no app-controlled offline tiles.
None of those block the app's feature set.

---

## 3. Mapbox Maps SDK for iOS

### Capabilities

Mapbox Maps SDK for iOS (current version `v11.35.0`) is the most feature-rich of the three: 3D and globe rendering, custom Mapbox Style Spec JSON, offline regions, built-in clustering, annotations, and full data-driven styling.

### Licensing and pricing

It is proprietary, not open source.
The source repository (`mapbox/mapbox-maps-ios`) states the software is licensed under the Mapbox Terms of Service for use with Mapbox products.

Pricing (as of the current pricing page):

- Requires a Mapbox account and an access token.
- Billed per "Maps SDKs for Mobile" monthly active user (MAU).
- Free tier: up to 25,000 MAU per month; beyond that, $4.00 per 1,000 MAU (tiered, decreasing with volume).
- Offline tile downloads are billed separately against tile-request tiers.
- A card-free trial account is limited to 100 MAU/month.
- Mapbox telemetry is on by default; the Mapbox ToS requires an in-app opt-out, and the Mapbox wordmark/attribution must remain visible.

### Assessment for this app

Mapbox brings capabilities this app does not need at a cost (account, token, telemetry, attribution, and MAU-based billing) that conflicts with the free/open-source, light-personal-use constraint.
Ruled out.

---

## 4. Comparison

| Criterion | MapKit (SwiftUI) | MapLibre Native for iOS | Mapbox Maps SDK for iOS |
| --- | --- | --- | --- |
| License | Proprietary but free, bundled with OS | BSD-2-Clause (open source) | Proprietary (Mapbox ToS) |
| API key / account | None | None (style host may need one) | Required |
| Billing | None | None | MAU-based, free tier 25k MAU/mo |
| SwiftUI API | First-class, native | Via official `swiftui-dsl` (pre-1.0) or `UIViewRepresentable` | SwiftUI wrappers available |
| User location | `UserAnnotation` + button | `showsUserLocation` / location component | Yes |
| Markers | `Marker`, `Annotation` | Symbol layers / DSL markers | Yes |
| Bounding-box camera fit | `MapCameraUpdate` / `.rect` | `MLNMapCamera` fit-to-bounds | Yes |
| Camera center/zoom/bearing/tilt | `MapCamera` + `MapCameraPosition` | Full camera API | Full camera API |
| Custom style JSON | No | Yes (Style Spec) | Yes (Mapbox Style Spec) |
| Clustering | `MKMapView` only, not SwiftUI `Map` | Yes | Yes |
| Offline maps | No app control (user-driven only) | Yes (`MLNOfflineStorage`) | Yes (billed separately) |
| Search | `MKLocalSearch` | None built in (bring your own) | Mapbox Search (billed) |
| Maintenance | Apple, evergreen | Active, community-governed | Active, commercial |

---

## 5. Is a KMP expect/actual map abstraction worth building?

No.
Keep the map UI fully platform-native.

Reasons:

- The map is a UI concern, and the effort's own map already states UI stays native (Compose on Android, SwiftUI on iOS).
- The two rendering stacks have no shared surface worth abstracting.
  Android uses the MapLibre Compose DSL (Compose-specific `MapState`, `CircleLayer`, `SymbolLayer`, style JSON), while iOS would use `Map`/`MapCameraPosition`/`Marker`.
  A common interface would force a lowest-common-denominator API that leaks style-JSON and layer concepts to iOS (where they do not apply) or silently drops them.
- The genuinely shared pieces are not rendering.
  They are the domain data (the `Place` model, `MapCameraPosition`, and bounding-box math), which already live in pure-Kotlin `core:model` and can cross the KMP boundary directly.
- Each platform's map rendering is small and idiomatic (the Android side is roughly one `MapPreview.kt` plus one screen).
  Two native implementations are cheaper to build and maintain than one leaky abstraction plus two adapters.
- Clustering, styling, and camera semantics differ enough between MapLibre and MapKit that any shared abstraction would be dominated by platform branches anyway.

What should be shared instead:

- `Place` and `MapCameraPosition` (already shared in `core:model`).
- The bounding-box computation and the camera-fit math can move to shared code as pure functions, so both platforms derive the fit the same way.

---

## 6. Recommendation and trade-offs

Recommend MapKit for SwiftUI, with MapLibre Native for iOS as the documented open-source fallback.

Trade-offs that matter for a light personal-use AR app:

- MapKit is free and has no key, no billing, and no telemetry, which fits the free/open-source-first, light-personal-use constraint with the least operational overhead.
- MapKit's native SwiftUI API is the most idiomatic fit for the iOS side and avoids a dependency.
- The loss is custom styling: the Android map's `dark_style`/`light_style` JSON and its cluster styling will not carry over.
  The app would use `.standard`/`.imagery` plus tinted markers instead.
  This is acceptable under the stated parity bar (feature and value parity, not pixel parity).
- Clustering and app-controlled offline tiles are the two concrete feature gaps on MapKit.
  Neither is required for the current feature set; clustering can be dropped or re-added later via `MKMapView`, and offline is already not supported on the Android side's current use of a remote style.
- If matching the Android look (same style JSON, clustering) or true offline maps becomes a requirement, MapLibre Native for iOS plus the official `swiftui-dsl` is the open-source answer.
  Its costs are a dependency, a pre-1.0 DSL API, and responsibility for a vector-tile style host.

---

## Sources

- MapLibre Native repository (license, fork history, platforms): https://github.com/maplibre/maplibre-native
- MapLibre Native releases (iOS 7.0.0, globe pre-releases, min iOS 15.5, Mapbox.h → MapLibre.h): https://github.com/maplibre/maplibre-native/releases
- MapLibre Native iOS distribution (Swift Package Manager): https://github.com/maplibre/maplibre-gl-native-distribution
- MapLibre Native iOS documentation (getting started, UIKit + UIViewRepresentable): https://maplibre.org/maplibre-native/ios/latest/documentation/maplibre-native-for-ios/gettingstarted/
- MapLibre Native iOS offline API (MLNOfflineStorage, offline packs): https://maplibre.org/maplibre-native/ios/latest/documentation/maplibre/mlnofflinestorage
- MapLibreSwiftUI (swiftui-dsl) README (migration from Stadia Maps, pre-1.0, iOS 15.5, MapLibre Native 7): https://github.com/maplibre/swiftui-dsl
- Stadia Maps blog, "Our SwiftUI DSL Is Joining the MapLibre Family": https://stadiamaps.com/blog/maplibre-swiftui-dsl/
- MapLibre Newsletters (Dec 2025, Oct 2026): https://maplibre.org/news/
- Apple, "MapKit for SwiftUI" documentation: https://developer.apple.com/documentation/mapkit/mapkit-for-swiftui
- WWDC23 session "Meet MapKit for SwiftUI": https://developer.apple.com/videos/play/wwdc2023/10043
- Apple MapKit usage terms and attribution requirements (Apple Developer Program License Agreement; "Legal" link and no-caching clause): https://developer.apple.com/forums/thread/127493 and https://forums.developer.apple.com/thread/116695
- Mapbox Maps SDK for iOS (current version, install, migrate-to-v11): https://docs.mapbox.com/ios/maps/guides/
- Mapbox Maps SDK for iOS license (Mapbox ToS): https://github.com/mapbox/mapbox-maps-ios/blob/main/LICENSE.md
- Mapbox pricing (Maps SDKs for Mobile MAU tiers and free tier): https://www.mapbox.com/pricing and https://docs.mapbox.com/ios/maps/guides/pricing/
- Mapbox sign-up and trial limits: https://docs.mapbox.com/accounts/guides/signup/
