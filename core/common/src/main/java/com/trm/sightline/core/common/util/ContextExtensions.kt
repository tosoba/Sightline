package com.trm.sightline.core.common.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun Context.startAppSettingsActivity() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", packageName, null))
  )
}

fun Context.locationUpdatesFlow(): Flow<Location> = callbackFlow {
  val client = LocationServices.getFusedLocationProviderClient(this@locationUpdatesFlow)
  val callback =
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let(::trySend)
      }
    }
  @SuppressLint("MissingPermission")
  client.requestLocationUpdates(locationRequest(), callback, Looper.getMainLooper())
  awaitClose { client.removeLocationUpdates(callback) }
}

suspend fun Context.checkLocationSettings(): CheckLocationSettingsResult =
  suspendCancellableCoroutine { continuation ->
    LocationServices.getSettingsClient(this)
      .checkLocationSettings(
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest()).build()
      )
      .addOnSuccessListener { continuation.resume(CheckLocationSettingsResult.Enabled) }
      .addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
          try {
            continuation.resume(
              CheckLocationSettingsResult.DisabledResolvable(
                IntentSenderRequest.Builder(exception.resolution).build()
              )
            )
          } catch (_: IntentSender.SendIntentException) {
            continuation.resume(CheckLocationSettingsResult.DisabledNonResolvable)
          }
        } else {
          continuation.resume(CheckLocationSettingsResult.DisabledNonResolvable)
        }
      }
  }

sealed interface CheckLocationSettingsResult {
  data object Enabled : CheckLocationSettingsResult

  data class DisabledResolvable(val intentSenderRequest: IntentSenderRequest) :
    CheckLocationSettingsResult

  data object DisabledNonResolvable : CheckLocationSettingsResult
}

private fun locationRequest(): LocationRequest =
  LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MS)
    .setMinUpdateDistanceMeters(10f)
    .build()

private const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
