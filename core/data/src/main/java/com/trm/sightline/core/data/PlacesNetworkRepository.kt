package com.trm.sightline.core.data

import com.trm.sightline.api.overpass.OverpassApi
import com.trm.sightline.api.overpass.models.query.settings.Filter
import com.trm.sightline.api.overpass.models.query.statements.ComplexQuery
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery
import com.trm.sightline.api.overpass.models.response.geometries.Node
import com.trm.sightline.core.domain.PlacesRepository
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import javax.inject.Inject

class PlacesNetworkRepository @Inject constructor(private val overpassApi: OverpassApi) :
  PlacesRepository {
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
          category = category,
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
      PlaceCategory.Attractions -> {
        ComplexQuery.Builder()
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
      PlaceCategory.Food -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "bar|biergarten|cafe|fast_food|food_court|ice_cream|pub|restaurant",
          filter = Filter.LIKE,
        )
      }
      PlaceCategory.Accommodation -> {
        NodeQuery.Builder()
          .around(latitude, longitude, radiusMeters)
          .tag("tourism", "alpine_hut|apartment|chalet|guest_house|hostel|hotel|motel", Filter.LIKE)
          .build()
      }
      PlaceCategory.Stores -> {
        NodeQuery.Builder()
          .around(latitude, longitude, radiusMeters)
          .tag("shop", "department_store|general|kiosk|mall|supermarket|wholesale", Filter.LIKE)
          .build()
      }
      PlaceCategory.BikeRental -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "bicycle_rental",
        )
      }
      PlaceCategory.BusStation -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "bus_station",
        )
      }
      PlaceCategory.CarRental -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "car_rental|car_sharing",
          filter = Filter.LIKE,
        )
      }
      PlaceCategory.CarWash -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "car_wash",
        )
      }
      PlaceCategory.ChargingStation -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "charging_station",
        )
      }
      PlaceCategory.Fuel -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "fuel",
        )
      }
      PlaceCategory.Parking -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "parking|motorcycle_parking",
          filter = Filter.LIKE,
        )
      }
      PlaceCategory.Taxi -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "taxi",
        )
      }
      PlaceCategory.Atm -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "atm|payment_terminal",
          filter = Filter.LIKE,
        )
      }
      PlaceCategory.Bank -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "bank",
        )
      }
      PlaceCategory.CurrencyExchange -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "bureau_de_change",
        )
      }
      PlaceCategory.Doctors -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "doctors",
        )
      }
      PlaceCategory.Hospital -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "hospital|clinic",
          filter = Filter.LIKE,
        )
      }
      PlaceCategory.Pharmacy -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "pharmacy",
        )
      }
      PlaceCategory.Veterinary -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "veterinary",
        )
      }
      PlaceCategory.Casino -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "casino",
        )
      }
      PlaceCategory.Cinema -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "cinema",
        )
      }
      PlaceCategory.CommunityCentre -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "community_centre",
        )
      }
      PlaceCategory.Library -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "library",
        )
      }
      PlaceCategory.Nightclub -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "nightclub",
        )
      }
      PlaceCategory.Theatre -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "theatre",
        )
      }
      PlaceCategory.FireStation -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "fire_station",
        )
      }
      PlaceCategory.ParcelLocker -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "parcel_locker",
        )
      }
      PlaceCategory.Police -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "police",
        )
      }
      PlaceCategory.PostBox -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "post_box",
        )
      }
      PlaceCategory.PostOffice -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "post_office",
        )
      }
      PlaceCategory.Toilets -> {
        amenityNodeQuery(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          value = "toilets",
        )
      }
    }.toQuery()

  private fun amenityNodeQuery(
    latitude: Double,
    longitude: Double,
    radiusMeters: Float,
    value: String,
    filter: Filter? = null,
  ): NodeQuery =
    NodeQuery.Builder()
      .around(latitude, longitude, radiusMeters)
      .tag("amenity", value, filter)
      .build()
}
