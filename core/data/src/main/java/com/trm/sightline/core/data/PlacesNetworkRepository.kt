package com.trm.sightline.core.data

import com.trm.sightline.api.overpass.OverpassApi
import com.trm.sightline.api.overpass.models.query.settings.Filter
import com.trm.sightline.api.overpass.models.query.statements.ComplexQuery
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery
import com.trm.sightline.api.overpass.models.response.OverpassResponse
import com.trm.sightline.api.overpass.models.response.geometries.Node
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.Marker
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlacesNetworkRepository : PlacesRepository {
  private val overpassApi = OverpassApi.create()

  override suspend fun fetchPlaces(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    category: PlaceCategory,
  ): List<Marker> =
    withContext(Dispatchers.IO) {
      val query =
        when (category) {
          PlaceCategory.ATTRACTIONS -> {
            val tourismQuery =
              NodeQuery.Builder()
                .around(latitude, longitude, radiusMeters)
                .tag(
                  "tourism",
                  "artwork|attraction|aquarium|gallery|museum|theme_park|viewpoint|zoo",
                  Filter.LIKE,
                )
                .build()
            val leisureQuery =
              NodeQuery.Builder()
                .around(latitude, longitude, radiusMeters)
                .tag(
                  "leisure",
                  "adult_gaming_centre|amusement_arcade|beach_resort|bowling_alley|dance|disc_golf_course|dog_park|escape_game|fishing|fitness_centre|fitness_station|garden|golf_course|hackerspace|high_ropes_course|horse_riding|ice_rink|marina|miniature_gold|nature_reserve|park|pitch|playground|resort|sauna|sports_centre|sports_hall|stadium|sunbathing|swimming_pool|track|trampoline_park",
                  Filter.LIKE,
                )
                .build()
            ComplexQuery.Builder()
              .timeout(DEFAULT_TIMEOUT_SECONDS)
              .union(listOf(tourismQuery, leisureQuery))
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
              .tag(
                "tourism",
                "alpine_hut|apartment|chalet|guest_house|hostel|hotel|motel",
                Filter.LIKE,
              )
              .build()
          }
          PlaceCategory.STORES -> {
            NodeQuery.Builder()
              .timeout(DEFAULT_TIMEOUT_SECONDS)
              .around(latitude, longitude, radiusMeters)
              .tag("shop", "department_store|general|kiosk|mall|supermarket|wholesale", Filter.LIKE)
              .build()
          }
        }

      val call = overpassApi.ask(query.toQuery())
      suspendCancellableCoroutine { continuation ->
          call.enqueue(
            object : Callback<OverpassResponse> {
              override fun onResponse(
                call: Call<OverpassResponse>,
                response: Response<OverpassResponse>,
              ) {
                if (response.isSuccessful) continuation.resume(response.body())
                else continuation.resumeWithException(HttpException(response))
              }

              override fun onFailure(call: Call<OverpassResponse>, t: Throwable) {
                continuation.resumeWithException(t)
              }
            }
          )
          continuation.invokeOnCancellation { call.cancel() }
        }
        ?.elements
        ?.filterIsInstance<Node>()
        ?.filter { !it.tags?.get("name").isNullOrBlank() }
        ?.map { node ->
          Marker(
            name = node.tags?.get("name") ?: "Unknown",
            latitude = node.lat,
            longitude = node.lon,
            tags = node.tags.orEmpty(),
          )
        }
        .orEmpty()
    }

  companion object {
    private const val DEFAULT_TIMEOUT_SECONDS = 25
  }
}
