package com.trm.sightline

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trm.sightline.core.data.PlacesNetworkRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
  private val repository: PlacesRepository = PlacesNetworkRepository()

  val places = mutableStateMapOf<PlaceCategory, List<Place>>()
  private val fetchJobs = mutableMapOf<PlaceCategory, Job>()

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
      fetchJobs[category] =
        viewModelScope.launch {
          places[category] =
            repository.fetchPlaces(
              category = category,
              latitude = currentLocation.latitude,
              longitude = currentLocation.longitude,
              radiusMeters = 1000f,
            )
        }
    }
  }
}
