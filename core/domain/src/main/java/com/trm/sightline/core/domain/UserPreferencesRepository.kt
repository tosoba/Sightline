package com.trm.sightline.core.domain

import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  fun getCustomLocation(): Flow<CustomLocation?>

  fun getUserLocationEnabled(): Flow<Boolean>

  fun getLastMapPosition(): Flow<MapCameraPosition?>

  suspend fun setCustomLocation(customLocation: CustomLocation)

  suspend fun setUserLocationEnabled(userLocationEnabled: Boolean)

  suspend fun setLastMapPosition(position: MapCameraPosition)

  fun getSearchRadius(): Flow<Int>

  suspend fun setSearchRadius(radius: Int)
}
