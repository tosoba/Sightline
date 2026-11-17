# Location provider

Groundwork spec for replacing Play Services Location with a multiplatform location provider.

Source: issue 03-location-provider-kmp.md.

## Decision

Hand-roll a small expect/actual `LocationProvider` and a platform-neutral `GeoLocation` type.
No third-party KMP location library.

## Current

Extension functions on `Context` in `core/common/util/ContextExtensions.kt` wrap `FusedLocationProviderClient`: `locationUpdatesFlow()`, `locationEnabledFlow()`, `checkLocationSettings()`.
Consumed in `app/MainViewModel.kt`.
No abstraction exists, and `android.location.Location` leaks into `core:ar` and features.

## Target design

- `GeoLocation` (lat/lng/altitude/accuracy/timestamp + `distanceTo`/`bearingTo` haversine) lives in `core:model`, replacing `android.location.Location` in shared code.
- `LocationProvider` is an expect interface in `core:common` exposing:
  - a fix flow (location updates),
  - an enabled-state flow,
  - an authorization state,
  - a settings check.
- androidMain actual reuses the existing `FusedLocationProviderClient` + `LocationManager` wiring nearly verbatim; move `ResolvableApiException`/`IntentSenderRequest` up into the Android UI layer.
- iosMain actual is a ~60-line `CLLocationManager` + `callbackFlow` (`distanceFilter = 10`, `desiredAccuracy = kCLLocationAccuracyBest`; iOS has no fixed interval, so throttle in the Flow).

## iOS specifics

- `NSLocationWhenInUseUsageDescription` is the only Info.plist key.
- `locationServicesEnabled()` has no broadcast on iOS; re-check on foreground and on `CLError.denied`.
- The settings-resolvable intent has no iOS equivalent; deep-link via `UIApplication.openSettingsURLString`.

## Rejected

- jordond/compass (MIT, maintained) is the only serious library fallback, but it still leaves the neutral type, distance/bearing, enabled-flow, and settings resolution hand-written.
