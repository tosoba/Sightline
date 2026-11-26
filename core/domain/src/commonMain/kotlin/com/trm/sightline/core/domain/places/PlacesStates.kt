package com.trm.sightline.core.domain.places

import com.trm.sightline.core.common.RequestError
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.model.PlaceSearchRadius

/**
 * Consolidated snapshot of everything the places sheet needs to render, emitted as one value so
 * hosts can observe a single stream instead of fanning out over every fine-grained flow.
 */
data class PlacesUiState(
  val places: Map<PlaceCategory, CategoryPlacesState> = emptyMap(),
  val allPlaces: List<Place> = emptyList(),
  val activeLocation: GeoLocation? = null,
  val userLocation: GeoLocation? = null,
  val userLocationAddress: LocationAddressState = LocationAddressState.Loaded(""),
  val customLocation: GeoLocation? = null,
  val customLocationAddress: String = "",
  val customLocationSearchResults: CustomLocationSearchState = CustomLocationSearchState.Idle,
  val userLocationEnabled: Boolean = false,
  val searchRadius: PlaceSearchRadius = PlaceSearchRadius.OneKilometer,
  val lastMapPosition: MapCameraPosition? = null,
  val error: RequestError? = null,
)

/**
 * Concrete, non-generic states so SKIE bridges them to exhaustive Swift enums with typed payloads.
 * Objective-C erases the type parameters of generic interfaces such as `LoadingState`.
 */
sealed interface CategoryPlacesState {
  data object Loading : CategoryPlacesState

  data class Loaded(val places: List<Place>) : CategoryPlacesState
}

sealed interface LocationAddressState {
  data class Loading(val previousAddress: String = "") : LocationAddressState

  data class Loaded(val address: String) : LocationAddressState
}

sealed interface CustomLocationSearchState {
  data object Idle : CustomLocationSearchState

  data object QueryTooShort : CustomLocationSearchState

  data object Loading : CustomLocationSearchState

  data class Loaded(val results: List<CustomLocation>) : CustomLocationSearchState
}
