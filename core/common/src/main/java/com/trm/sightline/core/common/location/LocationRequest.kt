package com.trm.sightline.core.common.location

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

internal fun locationRequest(): LocationRequest =
  LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL_MS)
    .setMinUpdateDistanceMeters(10f)
    .build()

private const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
