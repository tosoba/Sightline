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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: PlacesRepository) : ViewModel() {
  val places = mutableStateMapOf<PlaceCategory, LoadingState<List<Place>>>()
  val networkErrors = Channel<NetworkError>(Channel.UNLIMITED)

  var currentLocation by
    mutableStateOf(
      Location(null).apply {
        latitude = 52.237049
        longitude = 21.017532
      }
    )
    private set

  private val pendingJobs = mutableMapOf<PlaceCategory, Job>()
  private val executingJobs = mutableMapOf<PlaceCategory, Job>()

  private data class PlaceFetchRequest(
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
  )

  private val fetchQueue = Channel<PlaceFetchRequest>(Channel.RENDEZVOUS)

  init {
    viewModelScope.launch {
      for (request in fetchQueue) {
        if (!isActive || !places.containsKey(request.category)) continue

        val fetchJob = launch {
          repository
            .cancellableRunCatching {
              fetchPlaces(
                category = request.category,
                latitude = request.latitude,
                longitude = request.longitude,
                radiusMeters = 1000f,
              )
            }
            .onSuccess { places[request.category] = LoadingState.Loaded(it) }
            .onFailure { networkErrors.send(it.toNetworkError()) }
        }
        executingJobs[request.category] = fetchJob
        fetchJob.join()
        executingJobs.remove(request.category)
      }
    }
  }

  fun onTogglePlaceCategory(category: PlaceCategory) {
    pendingJobs.remove(category)?.cancel()
    executingJobs.remove(category)?.cancel()

    if (places.containsKey(category)) {
      places.remove(category)
    } else {
      places[category] = LoadingState.Loading
      pendingJobs[category] =
        viewModelScope.launch {
          fetchQueue.send(
            PlaceFetchRequest(
              category = category,
              latitude = currentLocation.latitude,
              longitude = currentLocation.longitude,
            )
          )
          pendingJobs.remove(category)
        }
    }
  }
}
