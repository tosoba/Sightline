package com.trm.sightline.core.data

import com.trm.sightline.api.overpass.OverpassApi
import com.trm.sightline.api.overpass.models.query.settings.Filter
import com.trm.sightline.api.overpass.models.query.statements.ComplexQuery
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery
import com.trm.sightline.api.overpass.models.response.geometries.Node
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

class PlacesNetworkRepository : PlacesRepository {
  private val overpassApi = OverpassApi.create()

  override suspend fun fetchPlaces(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): List<Place> =
    overpassApi
      .ask(
        buildQueryFor(
          category = category,
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
        )
      )
      .elements
      ?.filterIsInstance<Node>()
      ?.filter { !it.tags?.get("name").isNullOrBlank() }
      ?.map { node ->
        Place(
          id = node.id,
          name = requireNotNull(node.tags["name"]),
          latitude = node.lat,
          longitude = node.lon,
          tags = node.tags.orEmpty(),
        )
      }
      .orEmpty()

  private fun buildQueryFor(
    category: PlaceCategory,
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
  ): String =
    when (category) {
      PlaceCategory.ATTRACTIONS -> {
        ComplexQuery.Builder()
          .timeout(DEFAULT_TIMEOUT_SECONDS)
          .union(
            listOf(
              NodeQuery.Builder()
                .around(latitude, longitude, radiusMeters)
                .tag(
                  "tourism",
                  "artwork|attraction|aquarium|gallery|museum|theme_park|viewpoint|zoo",
                  Filter.LIKE,
                )
                .build(),
              NodeQuery.Builder()
                .around(latitude, longitude, radiusMeters)
                .tag(
                  "leisure",
                  "adult_gaming_centre|amusement_arcade|beach_resort|bowling_alley|dance|disc_golf_course|dog_park|escape_game|fishing|fitness_centre|fitness_station|garden|golf_course|hackerspace|high_ropes_course|horse_riding|ice_rink|marina|miniature_gold|nature_reserve|park|pitch|playground|resort|sauna|sports_centre|sports_hall|stadium|sunbathing|swimming_pool|track|trampoline_park",
                  Filter.LIKE,
                )
                .build(),
            )
          )
          .build()
      }
      PlaceCategory.FOOD -> {
        NodeQuery.Builder()
          .timeout(DEFAULT_TIMEOUT_SECONDS)
          .around(latitude, longitude, radiusMeters)
          .tag(
            "amenity",
            "bar|biergarten|cafe|fast_food|food_court|ice_cream|pub|restaurant",
            Filter.LIKE,
          )
          .build()
      }
      PlaceCategory.ACCOMMODATION -> {
        NodeQuery.Builder()
          .timeout(DEFAULT_TIMEOUT_SECONDS)
          .around(latitude, longitude, radiusMeters)
          .tag("tourism", "alpine_hut|apartment|chalet|guest_house|hostel|hotel|motel", Filter.LIKE)
          .build()
      }
      PlaceCategory.STORES -> {
        NodeQuery.Builder()
          .timeout(DEFAULT_TIMEOUT_SECONDS)
          .around(latitude, longitude, radiusMeters)
          .tag("shop", "department_store|general|kiosk|mall|supermarket|wholesale", Filter.LIKE)
          .build()
      }
    }.toQuery()

  companion object {
    private const val DEFAULT_TIMEOUT_SECONDS = 25
  }
}
