package com.trm.sightline.core.domain

import com.trm.sightline.core.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  suspend fun getCustomLocation(): SearchResult?

  fun getUserLocationEnabled(): Flow<Boolean>

  suspend fun setCustomLocation(searchResult: SearchResult?)

  suspend fun setUserLocationEnabled(userLocationEnabled: Boolean)
}
