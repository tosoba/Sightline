# Location provider migration to KMP

Researched 2026-11-17 against Apple Core Location docs, Kotlin Multiplatform docs, and the GitHub repos and release notes of the KMP location libraries.
Question: how should Play Services Location be replaced with a multiplatform location provider?

## Recommendation (summary)

Hand-roll a small `expect`/`actual` abstraction rather than adopt a library.

The surface to share is tiny (one fix flow, one enabled-state flow, one authorization state, one settings check) and two of the three current flows have no iOS analog or library equivalent, so a library would not remove the real work.
The one non-negotiable deliverable is a platform-neutral location type (with `distanceTo`/`bearingTo`) to replace `android.location.Location` in shared code, and no library provides that type or those helpers.
Reuse the existing Android `FusedLocationProviderClient` + `LocationManager` wiring almost verbatim as the `androidMain` actual, and write a ~60 line `CLLocationManager` + `callbackFlow` actual for iOS.

The strongest library candidate is Compass (jordond/compass), which is actively maintained and covers geolocation plus permission prompting.
It is a legitimate fallback if the team also wants to outsource the Android fused-provider wiring and iOS Core Location plumbing, but it still leaves the neutral type, `distanceTo`/`bearingTo`, the enabled-state flow, and the settings-resolvable intent as hand-written code.

---

## 1. Core Location (CLLocationManager) equivalents

### Continuous updates

`CLLocationManager.startUpdatingLocation()` is the direct equivalent of `FusedLocationProviderClient.requestLocationUpdates(...)` with a `LocationCallback`.
It returns immediately, obtains an initial fix, then calls the delegate's `locationManager(_:didUpdateLocations:)` when the `distanceFilter` is exceeded or a more accurate fix arrives.
Source: https://developer.apple.com/documentation/corelocation/cllocationmanager/startupdatinglocation()

Two properties configure it:

- `distanceFilter` maps to `setMinUpdateDistanceMeters(10f)`.
  It is the minimum horizontal meters the device must move before an update is generated.
  The default is `kCLDistanceFilterNone` (report all movement), so Sightline would set it to `10.0`.
  Source: https://developer.apple.com/documentation/corelocation/cllocationmanager/distancefilter
- `desiredAccuracy` maps to `Priority.PRIORITY_HIGH_ACCURACY`.
  `kCLLocationAccuracyBest` is the closest single constant and is the iOS default.
  Core Location delivers lower-accuracy fixes first and improves toward the target over time, which matches how the fused provider behaves with `PRIORITY_HIGH_ACCURACY`.
  Source: https://developer.apple.com/documentation/corelocation/cllocationmanager/desiredaccuracy

There is no fixed-interval callback in the Core Location standard service.
The Android `setIntervalMillis(10_000)` has no iOS equivalent: Core Location is event-driven (distance filter exceeded or accuracy improved).
If a 10 second minimum spacing is required on iOS, it must be applied in the `Flow` layer by dropping updates until the interval has elapsed, not by configuring the manager.

### Authorization model

The authorization flow is `CLLocationManager.requestWhenInUseAuthorization()` (or `requestAlwaysAuthorization()` for background), which prompts asynchronously and reports the result through the delegate's `locationManagerDidChangeAuthorization(_:)` (or the older `locationManager(_:didChangeAuthorization:)`).
The prompt text comes from the Info.plist key, which is mandatory before the request.
Source: https://developer.apple.com/documentation/corelocation/cllocationmanager/requestwheninuseauthorization()
Source: https://developer.apple.com/documentation/corelocation/requesting_authorization_for_location_services

Info.plist keys:

- `NSLocationWhenInUseUsageDescription` - required whenever the app requests When-in-Use or Always authorization.
- `NSLocationAlwaysAndWhenInUseUsageDescription` - required only when requesting Always authorization.
- `NSLocationUsageDescription` is macOS-only and should not be used on iOS.

Sightline tracks location only while the app is in the foreground, so it needs only `NSLocationWhenInUseUsageDescription`.
There is no `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`-style split in the manifest; the runtime permission is the single Info.plist string.

`CLAuthorizationStatus` is the enum the app reads from `CLLocationManager.authorizationStatus`:

- `.notDetermined` - not yet requested.
- `.restricted` - parental controls or an MDM profile; the user cannot grant access.
- `.denied` - the user denied; further prompts do nothing.
- `.authorizedWhenInUse` - foreground access granted.
- `.authorizedAlways` - background access granted.

These map onto Sightline's existing `PermissionStatus` (`Unknown` -> `.notDetermined`, `Granted` -> `.authorizedWhenInUse`/`.authorizedAlways`, `Denied` -> `.denied`, `PermanentlyDenied` -> `.denied` after a prior denial, `restricted` has no exact Android analog).
Source: https://developer.apple.com/documentation/corelocation/clauthorizationstatus

