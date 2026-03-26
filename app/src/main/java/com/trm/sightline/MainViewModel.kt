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
import com.trm.sightline.core.common.toRequestError
import com.trm.sightline.core.common.util.locationEnabledFlow
import com.trm.sightline.core.common.util.locationUpdatesFlow
import com.trm.sightline.core.common.util.withLatestFrom
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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

  val requestErrors = Channel<RequestError>(Channel.UNLIMITED)

  val userLocationEnabled: StateFlow<Boolean> =
    userPreferencesRepository
      .getUserLocationEnabled()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
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
      .withLatestFrom(userPreferencesRepository.getCustomLocation().map { it?.address }) {
        query,
        savedAddress ->
        query to savedAddress
      }
      .filter { (query, savedAddress) -> query != savedAddress }
      .map { (query, _) -> query }
      .debounce(1.seconds)
      .transformLatest { query ->
        if (query.trim().length < 3) {
          emit(LoadingState.Loaded(emptyList()))
        } else {
          emit(LoadingState.Loading)

          try {
            emit(LoadingState.Loaded(addressRepository.search(query = query, limit = 100)))
          } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            requestErrors.send(ex.toRequestError())
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

  init {
    handleCustomLocation()
    handleUserLocation()
    handlePlaces()
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
    application
      .locationEnabledFlow()
      .onEach { isLocationEnabled ->
        if (!isLocationEnabled && userLocationEnabled.value) {
          userPreferencesRepository.setUserLocationEnabled(false)
        }
      }
      .launchIn(viewModelScope)

    userLocationEnabled
      .flatMapLatest { enabled -> if (enabled) application.locationUpdatesFlow() else emptyFlow() }
      .transformLatest { location ->
        userLocation = location
        _userLocationAddress.value = LoadingState.Loading

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
          requestErrors.send(ex.toRequestError())
          _userLocationAddress.value = LoadingState.Loaded("")
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
          if (remaining > 0) delay(remaining)
          lastSentTime.set(System.currentTimeMillis())

          val location =
            (if (userLocationEnabled.value) userLocation else customLocation)
              ?: return@transformLatest
          try {
            places[category] =
              withTimeout(REQUEST_TIMEOUT_MS) {
                LoadingState.Loaded(
                  placesRepository.fetchPlaces(
                    category = category,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = 1_000f,
                  )
                )
              }
          } catch (ex: Exception) {
            places.remove(category)
            requestErrors.send(ex.toRequestError())
            if (ex is CancellationException) throw ex
          }
        }
        .launchIn(viewModelScope)
    }
  }

  fun onTogglePlaceCategory(category: PlaceCategory) {
    val isActive = !places.containsKey(category)
    if (isActive) places[category] = LoadingState.Loading else places.remove(category)
    viewModelScope.launch { placeCategoryToggles.emit(category to isActive) }
  }

  fun setCustomLocationAddress(address: String) {
    _customLocationAddress.value = address
  }

  fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUserLocationEnabled(userLocationEnabled) }
  }

  fun onCustomLocationSearchResultClick(customLocation: CustomLocation) {
    viewModelScope.launch { userPreferencesRepository.setCustomLocation(customLocation) }
  }

  companion object {
    private const val DEBOUNCE_MS = 1_000L
    private const val REQUEST_TIMEOUT_MS = 15_000L
  }
}
