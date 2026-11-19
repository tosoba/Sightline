package com.trm.sightline.core.common.location

import com.trm.sightline.core.model.GeoLocation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
  private val locationManager = CLLocationManager()

  private val updates =
    MutableSharedFlow<GeoLocation>(
      extraBufferCapacity = 8,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  private val authorization =
    MutableStateFlow(locationManager.authorizationStatus.toLocationAuthorization())

  // CLLocationManager.delegate is weak; the provider must own the delegate for callbacks to arrive.
  private val delegate = LocationDelegate(updates, authorization)

  init {
    locationManager.delegate = delegate
  }

  override fun locationUpdates(): Flow<GeoLocation> =
    callbackFlow {
        val collector = updates.onEach { send(it) }.launchIn(this)

        if (locationManager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
          locationManager.requestWhenInUseAuthorization()
        }
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = DISTANCE_FILTER_METERS
        locationManager.startUpdatingLocation()

        awaitClose {
          collector.cancel()
          locationManager.stopUpdatingLocation()
        }
      }
      .throttleFirst(UPDATE_THROTTLE)

  override fun isLocationEnabled(): Flow<Boolean> =
    flow { emit(CLLocationManager.locationServicesEnabled()) }

  override val authorizationState: Flow<LocationAuthorization> = authorization.asStateFlow()

  override suspend fun checkLocationSettings(): LocationSettingsState =
    if (CLLocationManager.locationServicesEnabled()) {
      LocationSettingsState.Enabled
    } else {
      LocationSettingsState.Disabled
    }

  private companion object {
    const val DISTANCE_FILTER_METERS = 10.0
    val UPDATE_THROTTLE = 10.seconds
  }
}

private const val MILLIS_PER_SECOND = 1_000.0

/**
 * Owns no reference to the provider, so the provider holding the delegate cannot retain it back.
 */
@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate(
  private val updates: MutableSharedFlow<GeoLocation>,
  private val authorization: MutableStateFlow<LocationAuthorization>,
) : NSObject(), CLLocationManagerDelegateProtocol {
  override fun locationManager(
    manager: CLLocationManager,
    didUpdateLocations: List<*>,
  ) {
    val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
    updates.tryEmit(location.toGeoLocation())
  }

  override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
    authorization.value = manager.authorizationStatus.toLocationAuthorization()
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toGeoLocation(): GeoLocation {
  val (latitude, longitude) = coordinate.useContents { latitude to longitude }
  return GeoLocation(
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    horizontalAccuracyMeters = horizontalAccuracy.takeIf { it >= 0 },
    timestampMillis = (timestamp.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong(),
  )
}

private fun CLAuthorizationStatus.toLocationAuthorization(): LocationAuthorization =
  when (this) {
    kCLAuthorizationStatusNotDetermined -> LocationAuthorization.NotDetermined
    kCLAuthorizationStatusRestricted -> LocationAuthorization.Restricted
    kCLAuthorizationStatusDenied -> LocationAuthorization.Denied
    kCLAuthorizationStatusAuthorizedWhenInUse,
    kCLAuthorizationStatusAuthorizedAlways -> LocationAuthorization.Granted
    else -> LocationAuthorization.NotDetermined
  }

private fun <T> Flow<T>.throttleFirst(period: Duration): Flow<T> = flow {
  var nextEmission = TimeSource.Monotonic.markNow()
  collect { value ->
    if (nextEmission.hasPassedNow()) {
      nextEmission = TimeSource.Monotonic.markNow() + period
      emit(value)
    }
  }
}
