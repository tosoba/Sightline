package com.trm.sightline.shared

import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlinx.coroutines.flow.Flow

class PreferencesFacade internal constructor(private val repository: UserPreferencesRepository) {
  val searchRadius: Flow<PlaceSearchRadius> = repository.getSearchRadius()

  val customLocation: Flow<CustomLocation?> = repository.getCustomLocation()

  val userLocationEnabled: Flow<Boolean> = repository.getUserLocationEnabled()

  suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    repository.setSearchRadius(radius)
  }

  suspend fun setCustomLocation(customLocation: CustomLocation) {
    repository.setCustomLocation(customLocation)
  }

  suspend fun setUserLocationEnabled(enabled: Boolean) {
    repository.setUserLocationEnabled(enabled)
  }
}
