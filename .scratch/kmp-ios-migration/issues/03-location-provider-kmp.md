Type: research
Status: resolved

## Question

How should Play Services Location be replaced with a multiplatform location provider?

Current: extension functions on `Context` in `core/common/util/ContextExtensions.kt` wrapping `FusedLocationProviderClient` (`locationUpdatesFlow`, `locationEnabledFlow`, `checkLocationSettings`), consumed in `app/MainViewModel.kt`.
There is no location abstraction, and `android.location.Location` leaks into `core:ar` and several feature modules.

Research and report:

- The Core Location (CLLocationManager) equivalents for the three current flows.
- A shared `expect`/`actual` LocationProvider interface design, and a platform-neutral location type.
- Whether an existing KMP location library fits (and its maintenance status) vs a small hand-rolled abstraction.

Recommend the abstraction shape and what to reuse vs rewrite.

## Answer

Hand-roll a small `expect`/`actual` `LocationProvider` rather than adopt a library.

- The shared surface is tiny: a fix flow, an enabled-state flow, an authorization state, and a settings check.
  Two of the three current flows (`locationEnabledFlow`, the `checkLocationSettings` resolvable intent) have no iOS analog and no library equivalent, so a library would not remove the real work.
- Non-negotiable deliverable: a platform-neutral `GeoLocation` type (lat/lng/altitude/accuracy/timestamp + `distanceTo`/`bearingTo` haversine math) to replace `android.location.Location` in shared code.
  No candidate library provides that type or those helpers.
- Reuse the existing `FusedLocationProviderClient` + `LocationManager` wiring nearly verbatim as the `androidMain` actual (move `ResolvableApiException`/`IntentSenderRequest` up into the Android UI layer).
  Rewrite only the neutral type and a ~60-line `CLLocationManager` + `callbackFlow` iOS actual (`distanceFilter = 10`, `desiredAccuracy = kCLLocationAccuracyBest`; no fixed interval on iOS, throttle in the Flow).
- iOS notes: `startUpdatingLocation`/`distanceFilter`/`desiredAccuracy` map 1:1; `NSLocationWhenInUseUsageDescription` is the only plist key needed; `locationServicesEnabled()` has no broadcast, so re-check on foreground + `CLError.denied`; the settings-resolvable intent has no equivalent - deep-link via `UIApplication.openSettingsURLString`.
- Compass (jordond/compass, MIT, maintained) is the only serious fallback but still leaves the neutral type, distance/bearing, enabled-flow, and settings resolution hand-written.

Full findings: [research/03-location-provider-kmp.md](research/03-location-provider-kmp.md).
