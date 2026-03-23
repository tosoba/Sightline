package com.trm.sightline

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trm.sightline.core.common.NetworkError
import com.trm.sightline.core.common.toNetworkError
import com.trm.sightline.core.common.util.locationEnabledFlow
import com.trm.sightline.core.common.util.locationUpdatesFlow
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel
@Inject
constructor(
  application: Application,
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

  val networkErrors = Channel<NetworkError>(Channel.UNLIMITED)

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

  private val categoryToggles = MutableSharedFlow<Pair<PlaceCategory, Boolean>>()

  init {
    viewModelScope.launch {
      _customLocationAddress.value = userPreferencesRepository.getCustomLocationAddress().orEmpty()
      customLocationAddress.collectLatest(userPreferencesRepository::setCustomLocationAddress)
    }

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
          _userLocationAddress.value = LoadingState.Loaded("")
        }
        emit(Unit)
      }
      .launchIn(viewModelScope)

    PlaceCategory.entries.forEach { category ->
      var lastSentTime = 0L

      categoryToggles
        .filter { (cat, _) -> cat == category }
        .transformLatest<Pair<PlaceCategory, Boolean>, Unit> { (_, isActive) ->
          if (!isActive) {
            lastSentTime = 0L
            return@transformLatest
          }

          val remaining = DEBOUNCE_MS - (System.currentTimeMillis() - lastSentTime)
          if (remaining > 0) delay(remaining)
          lastSentTime = System.currentTimeMillis()

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
            networkErrors.send(ex.toNetworkError())
            if (ex is CancellationException) throw ex
          }
        }
        .launchIn(viewModelScope)
    }
  }

  fun onTogglePlaceCategory(category: PlaceCategory) {
    val isActive = !places.containsKey(category)
    if (isActive) places[category] = LoadingState.Loading else places.remove(category)
    viewModelScope.launch { categoryToggles.emit(category to isActive) }
  }

  fun setCustomLocationAddress(address: String) {
    _customLocationAddress.value = address
  }

  fun setUserLocationEnabled(userLocationEnabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUserLocationEnabled(userLocationEnabled) }
  }

  companion object {
    private const val DEBOUNCE_MS = 1_000L
    private const val REQUEST_TIMEOUT_MS = 15_000L
  }
}