### Location-services-enabled / denied state

`CLLocationManager.locationServicesEnabled()` (a class method) reports whether location services are enabled on the whole device, equivalent to `LocationManagerCompat.isLocationEnabled(locationManager)` on Android.
Source: https://developer.apple.com/documentation/corelocation/cllocationmanager/locationservicesenabled()

There is no broadcast on iOS equivalent to Android's `LocationManager.PROVIDERS_CHANGED_ACTION`.
To make the enabled state reactive, re-check it on app foreground (scene phase change) and treat a `CLError.denied` in `locationManager(_:didFailWithError:)` as a live "location disabled" signal.

### The "settings resolvable" intent has no iOS equivalent

On Android the `checkLocationSettings()` flow returns a `ResolvableApiException` whose `resolution` is a one-tap `IntentSender` that can turn location on in place.
There is no iOS API that enables location services or grants authorization programmatically.
The closest analog is deep-linking into the Settings app:

- `UIApplication.openSettingsURLString` opens the app's own Settings page (permission toggles, precise-location toggle).
- A location-services-off state can only point the user at Settings -> Privacy -> Location Services, then the user toggles it manually.

Because the resolution UI is irreducibly different per platform, it should stay in the platform UI layer (see section 2), not in the shared provider.

---

## 2. Shared expect/actual LocationProvider design

### Platform-neutral location type

`android.location.Location` must be replaced in shared code with a plain type.
It should carry exactly what the app uses and the two helpers the AR and sort code rely on.

The app uses `latitude`, `longitude`, `altitude`, `distanceTo`, and `bearingTo` (grep of `core`, `feature`, and `app`).
It does not currently use `speed`, `course`, or `accuracy`, so those are optional.

```kotlin
// commonMain (core:model, already pure Kotlin)
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val horizontalAccuracyMeters: Double? = null,
    val timestampMillis: Long,
) {
    fun distanceTo(other: GeoLocation): Float   // haversine, meters
    fun bearingTo(other: GeoLocation): Float    // initial bearing, degrees
}
```

`distanceTo` and `bearingTo` are ~15 lines of well-known spherical math (haversine for distance, atan2 for initial bearing) and are identical across platforms, so they live in commonMain.
`Place.location` in `PlaceExtensions.kt` becomes `GeoLocation(latitude = latitude, longitude = longitude, timestampMillis = 0)`.

Scope note: `core:ar` (ARView, ARMarker, ARMarkerRenderer, Math3D) also imports `android.location.Location`, but AR is native per platform and out of scope for the shared layer.
Those files can keep `android.location.Location` on Android for now, or convert to `GeoLocation` later when the AR code itself is ported.
The only leak that blocks sharing today is the one that reaches `core:model`/`core:domain`/the ViewModel boundary via `PlaceExtensions` and the provider.

### LocationProvider interface

```kotlin
// commonMain
interface LocationProvider {
    // Cold flow of fixes. Collection starts updates, cancellation stops them.
    fun locationUpdates(): Flow<GeoLocation>

    // Reactive "is location enabled on the device" state (defaults to true on iOS,
    // where a disabled service surfaces as authorization denied).
    fun isLocationEnabled(): Flow<Boolean>

    // Authorization state, reactive where the platform allows it.
    val authorizationState: Flow<LocationAuthorization>

    suspend fun checkLocationSettings(): LocationSettingsState
}

enum class LocationAuthorization {
    NotDetermined, Granted, Denied, Restricted,
}

sealed interface LocationSettingsState {
    data object Enabled : LocationSettingsState
    data object Disabled : LocationSettingsState
}

expect fun createLocationProvider(context: PlatformContext): LocationProvider
```

Notes on the shape:

- `PlatformContext` is an `expect` type (`android.content.Context` on Android, a no-op holder on iOS), the same pattern the KMP docs and Coil 3 use, so the factory can be called from common code without leaking `android.*` into commonMain.
  Alternatively, wire the concrete provider from each platform's entry point (the project's chosen DI, Metro, is decided in a sibling ticket) and skip the factory.
- `checkLocationSettings()` returns the neutral `Enabled`/`Disabled` only.
  The Android `DisabledResolvable(intentSenderRequest)` and its `IntentSenderRequest` are Android types and cannot be shared, so they stay in the Android UI layer.
  The shared result only tells the UI "location is off"; the UI then chooses the platform-appropriate resolution (Android one-tap dialog, iOS deep-link to Settings).
