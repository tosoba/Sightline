package com.trm.sightline.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.trm.sightline.core.domain.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesLocalRepository
@Inject
constructor(@ApplicationContext private val context: Context) : UserPreferencesRepository {
  override fun getUserLocation(): Flow<Boolean> =
    context.dataStore.data.map { preferences -> preferences[USER_LOCATION] ?: true }

  override suspend fun setUserLocation(userLocation: Boolean) {
    context.dataStore.edit { preferences -> preferences[USER_LOCATION] = userLocation }
  }

  override fun getHasRequestedLocationPermission(): Flow<Boolean> =
    context.dataStore.data.map { preferences -> preferences[HAS_REQUESTED_PERMISSION] ?: false }

  override suspend fun setHasRequestedLocationPermission(hasRequested: Boolean) {
    context.dataStore.edit { preferences -> preferences[HAS_REQUESTED_PERMISSION] = hasRequested }
  }

  companion object {
    private val USER_LOCATION = booleanPreferencesKey("user_location")
    private val HAS_REQUESTED_PERMISSION = booleanPreferencesKey("has_requested_permission")
  }
}
