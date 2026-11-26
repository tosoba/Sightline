package com.trm.sightline.core.common.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.trm.sightline.core.common.util.CheckLocationSettingsResult
import com.trm.sightline.core.common.util.checkLocationSettings
import com.trm.sightline.core.model.GeoLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class AndroidLocationProvider(private val context: Context) : LocationProvider {
  override fun locationUpdates(): Flow<GeoLocation> = callbackFlow {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val callback =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { location ->
            trySend(
              GeoLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                horizontalAccuracyMeters =
                  if (location.hasAccuracy()) location.accuracy.toDouble() else null,
                timestampMillis = location.time,
              ),
            )
          }
        }
      }
    @SuppressLint("MissingPermission")
    client.requestLocationUpdates(locationRequest(), callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
  }

  override fun isLocationEnabled(): Flow<Boolean> = callbackFlow {
    val locationManager = context.getSystemService(LocationManager::class.java)
    trySend(LocationManagerCompat.isLocationEnabled(locationManager))
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            trySend(LocationManagerCompat.isLocationEnabled(locationManager))
          }
        }
      }
    context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    awaitClose { context.unregisterReceiver(receiver) }
  }

  override val authorizationState: Flow<LocationAuthorization> = flow {
    emit(
      if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
      ) {
        LocationAuthorization.Granted
      } else {
        LocationAuthorization.Denied
      },
    )
  }

  override suspend fun checkLocationSettings(): LocationSettingsState =
    when (context.checkLocationSettings()) {
      CheckLocationSettingsResult.Enabled -> LocationSettingsState.Enabled
      is CheckLocationSettingsResult.DisabledResolvable,
      CheckLocationSettingsResult.DisabledNonResolvable -> LocationSettingsState.Disabled
    }
}
