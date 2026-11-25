package com.trm.sightline.shared

import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.Place

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
