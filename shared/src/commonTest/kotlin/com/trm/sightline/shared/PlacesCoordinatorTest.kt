package com.trm.sightline.shared

import com.trm.sightline.core.common.RequestError
import com.trm.sightline.core.common.location.LocationAuthorization
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.common.location.LocationSettingsState
import com.trm.sightline.core.domain.AddressRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class PlacesCoordinatorTest {
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

  @Test
  fun startsWithNoCategoriesAndNoRequests() = runTest {
    val harness = harness()
    runCurrent()

    assertEquals(emptyMap(), harness.coordinator.places.value)
    assertEquals(emptyList(), harness.coordinator.allPlaces.value)
    assertNull(harness.coordinator.activeLocation.value)
    assertEquals(emptyList(), harness.places.queries)
  }

  @Test
  fun togglingACategoryOnShowsLoadingAndFetchesAfterOneSecond() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()
    assertEquals(krakow, harness.coordinator.activeLocation.value)

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)

    assertEquals(
      CategoryPlacesState.Loading,
      harness.coordinator.places.value[PlaceCategory.FoodDrink],
    )

    runCurrent()
    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), harness.places.queries)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(
      listOf(
        RecordedPlaceQuery(
          category = PlaceCategory.FoodDrink,
          latitude = krakow.latitude,
          longitude = krakow.longitude,
          radiusMeters = 1_000f,
        )
      ),
      harness.places.queries,
    )
    assertEquals(
      CategoryPlacesState.Loaded(emptyList()),
      harness.coordinator.places.value[PlaceCategory.FoodDrink],
    )
  }

  @Test
  fun togglingACategoryOffBeforeTheDebounceCancelsTheRequest() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(500)
    runCurrent()
    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)

    assertNull(harness.coordinator.places.value[PlaceCategory.FoodDrink])

    advanceTimeBy(5_000)
    runCurrent()

    assertEquals(emptyList(), harness.places.queries)
    assertTrue(harness.coordinator.places.value.isEmpty())
  }

  @Test
  fun togglingACategoryOffCancelsAnInFlightRequest() = runTest {
    val harness = harness(preferences = prefsWithKrakow(), places = RecordingPlacesRepository())
    harness.places.latency = 10.seconds
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(1, harness.places.queries.size)

    advanceTimeBy(2_000)
    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(20_000)
    runCurrent()

    assertNull(harness.coordinator.places.value[PlaceCategory.FoodDrink])
    assertEquals(emptyList(), harness.coordinator.allPlaces.value)
  }

  @Test
  fun placesAreSortedByDistanceFromTheActiveLocation() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    harness.places.results = { listOf(farPlace, nearPlace) }
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()

    assertEquals(
      CategoryPlacesState.Loaded(listOf(nearPlace, farPlace)),
      harness.coordinator.places.value[PlaceCategory.FoodDrink],
    )
  }

  @Test
  fun everyLoadedCategoryFlowsIntoAllPlaces() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    harness.places.results = { category ->
      listOf(nearPlace.copy(id = category.name, category = category))
    }
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    harness.coordinator.onTogglePlaceCategory(PlaceCategory.Health)
    advanceTimeBy(1_000)
    runCurrent()

    assertEquals(
      setOf(PlaceCategory.FoodDrink.name, PlaceCategory.Health.name),
      harness.coordinator.allPlaces.value.map(Place::id).toSet(),
    )
  }

  @Test
  fun aTimedOutFetchReportsIoAndClearsTheCategoryAndError() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    harness.places.latency = 40.seconds
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    advanceTimeBy(34_000)
    runCurrent()
    assertNull(harness.coordinator.error.value)

    advanceTimeBy(2_000)
    runCurrent()

    assertEquals(RequestError.IO, harness.coordinator.error.value)
    assertNull(harness.coordinator.places.value[PlaceCategory.FoodDrink])

    advanceTimeBy(4_000)
    runCurrent()

    assertNull(harness.coordinator.error.value)
  }

  @Test
  fun aFailedFetchReportsARequestErrorAndDropsTheCategory() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    harness.places.failure = { IllegalStateException("boom") }
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()

    assertTrue(harness.coordinator.error.value is RequestError.Other)
    assertNull(harness.coordinator.places.value[PlaceCategory.FoodDrink])
  }

  @Test
  fun activeLocationPrefersUserLocationAndKeepsTheSavedCustomLocation() = runTest {
    val harness = harness(preferences = prefsWithKrakow(userLocationEnabled = true))
    runCurrent()
    assertEquals(true, harness.coordinator.userLocationEnabled.value)
    assertNull(harness.coordinator.activeLocation.value)

    harness.location.emit(warsaw)
    runCurrent()

    assertEquals(warsaw, harness.coordinator.userLocation.value)
    assertEquals(warsaw, harness.coordinator.activeLocation.value)
    assertEquals(
      GeoLocation(latitude = krakow.latitude, longitude = krakow.longitude),
      harness.coordinator.customLocation.value,
    )

    harness.coordinator.setUserLocationEnabled(false)
    runCurrent()

    assertEquals(krakow, harness.coordinator.activeLocation.value)
    assertEquals(krakowAddress, harness.coordinator.customLocationAddress.value)
    assertEquals(savedKrakow, harness.preferences.savedCustomLocation)
    assertEquals(emptyList(), harness.preferences.customLocationWrites)
    assertEquals(listOf(false), harness.preferences.userLocationEnabledWrites)
  }

  @Test
  fun clearingTheActiveLocationClearsPlacesAndRestoresThemWhenItReturns() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    assertTrue(
      harness.coordinator.places.value[PlaceCategory.FoodDrink] is CategoryPlacesState.Loaded
    )

    harness.coordinator.setUserLocationEnabled(true)
    runCurrent()

    assertNull(harness.coordinator.activeLocation.value)
    assertTrue(harness.coordinator.places.value.isEmpty())
    assertEquals(emptyList(), harness.coordinator.allPlaces.value)

    harness.location.emit(warsaw)
    runCurrent()

    assertEquals(warsaw, harness.coordinator.activeLocation.value)
    assertEquals(
      RecordedPlaceQuery(
        category = PlaceCategory.FoodDrink,
        latitude = warsaw.latitude,
        longitude = warsaw.longitude,
        radiusMeters = 1_000f,
      ),
      harness.places.queries.last(),
    )
    assertTrue(
      harness.coordinator.places.value[PlaceCategory.FoodDrink] is CategoryPlacesState.Loaded
    )
  }

  @Test
  fun changingTheSearchRadiusRefetchesActiveCategories() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(1_000f, harness.places.queries.single().radiusMeters)

    harness.coordinator.setSearchRadius(PlaceSearchRadius.TwoKilometers)
    runCurrent()

    assertEquals(PlaceSearchRadius.TwoKilometers, harness.coordinator.searchRadius.value)
    assertEquals(2_000f, harness.places.queries.last().radiusMeters)
    assertEquals(2, harness.places.queries.size)
    assertEquals(
      listOf(PlaceSearchRadius.TwoKilometers),
      harness.preferences.searchRadiusWrites,
    )
  }

  @Test
  fun movingTheCustomLocationRefetchesActiveCategories() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()

    harness.coordinator.onCustomLocationSearchResultClick(
      CustomLocation(
        latitude = warsaw.latitude,
        longitude = warsaw.longitude,
        address = "Warsaw, Poland",
        timestamp = 2L,
      )
    )
    runCurrent()

    assertEquals(warsaw, harness.coordinator.activeLocation.value)
    assertEquals(warsaw.latitude, harness.places.queries.last().latitude)
    assertEquals(2, harness.places.queries.size)
  }

  @Test
  fun reverseGeocodingWaitsOneSecondBeforeReportingTheAddress() = runTest {
    val harness = harness(preferences = prefsWithKrakow(userLocationEnabled = true))
    harness.address.address = { _, _ -> "Warsaw, Poland" }
    runCurrent()

    harness.location.emit(warsaw)
    runCurrent()

    assertEquals(warsaw, harness.coordinator.userLocation.value)
    assertEquals(
      LocationAddressState.Loading(""),
      harness.coordinator.userLocationAddress.value,
    )

    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), harness.address.reverseGeocodeCalls)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(warsaw.latitude to warsaw.longitude, harness.address.reverseGeocodeCalls.single())
    assertEquals(
      LocationAddressState.Loaded("Warsaw, Poland"),
      harness.coordinator.userLocationAddress.value,
    )
  }

  @Test
  fun aNewUserLocationKeepsThePreviousAddressWhileGeocoding() = runTest {
    val harness = harness(preferences = prefsWithKrakow(userLocationEnabled = true))
    harness.address.address = { _, _ -> "Warsaw, Poland" }
    runCurrent()

    harness.location.emit(warsaw)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(
      LocationAddressState.Loaded("Warsaw, Poland"),
      harness.coordinator.userLocationAddress.value,
    )

    harness.location.emit(krakow)
    runCurrent()

    assertEquals(
      LocationAddressState.Loading("Warsaw, Poland"),
      harness.coordinator.userLocationAddress.value,
    )
  }

  @Test
  fun togglingOneCategoryOffLeavesTheOtherCategoriesLoaded() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    harness.places.results = { category ->
      listOf(nearPlace.copy(id = category.name, category = category))
    }
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    harness.coordinator.onTogglePlaceCategory(PlaceCategory.Health)
    advanceTimeBy(1_000)
    runCurrent()
    assertEquals(2, harness.coordinator.places.value.size)

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)

    assertEquals(setOf(PlaceCategory.Health), harness.coordinator.places.value.keys)
    assertEquals(
      listOf(PlaceCategory.Health.name),
      harness.coordinator.allPlaces.value.map(Place::id),
    )
  }

  @Test
  fun aNewUserLocationRestartsThePendingReverseGeocode() = runTest {
    val harness = harness(preferences = prefsWithKrakow(userLocationEnabled = true))
    runCurrent()

    harness.location.emit(krakow)
    runCurrent()
    advanceTimeBy(500)
    harness.location.emit(warsaw)
    runCurrent()
    advanceTimeBy(999)
    runCurrent()

    assertEquals(emptyList(), harness.address.reverseGeocodeCalls)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(warsaw.latitude to warsaw.longitude, harness.address.reverseGeocodeCalls.single())
  }

  @Test
  fun turningLocationServicesOffDisablesUserLocation() = runTest {
    val harness = harness(preferences = prefsWithKrakow(userLocationEnabled = true))
    runCurrent()
    assertEquals(true, harness.coordinator.userLocationEnabled.value)

    harness.location.setLocationEnabled(false)
    runCurrent()

    assertEquals(false, harness.coordinator.userLocationEnabled.value)
    assertEquals(listOf(false), harness.preferences.userLocationEnabledWrites)
  }

  @Test
  fun customLocationSearchValidatesTheTrimmedQueryLength() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.setCustomLocationAddress("")
    assertEquals(
      CustomLocationSearchState.Idle,
      harness.coordinator.customLocationSearchResults.value,
    )

    harness.coordinator.setCustomLocationAddress("   ")
    assertEquals(
      CustomLocationSearchState.Idle,
      harness.coordinator.customLocationSearchResults.value,
    )

    harness.coordinator.setCustomLocationAddress("ab")
    assertEquals(
      CustomLocationSearchState.QueryTooShort,
      harness.coordinator.customLocationSearchResults.value,
    )

    advanceTimeBy(5_000)
    runCurrent()

    assertEquals(emptyList(), harness.address.searchCalls)
  }

  @Test
  fun customLocationSearchDebouncesAndLoadsTheResults() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    val results =
      listOf(
        CustomLocation(
          latitude = warsaw.latitude,
          longitude = warsaw.longitude,
          address = "Warsaw, Poland",
          timestamp = 2L,
        )
      )
    harness.address.results = results
    runCurrent()

    harness.coordinator.setCustomLocationAddress("  wars  ")

    assertEquals(
      CustomLocationSearchState.Loading,
      harness.coordinator.customLocationSearchResults.value,
    )

    runCurrent()
    advanceTimeBy(999)
    runCurrent()
    assertEquals(emptyList(), harness.address.searchCalls)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(
      RecordedAddressQuery(query = "wars", limit = 100),
      harness.address.searchCalls.single(),
    )
    assertEquals(
      CustomLocationSearchState.Loaded(results),
      harness.coordinator.customLocationSearchResults.value,
    )
  }

  @Test
  fun typingAgainRestartsThePendingCustomLocationSearch() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.setCustomLocationAddress("kra")
    advanceTimeBy(500)
    harness.coordinator.setCustomLocationAddress("krakow")
    advanceTimeBy(999)
    runCurrent()

    assertEquals(emptyList(), harness.address.searchCalls)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(
      RecordedAddressQuery(query = "krakow", limit = 100),
      harness.address.searchCalls.single(),
    )
  }

  @Test
  fun selectingACustomLocationSearchResultPersistsAndActivatesIt() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()
    val selection =
      CustomLocation(
        latitude = warsaw.latitude,
        longitude = warsaw.longitude,
        address = "Warsaw, Poland",
        timestamp = 2L,
      )

    harness.coordinator.onCustomLocationSearchResultClick(selection)
    runCurrent()

    assertEquals(
      GeoLocation(latitude = warsaw.latitude, longitude = warsaw.longitude),
      harness.coordinator.activeLocation.value,
    )
    assertEquals("Warsaw, Poland", harness.coordinator.customLocationAddress.value)
    assertEquals(
      CustomLocationSearchState.Idle,
      harness.coordinator.customLocationSearchResults.value,
    )
    assertEquals(listOf(selection), harness.preferences.customLocationWrites)
  }

  @Test
  fun editingTheCustomLocationSearchClearsPlacesAndCancelRestoresTheSavedLocation() = runTest {
    val harness = harness(preferences = prefsWithKrakow())
    runCurrent()

    harness.coordinator.onTogglePlaceCategory(PlaceCategory.FoodDrink)
    advanceTimeBy(1_000)
    runCurrent()
    assertTrue(
      harness.coordinator.places.value[PlaceCategory.FoodDrink] is CategoryPlacesState.Loaded
    )

    harness.coordinator.setCustomLocationAddress("Warsaw")
    runCurrent()

    assertNull(harness.coordinator.activeLocation.value)
    assertNull(harness.coordinator.customLocation.value)
    assertTrue(harness.coordinator.places.value.isEmpty())

    harness.coordinator.cancelCustomLocationSearch()
    runCurrent()

    assertEquals(krakow, harness.coordinator.activeLocation.value)
    assertEquals(krakowAddress, harness.coordinator.customLocationAddress.value)
    assertEquals(
      CustomLocationSearchState.Idle,
      harness.coordinator.customLocationSearchResults.value,
    )
    assertTrue(
      harness.coordinator.places.value[PlaceCategory.FoodDrink] is CategoryPlacesState.Loaded
    )
  }

  @Test
  fun saveMapPositionPersistsAfterOneSecondOfQuiet() = runTest {
    val harness = harness()
    val first =
      MapCameraPosition(latitude = 50.0, longitude = 19.9, zoom = 12.0, bearing = 0.0, pitch = 0.0)
    val second =
      MapCameraPosition(latitude = 50.1, longitude = 20.0, zoom = 13.0, bearing = 10.0, pitch = 5.0)
    runCurrent()

    harness.coordinator.saveMapPosition(first)
    advanceTimeBy(500)
    harness.coordinator.saveMapPosition(second)
    advanceTimeBy(999)
    runCurrent()

    assertEquals(emptyList(), harness.preferences.lastMapPositionWrites)

    advanceTimeBy(1)
    runCurrent()

    assertEquals(listOf(second), harness.preferences.lastMapPositionWrites)
    assertEquals(second, harness.coordinator.lastMapPosition.value)
  }

  private fun TestScope.harness(
    places: RecordingPlacesRepository = RecordingPlacesRepository(),
    address: RecordingAddressRepository = RecordingAddressRepository(),
    preferences: StubUserPreferencesRepository = StubUserPreferencesRepository(),
    location: StubLocationProvider = StubLocationProvider(),
  ): Harness =
    Harness(
      coordinator =
        PlacesCoordinator(
          placesRepository = places,
          addressRepository = address,
          userPreferencesRepository = preferences,
          locationProvider = location,
          scope = backgroundScope,
        ),
      places = places,
      address = address,
      preferences = preferences,
      location = location,
    )

  private fun prefsWithKrakow(userLocationEnabled: Boolean = false) =
    StubUserPreferencesRepository(
      customLocation = savedKrakow,
      userLocationEnabled = userLocationEnabled,
    )
}

