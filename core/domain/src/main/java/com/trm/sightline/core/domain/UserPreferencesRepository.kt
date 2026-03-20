package com.trm.sightline.core.domain

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  fun getUserLocation(): Flow<Boolean>

  suspend fun setUserLocation(userLocation: Boolean)

  fun getHasRequestedLocationPermission(): Flow<Boolean>
  suspend fun setHasRequestedLocationPermission(hasRequested: Boolean)
}
