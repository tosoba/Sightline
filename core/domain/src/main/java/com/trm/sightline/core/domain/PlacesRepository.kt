package com.trm.sightline.core.domain

import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

interface PlacesRepository {
  suspend fun fetchPlaces(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): List<Place>
}