private class Harness(
  val coordinator: PlacesCoordinator,
  val places: RecordingPlacesRepository,
  val address: RecordingAddressRepository,
  val preferences: StubUserPreferencesRepository,
  val location: StubLocationProvider,
)

private data class RecordedPlaceQuery(
  val category: PlaceCategory,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Float,
)

private data class RecordedAddressQuery(val query: String, val limit: Int)

private class RecordingPlacesRepository : PlacesRepository {
  val queries = mutableListOf<RecordedPlaceQuery>()
  var results: (PlaceCategory) -> List<Place> = { emptyList() }
  var latency: Duration = Duration.ZERO
  var failure: (PlaceCategory) -> Exception? = { null }

  override suspend fun fetchPlaces(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): List<Place> {
    queries += RecordedPlaceQuery(category, latitude, longitude, radiusMeters)
    if (latency > Duration.ZERO) delay(latency)
    failure(category)?.let { throw it }
    return results(category)
  }
}

private class RecordingAddressRepository : AddressRepository {
  val reverseGeocodeCalls = mutableListOf<Pair<Double, Double>>()
  val searchCalls = mutableListOf<RecordedAddressQuery>()
  var address: (Double, Double) -> String? = { latitude, longitude -> "$latitude,$longitude" }
  var results: List<CustomLocation> = emptyList()
  var latency: Duration = Duration.ZERO

