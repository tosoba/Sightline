package com.trm.sightline

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trm.sightline.core.common.RequestError
import com.trm.sightline.core.common.messageResource
import com.trm.sightline.core.common.toRequestError
import com.trm.sightline.core.common.util.location
import com.trm.sightline.core.common.util.locationEnabledFlow
import com.trm.sightline.core.common.util.locationUpdatesFlow
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MainViewModel
@Inject
constructor(
  private val application: Application,
  private val placesRepository: PlacesRepository,
  private val addressRepository: AddressRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
) : AndroidViewModel(application) {
  val places = mutableStateMapOf<PlaceCategory, LoadingState<List<Place>>>()

  val allPlaces: List<Place>
    get() =
      places.values
        .filterIsInstance<LoadingState.Loaded<List<Place>>>()
        .flatMap(LoadingState.Loaded<List<Place>>::data)

  private val _errorMessage = MutableStateFlow<Int?>(null)
  val errorMessage = _errorMessage.asStateFlow()

  private var clearErrorMessageJob: Job? = null

  val userLocationEnabled: StateFlow<Boolean> =
    userPreferencesRepository
      .getUserLocationEnabled()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
      )
  val lastMapPosition: StateFlow<MapCameraPosition?> =
    userPreferencesRepository
      .getLastMapPosition()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
      )

  val searchRadius: StateFlow<PlaceSearchRadius> =
    userPreferencesRepository
      .getSearchRadius()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaceSearchRadius.OneKilometer,
      )

  var userLocation: Location? by mutableStateOf(null)
    private set

  private val _userLocationAddress = MutableStateFlow<LoadingState<String>>(LoadingState.Loaded(""))
  val userLocationAddress: StateFlow<LoadingState<String>> = _userLocationAddress.asStateFlow()

  var customLocation: Location? by mutableStateOf(null)
    private set

  private val _customLocationAddress = MutableStateFlow("")
  val customLocationAddress: StateFlow<String> = _customLocationAddress.asStateFlow()
  val customLocationSearchResults: StateFlow<LoadingState<List<CustomLocation>>> =
    customLocationAddress
      .debounce(1.seconds)
      .distinctUntilChanged()
      .transformLatest { query ->
        if (query.trim().length < 3) {
          emit(LoadingState.Loaded(emptyList()))
        } else {
          emit(LoadingState.Loading())

          try {
            emit(LoadingState.Loaded(addressRepository.search(query = query, limit = 100)))
          } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            Timber.e(ex)
            handleRequestError(ex.toRequestError())
            emit(LoadingState.Loaded(emptyList()))
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = LoadingState.Loaded(emptyList()),
      )

  private val placeCategoryToggles = MutableSharedFlow<Pair<PlaceCategory, Boolean>>()

  private val mapPositionUpdates =
    MutableSharedFlow<MapCameraPosition>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  init {
    handleCustomLocation()
    handleUserLocation()
    handlePlaces()
    handleMapPositionUpdates()
  }

  private fun handleCustomLocation() {
    userPreferencesRepository
      .getCustomLocation()
      .filterNotNull()
      .onEach {
        customLocation =
          Location("").apply {
            latitude = it.latitude
            longitude = it.longitude
          }
        _customLocationAddress.value = it.address
      }
      .launchIn(viewModelScope)
  }

  private fun handleUserLocation() {
    combine(application.locationEnabledFlow(), userLocationEnabled) {
        isLocationEnabled,
        userLocationEnabled ->
        !isLocationEnabled && userLocationEnabled
      }
      .filter { it }
      .onEach { userPreferencesRepository.setUserLocationEnabled(false) }
      .launchIn(viewModelScope)

    userLocationEnabled
      .flatMapLatest { enabled -> if (enabled) application.locationUpdatesFlow() else emptyFlow() }
      .transformLatest { location ->
        userLocation = location
        _userLocationAddress.update { LoadingState.Loading(it.data) }

        delay(1.seconds)

        try {
          _userLocationAddress.value =
            LoadingState.Loaded(
              addressRepository
                .getAddress(latitude = location.latitude, longitude = location.longitude)
                .orEmpty()
            )
        } catch (ex: Exception) {
          if (ex is CancellationException) throw ex
          Timber.e(ex)
          handleRequestError(ex.toRequestError())
          _userLocationAddress.update { LoadingState.Loaded(it.data.orEmpty()) }
        }

        emit(Unit)
      }
      .launchIn(viewModelScope)
  }

  private fun handlePlaces() {
    val lastSentTime = AtomicLong(0L)
    PlaceCategory.entries.forEach { category ->
      placeCategoryToggles
        .filter { (cat, _) -> cat == category }
        .transformLatest<Pair<PlaceCategory, Boolean>, Unit> { (_, isActive) ->
          if (!isActive) {
            lastSentTime.set(0L)
            return@transformLatest
          }

          val remaining = DEBOUNCE_MS - (System.currentTimeMillis() - lastSentTime.get())
          if (remaining > 0) delay(remaining.milliseconds)
          lastSentTime.set(System.currentTimeMillis())

          val location =
            (if (userLocationEnabled.value) userLocation else customLocation)
              ?: return@transformLatest
          try {
            places[category] =
              withTimeout(REQUEST_TIMEOUT_MS.milliseconds) {
                LoadingState.Loaded(
                  placesRepository
                    .fetchPlaces(
                      category = category,
                      latitude = location.latitude,
                      longitude = location.longitude,
                      radiusMeters = searchRadius.value.meters.toFloat(),
                    )
                    .sortedBy { location.distanceTo(it.location) }
                )
              }
          } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            Timber.e(ex)
            places.remove(category)
            handleRequestError(ex.toRequestError())
          }
        }
        .launchIn(viewModelScope)
    }
  }

  private fun handleMapPositionUpdates() {
    mapPositionUpdates
      .debounce(1.seconds)
      .distinctUntilChanged()
      .onEach(userPreferencesRepository::setLastMapPosition)
      .launchIn(viewModelScope)
  }

  private fun handleRequestError(error: RequestError) {
    clearErrorMessageJob?.cancel()
    _errorMessage.value = error.messageResource()
    clearErrorMessageJob = viewModelScope.launch {
      delay(4000.milliseconds)
      _errorMessage.value = null
    }
  }

  fun onTogglePlaceCategory(category: PlaceCategory) {
    val isActive = !places.containsKey(category)
    if (isActive) places[category] = LoadingState.Loading() else places.remove(category)
    viewModelScope.launch { placeCategoryToggles.emit(category to isActive) }
  }

  fun setCustomLocationAddress(address: String) {
    _customLocationAddress.value = address
    customLocation = null
  }

  fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUserLocationEnabled(userLocationEnabled) }
  }

  fun onCustomLocationSearchResultClick(customLocation: CustomLocation) {
    viewModelScope.launch { userPreferencesRepository.setCustomLocation(customLocation) }
  }

  fun saveMapPosition(position: MapCameraPosition) {
    viewModelScope.launch { mapPositionUpdates.emit(position) }
  }

  fun setSearchRadius(radius: PlaceSearchRadius) {
    viewModelScope.launch { userPreferencesRepository.setSearchRadius(radius) }
  }

  companion object {
    private const val DEBOUNCE_MS = 1_000L
    private const val REQUEST_TIMEOUT_MS = 15_000L
  }
}