- `isLocationEnabled()` is a `Flow` because `MainViewModel` currently combines it with the user preference to auto-disable tracking when the system toggle turns off.
  The Android actual keeps the `PROVIDERS_CHANGED_ACTION` broadcast receiver; the iOS actual emits `locationServicesEnabled()` re-checked on foreground, plus a `false` on `CLError.denied`.
- Permission requesting stays in the native UI layer, matching the current split (`MainScreen` uses `rememberPermissionState` on Android, SwiftUI will call `requestWhenInUseAuthorization()` on iOS).
  The shared provider exposes `authorizationState` for the ViewModel to gate tracking, and `locationUpdates()` assumes permission is already granted (reporting failure through the flow's error path if not).

### Reuse vs rewrite

Reuse (move nearly verbatim into the `androidMain` actual):

- The `FusedLocationProviderClient` + `LocationCallback` + `callbackFlow` body of `locationUpdatesFlow()`.
- The `LocationManagerCompat.isLocationEnabled` + `PROVIDERS_CHANGED_ACTION` receiver of `locationEnabledFlow()`.
- The `LocationRequest(PRIORITY_HIGH_ACCURACY, 10_000L).setMinUpdateDistanceMeters(10f)` configuration.
- The `checkLocationSettings()` settings client call, but split: the shared provider returns `Enabled`/`Disabled`, and the `ResolvableApiException` -> `IntentSenderRequest` branch moves into `MainScreen` (Android UI), where it already lives today.

Rewrite (new):

- The `GeoLocation` type and its `distanceTo`/`bearingTo`.
- The iOS `CLLocationManager` actual (delegate -> `callbackFlow`), setting `distanceFilter = 10.0` and `desiredAccuracy = kCLLocationAccuracyBest`.
- The iOS authorization state (delegate callback plus `locationServicesEnabled()`).
- The iOS "open Settings" action (deep-link), which has no current equivalent.

---

## 3. Existing KMP location libraries

### Compass (jordond/compass)

The most actively maintained KMP location toolkit.
MIT license, 462 stars, latest tag 4.0.1 (Oct 2026).
Source: https://github.com/jordond/compass

Relevant API (from the docs):

- `Geolocator` interface with `current()`, `startTracking()`/`stopTracking()`, `locationUpdates: Flow<Location>`, and `trackingStatus: Flow<TrackingStatus>`.
- `Location(coordinates, accuracy, azimuth, speed, altitude, timestampMillis)` - a plain data holder.
- `LocationRequest(priority, interval, maximumAge)`; `interval` is honoured on both platforms (time-throttled in the Flow on iOS).
- `LocationPermissionController.mobile()` with `requestPermission()`, `grantedAccuracy()` (full vs reduced/approximate), and `isPermissionDeniedForever`.
- Android: `geolocation-mobile` uses the built-in `LocationManager` with no Play Services dependency; `geolocation-android-gms` opts back into the fused provider and falls back to `LocationManager` when Play Services is absent.
- iOS: Core Location under the hood; `info.plist` usage key is still the app's job.
- Also ships `geocoder-mobile` (native reverse geocoding), which Sightline does not need because reverse geocoding already goes through the photon HTTP API.

Sources: https://compass.jordond.dev/geolocation/geolocator.md, https://compass.jordond.dev/geolocation/location.md, https://compass.jordond.dev/geolocation/android-ios.md

Fit assessment:

- Strong fit for the fix flow, permission prompting, and precise/approximate accuracy.
- Does not cover two of the three current flows: there is no reactive enabled-state flow (`isAvailable()` is a one-shot check, not a Flow), and no settings-resolvable intent.
- Its `Location` type has no `distanceTo`/`bearingTo`, so Sightline still writes that math and a mapping layer.
- Introduces a dependency plus a permission model that overlaps Sightline's existing `PermissionState`/`PermissionStatus` in `core:common`.

### moko-geo (icerockdev)

The older, established KMP geolocation library.
Apache-2.0, latest 0.8.0 (Mar 2026), but the repo has only 89 commits across its history and recent releases are almost entirely `moko-permissions` version bumps, so it is maintained lightly rather than actively.
Source: https://github.com/icerockdev/moko-geo and https://github.com/icerockdev/moko-geo/releases

Relevant API:

- `expect class LocationTracker` with `startTracking()` (suspend, triggers permission), `stopTracking()`, `getLocationsFlow(): Flow<LatLng>`, `getExtendedLocationsFlow(): Flow<ExtendedLocation>`, and a `permissionsController`.
- `Location(coordinates: LatLng, coordinatesAccuracyMeters: Double)` - only lat/lng plus accuracy; no altitude, bearing, timestamp, or speed in the base type.
- Depends on a separate `dev.icerock.moko:permissions-location` artifact.

Sources: https://github.com/icerockdev/moko-geo/blob/master/geo/src/commonMain/kotlin/dev/icerock/moko/geo/Location.kt and .../LocationTracker.kt

Fit assessment:

- The base `Location` lacks altitude, which `core:ar` math reads directly, so it needs the extended type or a mapping layer anyway.
- No enabled-state flow, no settings-check, no `distanceTo`/`bearingTo`.
- `expect class` plus a required permission dependency is more coupling than the small surface justifies.

### klocation (addhen/klocation)

A newer, small library (16 stars, 0 forks) for retrieving device location with Compose Multiplatform integration.
Too small and unproven to build a migration on.
Source: https://github.com/addhen/klocation

### Comparison table

| Library | License | Latest | Maintenance | Fix Flow | Enabled state | Settings-resolvable | Permission | Neutral type w/ distance/bearing |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Compass | MIT | 4.0.1 (Oct 2026) | Active | Yes | one-shot `isAvailable()` | No | Yes (incl. precise/approx) | No |
| moko-geo | Apache-2.0 | 0.8.0 (Mar 2026) | Light | Yes (`LatLng`/`ExtendedLocation`) | No | No | Via `moko-permissions` | No |
| klocation | Apache-2.0 | - | Unproven (16 stars) | Yes | No | No | Unclear | No |
| Hand-rolled | - | - | Owned | Yes | Yes (Flow) | Android-only, in UI | Existing `PermissionStatus` | Yes |

---

## 4. Recommendation

Hand-roll the `expect`/`actual` abstraction described in section 2.

Rationale:

- The neutral `GeoLocation` type with `distanceTo`/`bearingTo` is required work no library removes, because no candidate provides those helpers and `android.location.Location` must stop leaking into shared code.
- Two of the three current flows (`locationEnabledFlow` and the settings-resolvable intent) have no library equivalent, so a library would still leave those hand-written.
- The remaining flow (`locationUpdatesFlow`) is already written and working; it moves into an `androidMain` actual almost unchanged.
- The iOS actual is small and maps 1:1 onto the Core Location primitives in section 1.
- It keeps the dependency graph light, matching the project's stated preference for FOSS and lighter alternatives, and avoids reconciling a third-party permission model with the existing `PermissionStatus`/`PermissionState`.

Adopt Compass only if the team additionally wants to outsource the Android fused-provider wiring, iOS Core Location plumbing, and runtime permission prompting as a package.
That is a reasonable trade if the app grows more location surface later, but for the current three flows it buys little over the hand-rolled version while still requiring the neutral type, the distance/bearing math, the enabled-state flow, and the settings resolution.

---

## Sources

- Apple, CLLocationManager `startUpdatingLocation()`: https://developer.apple.com/documentation/corelocation/cllocationmanager/startupdatinglocation()
- Apple, CLLocationManager `distanceFilter`: https://developer.apple.com/documentation/corelocation/cllocationmanager/distancefilter
- Apple, CLLocationManager `desiredAccuracy`: https://developer.apple.com/documentation/corelocation/cllocationmanager/desiredaccuracy
- Apple, Requesting authorization to use location services: https://developer.apple.com/documentation/corelocation/requesting_authorization_for_location_services
- Apple, `requestWhenInUseAuthorization()`: https://developer.apple.com/documentation/corelocation/cllocationmanager/requestwheninuseauthorization()
- Apple, `CLAuthorizationStatus`: https://developer.apple.com/documentation/corelocation/clauthorizationstatus
- Apple, `locationServicesEnabled()`: https://developer.apple.com/documentation/corelocation/cllocationmanager/locationservicesenabled()
- Apple, Configuring your app to use location services: https://developer.apple.com/documentation/corelocation/configuring-your-app-to-use-location-services
- Kotlin Multiplatform, Expected and actual declarations: https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html
- Kotlin Multiplatform, Use platform-specific APIs: https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html
- Compass repo: https://github.com/jordond/compass
- Compass Geolocator docs: https://compass.jordond.dev/geolocation/geolocator.md
- Compass Location docs: https://compass.jordond.dev/geolocation/location.md
- Compass Android/iOS docs: https://compass.jordond.dev/geolocation/android-ios.md
- moko-geo repo: https://github.com/icerockdev/moko-geo
- moko-geo releases: https://github.com/icerockdev/moko-geo/releases
- moko-geo Location source: https://github.com/icerockdev/moko-geo/blob/master/geo/src/commonMain/kotlin/dev/icerock/moko/geo/Location.kt
- moko-geo LocationTracker source: https://github.com/icerockdev/moko-geo/blob/master/geo/src/commonMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
- klocation repo: https://github.com/addhen/klocation
