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

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "user_preferences")

class UserPreferencesLocalRepository
@Inject
constructor(@ApplicationContext private val context: Context) : UserPreferencesRepository {
  override fun getUserLocation(): Flow<Boolean> =
    context.dataStore.data.map { preferences -> preferences[USER_LOCATION] ?: false }

  override suspend fun setUserLocation(userLocation: Boolean) {
    context.dataStore.edit { preferences -> preferences[USER_LOCATION] = userLocation }
  }

  companion object {
    private val USER_LOCATION = booleanPreferencesKey("user_location")
  }
}
