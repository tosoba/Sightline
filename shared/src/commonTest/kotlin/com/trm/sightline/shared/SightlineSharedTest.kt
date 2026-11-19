package com.trm.sightline.shared

import com.trm.sightline.core.common.location.LocationAuthorization
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.common.location.LocationSettingsState
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SightlineSharedTest {
  private val krakow = GeoLocation(latitude = 50.0614, longitude = 19.9366)
  private val nearPlace =
    Place(
      id = "near",
      name = "Near",
      latitude = 50.0620,
      longitude = 19.9370,
      category = PlaceCategory.FoodDrink,
    )
  private val farPlace =
    Place(
      id = "far",
      name = "Far",
      latitude = 50.1000,
      longitude = 19.9800,
      category = PlaceCategory.FoodDrink,
    )

  private val placesRepository = FakePlacesRepository(listOf(farPlace, nearPlace))
  private val userPreferencesRepository = FakeUserPreferencesRepository()
  private val shared =
    SightlineShared(
      placesRepository = placesRepository,
      locationProvider = FakeLocationProvider(),
      userPreferencesRepository = userPreferencesRepository,
    )

  @Test
  fun nearbyPlacesAreOrderedByDistanceFromTheGivenLocation() = runTest {
    val places = shared.nearbyPlaces(PlaceCategory.FoodDrink, krakow, radiusMeters = 1_000f)

    assertEquals(listOf(nearPlace.id, farPlace.id), places.map(Place::id))
  }

  @Test
  fun nearbyPlacesQueryTheRequestedCategoryLocationAndRadius() = runTest {
    shared.nearbyPlaces(PlaceCategory.ArtsCulture, krakow, radiusMeters = 500f)

    assertEquals(
      listOf(
        PlaceQuery(
          category = PlaceCategory.ArtsCulture,
          latitude = krakow.latitude,
          longitude = krakow.longitude,
          radiusMeters = 500f,
        )
      ),
      placesRepository.queries,
    )
  }

  @Test
  fun categoriesExposeEveryPlaceCategory() {
    assertEquals(PlaceCategory.entries, shared.categories)
  }

  @Test
  fun searchRadiusPreferencesAreDelegatedToTheRepository() = runTest {
    shared.preferences.setSearchRadius(PlaceSearchRadius.TwoKilometers)

    assertEquals(PlaceSearchRadius.TwoKilometers, shared.preferences.searchRadius.first())
  }
}

private data class PlaceQuery(
  val category: PlaceCategory,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Float,
)

private class FakePlacesRepository(private val places: List<Place>) : PlacesRepository {
  val queries = mutableListOf<PlaceQuery>()

  override suspend fun fetchPlaces(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): List<Place> {
    queries += PlaceQuery(category, latitude, longitude, radiusMeters)
    return places
  }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
  private val searchRadius = MutableStateFlow<PlaceSearchRadius>(PlaceSearchRadius.OneKilometer)

  override fun getCustomLocation(): Flow<CustomLocation?> = emptyFlow()

  override fun getUserLocationEnabled(): Flow<Boolean> = emptyFlow()

  override fun getLastMapPosition(): Flow<MapCameraPosition?> = emptyFlow()

  override suspend fun setCustomLocation(customLocation: CustomLocation) = Unit

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) = Unit

  override suspend fun setLastMapPosition(position: MapCameraPosition) = Unit

  override fun getSearchRadius(): Flow<PlaceSearchRadius> = searchRadius

  override suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    searchRadius.value = radius
  }
}

private class FakeLocationProvider : LocationProvider {
  override fun locationUpdates(): Flow<GeoLocation> = emptyFlow()

  override fun isLocationEnabled(): Flow<Boolean> = emptyFlow()

  override val authorizationState: Flow<LocationAuthorization> = emptyFlow()

  override suspend fun checkLocationSettings(): LocationSettingsState =
    LocationSettingsState.Enabled
}
