package com.trm.sightline.core.domain

import com.trm.sightline.core.model.CustomLocation
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  fun getCustomLocation(): Flow<CustomLocation?>

  fun getUserLocationEnabled(): Flow<Boolean>

  suspend fun setCustomLocation(customLocation: CustomLocation)

  suspend fun setUserLocationEnabled(userLocationEnabled: Boolean)
}
