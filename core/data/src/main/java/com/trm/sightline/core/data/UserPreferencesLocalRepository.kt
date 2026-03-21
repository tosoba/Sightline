package com.trm.sightline.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trm.sightline.core.domain.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "user_preferences")

class UserPreferencesLocalRepository
@Inject
constructor(@ApplicationContext private val context: Context) : UserPreferencesRepository {
  override suspend fun getCustomLocationAddress(): String? =
    context.dataStore.data.map { preferences -> preferences[CUSTOM_LOCATION_ADDRESS] }.firstOrNull()

  override fun getUserLocationEnabled(): Flow<Boolean> =
    context.dataStore.data.map { preferences -> preferences[USER_LOCATION_ENABLED] ?: false }

  override suspend fun setCustomLocationAddress(address: String?) {
    context.dataStore.edit { preferences ->
      address?.takeIf(String::isNotBlank)?.let { preferences[CUSTOM_LOCATION_ADDRESS] = it }
        ?: preferences.remove(CUSTOM_LOCATION_ADDRESS)
    }
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[USER_LOCATION_ENABLED] = userLocationEnabled
    }
  }

  companion object {
    private val USER_LOCATION_ENABLED = booleanPreferencesKey("user_location_enabled")
    private val CUSTOM_LOCATION_ADDRESS = stringPreferencesKey("custom_location_address")
  }
}
