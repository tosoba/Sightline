package com.trm.sightline.shared

import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class PreferencesFacadeTest {
  private val repository = InMemoryUserPreferencesRepository()
  private val facade = PreferencesFacade(repository)

  @Test
  fun lastMapPositionRoundTripsThroughTheRepository() = runTest {
    val position =
      MapCameraPosition(
        latitude = 50.0614,
        longitude = 19.9366,
        zoom = 12.5,
        bearing = 30.0,
        pitch = 45.0,
      )

    facade.setLastMapPosition(position)

    assertEquals(position, facade.lastMapPosition.first())
  }

  @Test
  fun userLocationEnabledRoundTripsThroughTheRepository() = runTest {
    facade.setUserLocationEnabled(true)

    assertEquals(true, facade.userLocationEnabled.first())
  }
}

private class InMemoryUserPreferencesRepository : UserPreferencesRepository {
  private val lastMapPosition = MutableStateFlow<MapCameraPosition?>(null)
  private val userLocationEnabled = MutableStateFlow(false)
  private val searchRadius = MutableStateFlow<PlaceSearchRadius>(PlaceSearchRadius.OneKilometer)
  private val customLocation = MutableStateFlow<CustomLocation?>(null)

  override fun getCustomLocation(): Flow<CustomLocation?> = customLocation

  override fun getUserLocationEnabled(): Flow<Boolean> = userLocationEnabled

  override fun getLastMapPosition(): Flow<MapCameraPosition?> = lastMapPosition

  override fun getSearchRadius(): Flow<PlaceSearchRadius> = searchRadius

  override suspend fun setCustomLocation(customLocation: CustomLocation) {
    this.customLocation.value = customLocation
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    this.userLocationEnabled.value = userLocationEnabled
  }

  override suspend fun setLastMapPosition(position: MapCameraPosition) {
    lastMapPosition.value = position
  }

  override suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    searchRadius.value = radius
  }

  override suspend fun reset() {
    customLocation.value = null
    userLocationEnabled.value = false
    lastMapPosition.value = null
    searchRadius.value = PlaceSearchRadius.OneKilometer
  }
}
