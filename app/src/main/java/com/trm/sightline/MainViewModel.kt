package com.trm.sightline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trm.sightline.core.common.location.LocationProvider
import com.trm.sightline.core.domain.AddressRepository
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.domain.UserPreferencesRepository
import com.trm.sightline.core.domain.places.PlacesCoordinator
import com.trm.sightline.core.domain.places.PlacesUiState
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class MainViewModel(
  locationProvider: LocationProvider,
  placesRepository: PlacesRepository,
  addressRepository: AddressRepository,
  userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
  private val coordinator =
    PlacesCoordinator(
      placesRepository = placesRepository,
      addressRepository = addressRepository,
      userPreferencesRepository = userPreferencesRepository,
      locationProvider = locationProvider,
      scope = viewModelScope,
    )

  val state: StateFlow<PlacesUiState> = coordinator.state

  fun onTogglePlaceCategory(category: PlaceCategory) {
    coordinator.onTogglePlaceCategory(category)
  }

  fun setUserLocationEnabled(enabled: Boolean) {
    coordinator.setUserLocationEnabled(enabled)
  }

  fun setCustomLocationAddress(address: String) {
    coordinator.setCustomLocationAddress(address)
  }

  fun onCustomLocationSearchResultClick(customLocation: CustomLocation) {
    coordinator.onCustomLocationSearchResultClick(customLocation)
  }

  fun cancelCustomLocationSearch() {
    coordinator.cancelCustomLocationSearch()
  }

  fun saveMapPosition(position: MapCameraPosition) {
    coordinator.saveMapPosition(position)
  }

  fun setSearchRadius(radius: PlaceSearchRadius) {
    coordinator.setSearchRadius(radius)
  }
}
