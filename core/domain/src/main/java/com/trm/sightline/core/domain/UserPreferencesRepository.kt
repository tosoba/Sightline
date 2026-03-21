package com.trm.sightline.core.domain

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  suspend fun getCustomLocationAddress(): String?

  fun getUserLocationEnabled(): Flow<Boolean>

  suspend fun setCustomLocationAddress(address: String?)

  suspend fun setUserLocationEnabled(userLocationEnabled: Boolean)
}
