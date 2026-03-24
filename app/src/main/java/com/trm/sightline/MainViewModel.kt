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
import com.trm.sightline.core.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong
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

  private val _autocompleteResults = MutableStateFlow<List<SearchResult>>(emptyList())
  val autocompleteResults: StateFlow<List<SearchResult>> = _autocompleteResults.asStateFlow()

  private val categoryToggles = MutableSharedFlow<Pair<PlaceCategory, Boolean>>()

  init {
    viewModelScope.launch {
      val initialLocation = userPreferencesRepository.getCustomLocation()
      _customLocationAddress.value = initialLocation?.address.orEmpty()
      customLocation =
        initialLocation?.let {
          Location("").apply {
            latitude = it.latitude
            longitude = it.longitude
          }
        }
    }

    customLocationAddress
      .onEach { if (it.length < 3) _autocompleteResults.value = emptyList() }
      .filter { it.length >= 3 }
      .transformLatest { query ->
        delay(1.seconds)

        try {
          _autocompleteResults.value = addressRepository.search(query = query, limit = 10)
        } catch (ex: Exception) {
          if (ex is CancellationException) throw ex
          _autocompleteResults.value = emptyList()
        }

        emit(Unit)
      }
      .launchIn(viewModelScope)

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

    val lastSentTime = AtomicLong(0L)

    PlaceCategory.entries.forEach { category ->
      categoryToggles
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

  fun onSearchResultClick(searchResult: SearchResult) {
    _customLocationAddress.value = searchResult.address
    _autocompleteResults.value = emptyList()
    customLocation =
      Location("").apply {
        latitude = searchResult.latitude
        longitude = searchResult.longitude
      }
    viewModelScope.launch { userPreferencesRepository.setCustomLocation(searchResult) }
  }

  companion object {
    private const val DEBOUNCE_MS = 1_000L
    private const val REQUEST_TIMEOUT_MS = 15_000L
  }
}
