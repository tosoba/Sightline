package com.trm.sightline.core.domain.places

import com.trm.sightline.core.common.RequestError
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.common.toRequestError
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Owns the reactive places, location, and custom-location-search state shared by Android and iOS.
 * State is derived from the same repositories and preferences, exposed as fine-grained
 * `StateFlow`s plus the consolidated [state] snapshot, and mutated through the intents below.
 *
 * Intents are expected to be invoked from the main thread; every effect runs on [scope].
 */
class PlacesCoordinator(
  private val placesRepository: PlacesRepository,
  private val addressRepository: AddressRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val locationProvider: LocationProvider,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
  private val _places = MutableStateFlow<Map<PlaceCategory, CategoryPlacesState>>(emptyMap())
  val places: StateFlow<Map<PlaceCategory, CategoryPlacesState>> = _places.asStateFlow()

  private val _allPlaces = MutableStateFlow<List<Place>>(emptyList())
  val allPlaces: StateFlow<List<Place>> = _allPlaces.asStateFlow()

  private val _activeLocation = MutableStateFlow<GeoLocation?>(null)
  val activeLocation: StateFlow<GeoLocation?> = _activeLocation.asStateFlow()

  private val _userLocation = MutableStateFlow<GeoLocation?>(null)
  val userLocation: StateFlow<GeoLocation?> = _userLocation.asStateFlow()

  private val _userLocationAddress =
    MutableStateFlow<LocationAddressState>(LocationAddressState.Loaded(""))
  val userLocationAddress: StateFlow<LocationAddressState> = _userLocationAddress.asStateFlow()

  private val _customLocation = MutableStateFlow<GeoLocation?>(null)
  val customLocation: StateFlow<GeoLocation?> = _customLocation.asStateFlow()

  private val _customLocationAddress = MutableStateFlow("")
  val customLocationAddress: StateFlow<String> = _customLocationAddress.asStateFlow()

  private val _customLocationSearchResults =
    MutableStateFlow<CustomLocationSearchState>(CustomLocationSearchState.Idle)
  val customLocationSearchResults: StateFlow<CustomLocationSearchState> =
    _customLocationSearchResults.asStateFlow()

  private val _userLocationEnabled = MutableStateFlow(false)
  val userLocationEnabled: StateFlow<Boolean> = _userLocationEnabled.asStateFlow()

  private val _searchRadius = MutableStateFlow(PlaceSearchRadius.OneKilometer)
  val searchRadius: StateFlow<PlaceSearchRadius> = _searchRadius.asStateFlow()

  private val _lastMapPosition = MutableStateFlow<MapCameraPosition?>(null)
  val lastMapPosition: StateFlow<MapCameraPosition?> = _lastMapPosition.asStateFlow()

  private val _error = MutableStateFlow<RequestError?>(null)
  val error: StateFlow<RequestError?> = _error.asStateFlow()

  val state: StateFlow<PlacesUiState> =
    combine(
        combine(
          _places,
          _allPlaces,
          _activeLocation,
          _userLocation,
          _userLocationAddress,
          ::PlaceStateSnapshot,
        ),
        combine(
          _customLocation,
          _customLocationAddress,
          _customLocationSearchResults,
          _userLocationEnabled,
          _searchRadius,
          ::LocationStateSnapshot,
        ),
        _lastMapPosition,
        _error,
      ) { places, location, lastMapPosition, error ->
        PlacesUiState(
          places = places.places,
          allPlaces = places.allPlaces,
          activeLocation = places.activeLocation,
          userLocation = places.userLocation,
          userLocationAddress = places.userLocationAddress,
          customLocation = location.customLocation,
          customLocationAddress = location.customLocationAddress,
          customLocationSearchResults = location.customLocationSearchResults,
          userLocationEnabled = location.userLocationEnabled,
          searchRadius = location.searchRadius,
          lastMapPosition = lastMapPosition,
          error = error,
        )
      }
      .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = PlacesUiState())

  private val activeCategories = mutableSetOf<PlaceCategory>()
  private val fetchJobs = mutableMapOf<PlaceCategory, Job>()
  private var errorClearJob: Job? = null
  private var customLocationSearchJob: Job? = null
  private var mapPositionSaveJob: Job? = null
  private var locationServicesJob: Job? = null
  private var persistedCustomLocation: CustomLocation? = null
  private var isEditingCustomLocationAddress = false

  init {
    scope.launch { observeCustomLocationPreference() }
    scope.launch { observeUserLocationEnabledPreference() }
    scope.launch { observeSearchRadiusPreference() }
    scope.launch { observeLastMapPositionPreference() }
    locationServicesJob = scope.launch { observeLocationServices() }
    scope.launch { observeUserLocation() }
  }

  fun onTogglePlaceCategory(category: PlaceCategory) {
    if (activeCategories.contains(category)) deactivate(category) else activate(category)
  }

  fun setUserLocationEnabled(enabled: Boolean) {
    if (_userLocationEnabled.value != enabled) {
      _userLocationEnabled.value = enabled
      updateActiveLocation()
    }
    persist { userPreferencesRepository.setUserLocationEnabled(enabled) }
  }

  fun setSearchRadius(radius: PlaceSearchRadius) {
    if (_searchRadius.value != radius) {
      _searchRadius.value = radius
      refreshActiveCategories()
    }
    persist { userPreferencesRepository.setSearchRadius(radius) }
  }

  fun setCustomLocationAddress(query: String) {
    isEditingCustomLocationAddress = true
    _customLocationAddress.value = query
    updateCustomLocation(null)
    searchCustomLocation(query)
  }

  fun onCustomLocationSearchResultClick(location: CustomLocation) {
    isEditingCustomLocationAddress = false
    customLocationSearchJob?.cancel()
    _customLocationSearchResults.value = CustomLocationSearchState.Idle
    _customLocationAddress.value = location.address
    updateCustomLocation(GeoLocation(latitude = location.latitude, longitude = location.longitude))
    persist { userPreferencesRepository.setCustomLocation(location) }
  }

  fun cancelCustomLocationSearch() {
    isEditingCustomLocationAddress = false
    customLocationSearchJob?.cancel()
    _customLocationSearchResults.value = CustomLocationSearchState.Idle

    val persisted = persistedCustomLocation
    if (persisted == null) {
      _customLocationAddress.value = ""
      updateCustomLocation(null)
      return
    }
    _customLocationAddress.value = persisted.address
    updateCustomLocation(
      GeoLocation(latitude = persisted.latitude, longitude = persisted.longitude)
    )
  }

  fun saveMapPosition(position: MapCameraPosition) {
    mapPositionSaveJob?.cancel()
    mapPositionSaveJob =
      scope.launch {
        delay(MAP_POSITION_SAVE_DEBOUNCE)
        writePreferences { userPreferencesRepository.setLastMapPosition(position) }
      }
  }

  /**
   * Re-reads the platform location-services switch by restarting its observation. Providers without
   * change notifications (the iOS location provider) only report the current value when collected
   * again, and the restart keeps the auto-disable behavior when services stay off.
   */
  fun refreshLocationSettings() {
    locationServicesJob?.cancel()
    locationServicesJob = scope.launch { observeLocationServices() }
  }

  private suspend fun observeCustomLocationPreference() {
    userPreferencesRepository
      .getCustomLocation()
      .filterNotNull()
      .distinctUntilChanged()
      .collect { persisted ->
        persistedCustomLocation = persisted
        if (!isEditingCustomLocationAddress) {
          _customLocationAddress.value = persisted.address
        }
        updateCustomLocation(
          GeoLocation(latitude = persisted.latitude, longitude = persisted.longitude)
        )
      }
  }

  private suspend fun observeUserLocationEnabledPreference() {
    userPreferencesRepository
      .getUserLocationEnabled()
      .distinctUntilChanged()
      .collect { enabled ->
        if (_userLocationEnabled.value == enabled) return@collect
        _userLocationEnabled.value = enabled
        updateActiveLocation()
      }
  }

  private suspend fun observeSearchRadiusPreference() {
    userPreferencesRepository
      .getSearchRadius()
      .distinctUntilChanged()
      .collect { radius ->
        val changed = _searchRadius.value != radius
        _searchRadius.value = radius
        if (changed) refreshActiveCategories()
      }
  }

  private suspend fun observeLastMapPositionPreference() {
    userPreferencesRepository
      .getLastMapPosition()
      .distinctUntilChanged()
      .collect { position -> _lastMapPosition.value = position }
  }

  private suspend fun observeLocationServices() {
    combine(locationProvider.isLocationEnabled(), _userLocationEnabled) { available, wanted ->
        !available && wanted
      }
      .distinctUntilChanged()
      .collect { shouldDisable ->
        if (shouldDisable) {
          writePreferences { userPreferencesRepository.setUserLocationEnabled(false) }
        }
      }
  }

  private suspend fun observeUserLocation() {
    _userLocationEnabled.collectLatest { enabled ->
      if (!enabled) {
        _userLocation.value = null
        _userLocationAddress.value = LocationAddressState.Loaded("")
        updateActiveLocation()
        return@collectLatest
      }
      locationProvider.locationUpdates().collectLatest(::onUserLocationUpdate)
    }
  }

  private suspend fun onUserLocationUpdate(location: GeoLocation) {
    _userLocation.value = location
    updateActiveLocation()

    val previousAddress =
      (_userLocationAddress.value as? LocationAddressState.Loaded)?.address.orEmpty()
    _userLocationAddress.value = LocationAddressState.Loading(previousAddress)

    delay(USER_LOCATION_ADDRESS_DELAY)

    try {
      val address = addressRepository.getAddress(location.latitude, location.longitude).orEmpty()
      _userLocationAddress.value = LocationAddressState.Loaded(address)
    } catch (ex: CancellationException) {
      throw ex
    } catch (ex: Exception) {
      reportError(ex.toRequestError())
      _userLocationAddress.value = LocationAddressState.Loaded(previousAddress)
    }
  }

  private fun activate(category: PlaceCategory) {
    activeCategories += category
    putCategory(category, CategoryPlacesState.Loading)

    fetchJobs.remove(category)?.cancel()
    fetchJobs[category] =
      scope.launch {
        delay(CATEGORY_TOGGLE_DEBOUNCE)
        fetchCategory(category)
      }
  }

  private fun deactivate(category: PlaceCategory) {
    activeCategories -= category
    fetchJobs.remove(category)?.cancel()
    removeCategory(category)
  }

  private suspend fun fetchCategory(category: PlaceCategory) {
    if (!activeCategories.contains(category)) return

    val location = _activeLocation.value
    if (location == null) {
      activeCategories -= category
      removeCategory(category)
      return
    }

    val radiusMeters = _searchRadius.value.meters.toFloat()

    try {
      val fetched =
        withTimeout(REQUEST_TIMEOUT) {
          placesRepository.fetchPlaces(
            category = category,
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMeters = radiusMeters,
          )
        }
      putCategory(
        category = category,
        state = CategoryPlacesState.Loaded(fetched.sortedBy { location.distanceTo(it.location) }),
      )
    } catch (ex: TimeoutCancellationException) {
      failCategory(category, RequestError.IO)
    } catch (ex: CancellationException) {
      throw ex
    } catch (ex: Exception) {
      failCategory(category, ex.toRequestError())
    }
  }

  private fun failCategory(category: PlaceCategory, error: RequestError) {
    activeCategories -= category
    fetchJobs.remove(category)
    removeCategory(category)
    reportError(error)
  }

  private fun refreshActiveCategories() {
    if (_activeLocation.value == null) {
      clearPlaces()
      return
    }
    if (activeCategories.isEmpty()) return

    updatePlaces { activeCategories.associateWith { CategoryPlacesState.Loading } }

    activeCategories.toList().forEach(::refetchCategory)
  }

  private fun refetchCategory(category: PlaceCategory) {
    fetchJobs.remove(category)?.cancel()
    fetchJobs[category] = scope.launch { fetchCategory(category) }
  }

  private fun putCategory(category: PlaceCategory, state: CategoryPlacesState) {
    updatePlaces { it + (category to state) }
  }

  private fun removeCategory(category: PlaceCategory) {
    updatePlaces { it - category }
  }

  private fun clearPlaces() {
    fetchJobs.values.toList().forEach(Job::cancel)
    fetchJobs.clear()
    updatePlaces { emptyMap() }
  }

  private fun updatePlaces(
    transform: (Map<PlaceCategory, CategoryPlacesState>) -> Map<PlaceCategory, CategoryPlacesState>
  ) {
    val places = _places.updateAndGet(transform)
    _allPlaces.value =
      places.values
        .filterIsInstance<CategoryPlacesState.Loaded>()
        .flatMap(CategoryPlacesState.Loaded::places)
  }

  private fun updateCustomLocation(location: GeoLocation?) {
    if (_customLocation.value == location) return
    _customLocation.value = location
    updateActiveLocation()
  }

  private fun updateActiveLocation() {
    val next = if (_userLocationEnabled.value) _userLocation.value else _customLocation.value
    val previous = _activeLocation.value
    if (next == previous) return

    _activeLocation.value = next
    when {
      next == null -> clearPlaces()
      previous == null || !next.hasSameCoordinates(previous) -> refreshActiveCategories()
    }
  }

  private fun searchCustomLocation(query: String) {
    customLocationSearchJob?.cancel()
    val trimmed = query.trim()

    when {
      trimmed.isEmpty() -> _customLocationSearchResults.value = CustomLocationSearchState.Idle
      trimmed.length < MIN_CUSTOM_LOCATION_QUERY_LENGTH ->
        _customLocationSearchResults.value = CustomLocationSearchState.QueryTooShort
      else -> {
        _customLocationSearchResults.value = CustomLocationSearchState.Loading
        customLocationSearchJob =
          scope.launch {
            delay(CUSTOM_LOCATION_SEARCH_DEBOUNCE)
            try {
              val results =
                addressRepository.search(query = trimmed, limit = CUSTOM_LOCATION_SEARCH_LIMIT)
              _customLocationSearchResults.value = CustomLocationSearchState.Loaded(results)
            } catch (ex: CancellationException) {
              throw ex
            } catch (ex: Exception) {
              reportError(ex.toRequestError())
              _customLocationSearchResults.value = CustomLocationSearchState.Loaded(emptyList())
            }
          }
      }
    }
  }

  private fun reportError(error: RequestError) {
    errorClearJob?.cancel()
    _error.value = error
    errorClearJob =
      scope.launch {
        delay(ERROR_AUTO_CLEAR)
        _error.value = null
      }
  }

  private fun persist(block: suspend () -> Unit) {
    scope.launch { writePreferences(block) }
  }

  private suspend fun writePreferences(block: suspend () -> Unit) {
    try {
      block()
    } catch (ex: CancellationException) {
      throw ex
    } catch (ex: Exception) {
      reportError(ex.toRequestError())
    }
  }

  private fun GeoLocation.hasSameCoordinates(other: GeoLocation): Boolean =
    latitude == other.latitude && longitude == other.longitude

  private companion object {
    val CATEGORY_TOGGLE_DEBOUNCE: Duration = 1.seconds
    val REQUEST_TIMEOUT: Duration = 35.seconds
    val ERROR_AUTO_CLEAR: Duration = 4.seconds
    val USER_LOCATION_ADDRESS_DELAY: Duration = 1.seconds
    val CUSTOM_LOCATION_SEARCH_DEBOUNCE: Duration = 1.seconds
    val MAP_POSITION_SAVE_DEBOUNCE: Duration = 1.seconds
    const val MIN_CUSTOM_LOCATION_QUERY_LENGTH = 3
    const val CUSTOM_LOCATION_SEARCH_LIMIT = 100
  }
}

private data class PlaceStateSnapshot(
  val places: Map<PlaceCategory, CategoryPlacesState>,
  val allPlaces: List<Place>,
  val activeLocation: GeoLocation?,
  val userLocation: GeoLocation?,
  val userLocationAddress: LocationAddressState,
)

private data class LocationStateSnapshot(
  val customLocation: GeoLocation?,
  val customLocationAddress: String,
  val customLocationSearchResults: CustomLocationSearchState,
  val userLocationEnabled: Boolean,
  val searchRadius: PlaceSearchRadius,
)
