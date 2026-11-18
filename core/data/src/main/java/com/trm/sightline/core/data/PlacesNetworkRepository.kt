package com.trm.sightline.core.data

import com.trm.sightline.api.photon.PhotonApi
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
class PlacesNetworkRepository @Inject constructor(private val photonApi: PhotonApi) :
  PlacesRepository {
  override suspend fun fetchPlaces(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): List<Place> =
    photonApi
      .reverse(
        lat = latitude,
        lon = longitude,
        radius = metresToKilometres(radiusMeters),
        osmTag = category.osmTags,
        limit = LIMIT,
      )
      .features()
      ?.mapNotNull { FeatureMapper.toPlace(it, category) }
      ?.distinctBy(Place::id)
      .orEmpty()
}

internal fun metresToKilometres(radiusMeters: Float): Double = radiusMeters / METRES_PER_KILOMETRE

private const val METRES_PER_KILOMETRE = 1000.0
private const val LIMIT = 100
