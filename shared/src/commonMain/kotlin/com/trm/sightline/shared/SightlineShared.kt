package com.trm.sightline.shared

import com.trm.sightline.core.common.location.LocationAuthorization
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.common.util.location
import com.trm.sightline.core.data.UserPreferencesDirectory
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.flow.Flow

class SightlineShared
internal constructor(
  private val placesRepository: PlacesRepository,
  private val locationProvider: LocationProvider,
  private val userPreferencesRepository: UserPreferencesRepository,
) {
  val categories: List<PlaceCategory> = PlaceCategory.entries

  val preferences: PreferencesFacade = PreferencesFacade(userPreferencesRepository)

  val authorizationState: Flow<LocationAuthorization> = locationProvider.authorizationState

  val locationEnabled: Flow<Boolean> = locationProvider.isLocationEnabled()

  val locationUpdates: Flow<GeoLocation> = locationProvider.locationUpdates()

  suspend fun nearbyPlaces(
    category: PlaceCategory,
    location: GeoLocation,
    radiusMeters: Float,
  ): List<Place> =
    placesRepository
      .fetchPlaces(
        category = category,
        latitude = location.latitude,
        longitude = location.longitude,
        radiusMeters = radiusMeters,
      )
      .sortedBy { place -> place.location.distanceTo(location) }

  /**
   * Restores every shared preference to its default through the real repository.
   * The iOS host calls this only for the `-resetPreferences` UI-test launch argument.
   */
  suspend fun resetPreferencesForTesting() {
    userPreferencesRepository.reset()
  }

  companion object {
    fun create(): SightlineShared {
      val graph = createGraphFactory<IosAppGraph.Factory>().create(userPreferencesDirectory())
      return SightlineShared(
        placesRepository = graph.placesRepository,
        locationProvider = graph.locationProvider,
        userPreferencesRepository = graph.userPreferencesRepository,
      )
    }
  }
}

internal expect fun userPreferencesDirectory(): UserPreferencesDirectory
