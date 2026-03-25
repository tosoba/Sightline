package com.trm.sightline.core.datastore

import androidx.datastore.core.DataStore
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesLocalRepository(private val userPreferencesStore: DataStore<UserPreferences>) :
  UserPreferencesRepository {
  override fun getCustomLocation(): Flow<CustomLocation?> =
    userPreferencesStore.data.map { preferences ->
      with(preferences) {
        if (hasCustomLocationLat() && hasCustomLocationLng() && hasCustomLocationAddress()) {
          CustomLocation(
            latitude = customLocationLat,
            longitude = customLocationLng,
            address = customLocationAddress,
          )
        } else {
          null
        }
      }
    }

  override fun getUserLocationEnabled(): Flow<Boolean> =
    userPreferencesStore.data.map { preferences -> preferences.userLocationEnabled }

  override suspend fun setCustomLocation(customLocation: CustomLocation) {
    userPreferencesStore.updateData { preferences ->
      preferences
        .toBuilder()
        .setCustomLocationLat(customLocation.latitude)
        .setCustomLocationLng(customLocation.longitude)
        .setCustomLocationAddress(customLocation.address)
        .build()
    }
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    userPreferencesStore.updateData { preferences ->
      preferences.toBuilder().setUserLocationEnabled(userLocationEnabled).build()
    }
  }
}
