package com.trm.sightline.core.common.location

import com.trm.sightline.core.model.GeoLocation
import kotlinx.coroutines.flow.Flow

interface LocationProvider {
  fun locationUpdates(): Flow<GeoLocation>
  fun isLocationEnabled(): Flow<Boolean>
  val authorizationState: Flow<LocationAuthorization>
  suspend fun checkLocationSettings(): LocationSettingsState
}
