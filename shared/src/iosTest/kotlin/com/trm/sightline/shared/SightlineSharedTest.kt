package com.trm.sightline.shared

import com.trm.sightline.core.common.location.LocationAuthorization
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.common.location.LocationSettingsState
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.domain.places.CategoryPlacesState
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SightlineSharedTest {
  private val krakow = GeoLocation(latitude = 50.0614, longitude = 19.9366)
  private val warsaw = GeoLocation(latitude = 52.2297, longitude = 21.0122)
  private val krakowAddress = "Kraków, Poland"
  private val savedKrakow =
    CustomLocation(
      latitude = krakow.latitude,
      longitude = krakow.longitude,
      address = krakowAddress,
      timestamp = 1L,
    )
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

  @AfterTest
  fun restoreMainDispatcher() {
    Dispatchers.resetMain()
  }

  @Test
  fun togglingACategoryFetchesFromTheActiveCustomLocationAfterTheDebounce() = runTest {
    installTestMain()
    val places = FakePlacesRepository(listOf(farPlace, nearPlace))
    val shared = shared(places = places, preferences = FakeUserPreferencesRepository(customLocation = savedKrakow))
    val coordinator = shared.placesCoordinator
    runCurrent()

    assertEquals(krakow, coordinator.activeLocation.value)

    shared.placesCoordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)

    assertEquals(
      CategoryPlacesState.Loading,
      shared.placesCoordinator.places.value[PlaceCategory.FoodDrink],
    )

    advanceTimeBy(999)
    runCurrent()
    assertTrue(places.queries.isEmpty())

    advanceTimeBy(1)
    runCurrent()

    assertEquals(
      listOf(PlaceQuery(PlaceCategory.FoodDrink, krakow.latitude, krakow.longitude, 1_000f)),
      places.queries,
    )
    assertEquals(
      CategoryPlacesState.Loaded(listOf(nearPlace, farPlace)),
      shared.placesCoordinator.places.value[PlaceCategory.FoodDrink],
    )
    assertEquals(
      listOf(nearPlace, farPlace),
      shared.placesCoordinator.state.value.allPlaces,
    )
  }

  @Test
  fun activeLocationFollowsUserLocationWhileTrackingIsEnabled() = runTest {
    installTestMain()
    val preferences = FakeUserPreferencesRepository(customLocation = savedKrakow, userLocationEnabled = true)
    val location = FakeLocationProvider()
    val shared = shared(preferences = preferences, location = location)
    val coordinator = shared.placesCoordinator
    runCurrent()

    assertNull(coordinator.activeLocation.value)

    location.emit(warsaw)
    runCurrent()

    assertEquals(warsaw, coordinator.activeLocation.value)
    assertEquals(warsaw, coordinator.state.value.activeLocation)
    assertEquals(krakow, coordinator.customLocation.value)
  }

  @Test
  fun placesCoordinatorIsASingleInjectedInstance() = runTest {
    installTestMain()
    val shared = shared()

    assertSame(shared.placesCoordinator, shared.placesCoordinator)
  }

  @Test
  fun consolidatedStateEmitsTheActiveLocationAndPlaces() = runTest {
    installTestMain()
    val places = FakePlacesRepository(listOf(nearPlace))
    val shared = shared(places = places, preferences = FakeUserPreferencesRepository(customLocation = savedKrakow))
    val coordinator = shared.placesCoordinator
    runCurrent()

    coordinator.onTogglePlaceCategory(PlaceCategory.ArtsCulture)
    advanceTimeBy(1_000)
    runCurrent()

    val state = coordinator.state.value
    assertEquals(krakow, state.activeLocation)
    assertEquals(krakow, state.customLocation)
    assertEquals(krakowAddress, state.customLocationAddress)
    assertEquals(mapOf(PlaceCategory.ArtsCulture to CategoryPlacesState.Loaded(listOf(nearPlace))), state.places)
    assertEquals(listOf(nearPlace), state.allPlaces)
  }

  @Test
  fun authorizationStateAndLocationEnabledExposeTheLocationProvider() = runTest {
    installTestMain()
    val location = FakeLocationProvider()
    val shared = shared(location = location)

    assertEquals(LocationAuthorization.Granted, shared.authorizationState.first())
    assertEquals(true, shared.locationEnabled.first())
  }

  @Test
  fun resetPreferencesForTestingRestoresEveryDefault() = runTest {
    installTestMain()
    val preferences = FakeUserPreferencesRepository(customLocation = savedKrakow)
    val shared = shared(preferences = preferences)
    preferences.setSearchRadius(PlaceSearchRadius.TwoKilometers)
    preferences.setUserLocationEnabled(true)
    preferences.setLastMapPosition(
      MapCameraPosition(
        latitude = krakow.latitude,
        longitude = krakow.longitude,
        zoom = 12.5,
        bearing = 30.0,
        pitch = 45.0,
      )
    )

    shared.resetPreferencesForTesting()

    assertNull(preferences.getCustomLocation().first())
    assertEquals(false, preferences.getUserLocationEnabled().first())
    assertNull(preferences.getLastMapPosition().first())
    assertEquals(PlaceSearchRadius.OneKilometer, preferences.getSearchRadius().first())
  }

  private fun TestScope.installTestMain() {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  }

  private fun shared(
    places: FakePlacesRepository = FakePlacesRepository(emptyList()),
    preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
    location: FakeLocationProvider = FakeLocationProvider(),
  ): SightlineShared =
    SightlineShared(
      placesRepository = places,
      addressRepository = FakeAddressRepository(),
      locationProvider = location,
      userPreferencesRepository = preferences,
    )
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

private class FakeAddressRepository : AddressRepository {
  override suspend fun getAddress(latitude: Double, longitude: Double): String? = null

  override suspend fun search(query: String, limit: Int): List<CustomLocation> = emptyList()
}

private class FakeUserPreferencesRepository(
  customLocation: CustomLocation? = null,
  userLocationEnabled: Boolean = false,
  lastMapPosition: MapCameraPosition? = null,
  searchRadius: PlaceSearchRadius = PlaceSearchRadius.OneKilometer,
) : UserPreferencesRepository {
  private val customLocation = MutableStateFlow(customLocation)
  private val userLocationEnabled = MutableStateFlow(userLocationEnabled)
  private val lastMapPosition = MutableStateFlow(lastMapPosition)
  private val searchRadius = MutableStateFlow(searchRadius)

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

private class FakeLocationProvider : LocationProvider {
  private val updates = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 8)
  private val locationEnabled = MutableStateFlow(true)

  fun emit(location: GeoLocation) {
    updates.tryEmit(location)
  }

  override fun locationUpdates(): Flow<GeoLocation> = updates

  override fun isLocationEnabled(): Flow<Boolean> = locationEnabled

  override val authorizationState: Flow<LocationAuthorization> =
    MutableStateFlow(LocationAuthorization.Granted)

  override suspend fun checkLocationSettings(): LocationSettingsState =
    LocationSettingsState.Enabled
}
