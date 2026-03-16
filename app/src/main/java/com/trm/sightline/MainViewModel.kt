package com.trm.sightline

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trm.sightline.core.common.NetworkError
import com.trm.sightline.core.common.cancellableRunCatching
import com.trm.sightline.core.common.toNetworkError
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: PlacesRepository) : ViewModel() {
  val places = mutableStateMapOf<PlaceCategory, LoadingState<List<Place>>>()
  private val fetchJobs = mutableMapOf<PlaceCategory, Job>()
  val networkErrors = Channel<NetworkError>(Channel.UNLIMITED)

  var currentLocation by
    mutableStateOf(
      Location(null).apply {
        latitude = 52.237049
        longitude = 21.017532
      }
    )
    private set

  fun onTogglePlaceCategory(category: PlaceCategory) {
    fetchJobs.remove(category)?.cancel()

    if (places.containsKey(category)) {
      places.remove(category)
    } else {
      places[category] = LoadingState.Loading
      fetchJobs[category] =
        viewModelScope.launch {
          repository
            .cancellableRunCatching {
              fetchPlaces(
                category = category,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                radiusMeters = 1000f,
              )
            }
            .onSuccess { places[category] = LoadingState.Loaded(it) }
            .onFailure { networkErrors.send(it.toNetworkError()) }
        }
    }
  }
}