  override suspend fun getAddress(latitude: Double, longitude: Double): String? {
    reverseGeocodeCalls += latitude to longitude
    if (latency > Duration.ZERO) delay(latency)
    return address(latitude, longitude)
  }

  override suspend fun search(query: String, limit: Int): List<CustomLocation> {
    searchCalls += RecordedAddressQuery(query, limit)
    if (latency > Duration.ZERO) delay(latency)
    return results
  }
}

private class StubUserPreferencesRepository(
  customLocation: CustomLocation? = null,
  userLocationEnabled: Boolean = false,
  lastMapPosition: MapCameraPosition? = null,
  searchRadius: PlaceSearchRadius = PlaceSearchRadius.OneKilometer,
) : UserPreferencesRepository {
  private val customLocation = MutableStateFlow(customLocation)
  private val userLocationEnabled = MutableStateFlow(userLocationEnabled)
  private val lastMapPosition = MutableStateFlow(lastMapPosition)
  private val searchRadius = MutableStateFlow(searchRadius)

  val customLocationWrites = mutableListOf<CustomLocation>()
  val userLocationEnabledWrites = mutableListOf<Boolean>()
  val lastMapPositionWrites = mutableListOf<MapCameraPosition>()
  val searchRadiusWrites = mutableListOf<PlaceSearchRadius>()

  val savedCustomLocation: CustomLocation?
    get() = customLocation.value

  override fun getCustomLocation(): Flow<CustomLocation?> = customLocation

  override fun getUserLocationEnabled(): Flow<Boolean> = userLocationEnabled

  override fun getLastMapPosition(): Flow<MapCameraPosition?> = lastMapPosition

  override fun getSearchRadius(): Flow<PlaceSearchRadius> = searchRadius

  override suspend fun setCustomLocation(customLocation: CustomLocation) {
    customLocationWrites += customLocation
    this.customLocation.value = customLocation
  }

  override suspend fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    userLocationEnabledWrites += userLocationEnabled
    this.userLocationEnabled.value = userLocationEnabled
  }

  override suspend fun setLastMapPosition(position: MapCameraPosition) {
    lastMapPositionWrites += position
    lastMapPosition.value = position
  }

  override suspend fun setSearchRadius(radius: PlaceSearchRadius) {
    searchRadiusWrites += radius
    searchRadius.value = radius
  }

  override suspend fun reset() {
    customLocation.value = null
    userLocationEnabled.value = false
    lastMapPosition.value = null
    searchRadius.value = PlaceSearchRadius.OneKilometer
  }
}

private class StubLocationProvider : LocationProvider {
  private val updates = MutableSharedFlow<GeoLocation>(extraBufferCapacity = 8)
  private val locationEnabled = MutableStateFlow(true)
  private val authorization = MutableStateFlow<LocationAuthorization>(LocationAuthorization.Granted)

  fun emit(location: GeoLocation) {
    updates.tryEmit(location)
  }

  fun setLocationEnabled(enabled: Boolean) {
    locationEnabled.value = enabled
  }

  override fun locationUpdates(): Flow<GeoLocation> = updates

  override fun isLocationEnabled(): Flow<Boolean> = locationEnabled

  override val authorizationState: Flow<LocationAuthorization> = authorization

  override suspend fun checkLocationSettings(): LocationSettingsState =
    LocationSettingsState.Enabled
}
