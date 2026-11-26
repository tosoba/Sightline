package com.trm.sightline.core.common.util

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.trm.sightline.core.common.location.locationRequest
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

fun Context.startAppSettingsActivity() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", packageName, null)),
  )
}

suspend fun Context.checkLocationSettings(): CheckLocationSettingsResult =
  suspendCancellableCoroutine { continuation ->
    LocationServices.getSettingsClient(this)
      .checkLocationSettings(
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest()).build(),
      )
      .addOnSuccessListener { continuation.resume(CheckLocationSettingsResult.Enabled) }
      .addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
          try {
            continuation.resume(
              CheckLocationSettingsResult.DisabledResolvable(
                IntentSenderRequest.Builder(exception.resolution).build(),
              ),
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
