package com.trm.sightline.core.data

import androidx.datastore.core.DataStore
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.PlaceSearchRadius
import com.trm.sightline.core.model.UserPreferences
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesLocalRepository(private val userPreferencesStore: DataStore<UserPreferences>) :
  UserPreferencesRepository {
  override fun getCustomLocation(): Flow<CustomLocation?> =
    userPreferencesStore.data.map { preferences ->
      val latitude = preferences.customLocationLatitude
      val longitude = preferences.customLocationLongitude
      val address = preferences.customLocationAddress
      val timestamp = preferences.customLocationTimestampMillis
      if (latitude != null && longitude != null && address != null && timestamp != null) {
        CustomLocation(
          latitude = latitude,
          longitude = longitude,
          address = address,
          timestamp = timestamp,
        )
      } else {
        null
      }
    }

  override fun getUserLocationEnabled(): Flow<Boolean> =
    userPreferencesStore.data.map { preferences -> preferences.userLocationEnabled }

  override fun getLastMapPosition(): Flow<MapCameraPosition?> =
    userPreferencesStore.data.map { preferences ->
      val latitude = preferences.lastMapLatitude
      val longitude = preferences.lastMapLongitude
      val zoom = preferences.lastMapZoom
      val bearing = preferences.lastMapBearing
      val pitch = preferences.lastMapPitch
      if (
        latitude != null && longitude != null && zoom != null && bearing != null && pitch != null
      ) {
        MapCameraPosition(
          latitude = latitude,
          longitude = longitude,
          zoom = zoom,
          bearing = bearing,
          pitch = pitch,
        )
      } else {
        null
      }
    }

  override suspend fun setCustomLocation(customLocation: CustomLocation) {
    userPreferencesStore.updateData { preferences ->
      preferences.copy(
        customLocationLatitude = customLocation.latitude,
        customLocationLongitude = customLocation.longitude,
        customLocationAddress = customLocation.address,
        customLocationTimestampMillis = Clock.System.now().toEpochMilliseconds(),
      )
    }
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    userPreferencesStore.updateData { preferences ->
      preferences.copy(userLocationEnabled = userLocationEnabled)
    }
  }

  override suspend fun setLastMapPosition(position: MapCameraPosition) {
    userPreferencesStore.updateData { preferences ->
      preferences.copy(
        lastMapLatitude = position.latitude,
        lastMapLongitude = position.longitude,
        lastMapZoom = position.zoom,
        lastMapBearing = position.bearing,
        lastMapPitch = position.pitch,
      )
    }
  }

  override fun getSearchRadius(): Flow<PlaceSearchRadius> =
    userPreferencesStore.data.map { preferences ->
      preferences.searchRadiusMeters?.let(PlaceSearchRadius::fromMeters)
        ?: PlaceSearchRadius.OneKilometer
    }

  override suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    userPreferencesStore.updateData { preferences ->
      preferences.copy(searchRadiusMeters = radius.meters)
    }
  }

  override suspend fun reset() {
    userPreferencesStore.updateData { UserPreferences() }
  }
}
