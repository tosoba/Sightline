package com.trm.sightline.core.datastore

import androidx.datastore.core.DataStore
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserPreferencesLocalRepository(private val userPreferencesStore: DataStore<UserPreferences>) :
  UserPreferencesRepository {
  override suspend fun getCustomLocation(): SearchResult? =
    userPreferencesStore.data
      .map { preferences ->
        if (preferences.customLocationAddress.isNotBlank()) {
          SearchResult(
            latitude = preferences.customLocationLat,
            longitude = preferences.customLocationLng,
            address = preferences.customLocationAddress,
          )
        } else {
          null
        }
      }
      .firstOrNull()

  override fun getUserLocationEnabled(): Flow<Boolean> =
    userPreferencesStore.data.map { preferences -> preferences.userLocationEnabled }

  override suspend fun setCustomLocation(searchResult: SearchResult?) {
    userPreferencesStore.updateData { preferences ->
      preferences
        .toBuilder()
        .setCustomLocationLat(searchResult?.latitude ?: 0.0)
        .setCustomLocationLng(searchResult?.longitude ?: 0.0)
        .setCustomLocationAddress(searchResult?.address.orEmpty())
        .build()
    }
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    userPreferencesStore.updateData { preferences ->
      preferences.toBuilder().setUserLocationEnabled(userLocationEnabled).build()
    }
  }
}
