package com.trm.sightline.core.domain

import com.trm.sightline.core.model.Marker
import com.trm.sightline.core.model.PlaceCategory

interface PlacesRepository {
  suspend fun fetchPlaces(
      latitude: Double,
      longitude: Double,
      radiusMeters: Float,
      category: PlaceCategory,
  ): List<Marker>
}
