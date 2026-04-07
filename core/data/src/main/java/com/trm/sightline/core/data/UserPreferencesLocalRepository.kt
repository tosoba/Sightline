package com.trm.sightline.core.data

import androidx.datastore.core.DataStore
import com.trm.sightline.core.datastore.UserPreferences
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesLocalRepository(private val userPreferencesStore: DataStore<UserPreferences>) :
  UserPreferencesRepository {
  override fun getCustomLocation(): Flow<CustomLocation?> =
    userPreferencesStore.data.map { preferences ->
      with(preferences) {
        if (
          hasCustomLocationLat() &&
            hasCustomLocationLng() &&
            hasCustomLocationAddress() &&
            hasCustomLocationTimestamp()
        ) {
          CustomLocation(
            latitude = customLocationLat,
            longitude = customLocationLng,
            address = customLocationAddress,
            timestamp = customLocationTimestamp,
          )
        } else {
          null
        }
      }
    }

  override fun getUserLocationEnabled(): Flow<Boolean> =
    userPreferencesStore.data.map { preferences -> preferences.userLocationEnabled }

  override fun getLastMapPosition(): Flow<MapCameraPosition?> =
    userPreferencesStore.data.map { preferences ->
      with(preferences) {
        if (
          hasLastMapLat() &&
            hasLastMapLng() &&
            hasLastMapZoom() &&
            hasLastMapBearing() &&
            hasLastMapTilt()
        ) {
          MapCameraPosition(
            latitude = lastMapLat,
            longitude = lastMapLng,
            zoom = lastMapZoom,
            bearing = lastMapBearing,
            tilt = lastMapTilt,
          )
        } else {
          null
        }
      }
    }

  override suspend fun setCustomLocation(customLocation: CustomLocation) {
    userPreferencesStore.updateData { preferences ->
      preferences
        .toBuilder()
        .setCustomLocationLat(customLocation.latitude)
        .setCustomLocationLng(customLocation.longitude)
        .setCustomLocationAddress(customLocation.address)
        .setCustomLocationTimestamp(System.currentTimeMillis())
        .build()
    }
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    userPreferencesStore.updateData { preferences ->
      preferences.toBuilder().setUserLocationEnabled(userLocationEnabled).build()
    }
  }

  override suspend fun setLastMapPosition(position: MapCameraPosition) {
    userPreferencesStore.updateData { preferences ->
      preferences
        .toBuilder()
        .setLastMapLat(position.latitude)
        .setLastMapLng(position.longitude)
        .setLastMapZoom(position.zoom)
        .setLastMapBearing(position.bearing)
        .setLastMapTilt(position.tilt)
        .build()
    }
  }

  override fun getSearchRadius(): Flow<PlaceSearchRadius> =
    userPreferencesStore.data.map { preferences ->
      if (preferences.hasSearchRadius()) PlaceSearchRadius.fromMeters(preferences.searchRadius)
      else PlaceSearchRadius.OneKilometer
    }

  override suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    userPreferencesStore.updateData { preferences ->
      preferences.toBuilder().setSearchRadius(radius.meters).build()
    }
  }
}
