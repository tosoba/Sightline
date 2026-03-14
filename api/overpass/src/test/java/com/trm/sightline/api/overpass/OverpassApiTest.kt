package com.trm.sightline.api.overpass

import com.trm.sightline.api.overpass.models.query.settings.Filter
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OverpassApiTest {
  private val overpassApi = OverpassApi.create()

  @Test
  fun singleAmenity() = runTest {
    println(
      overpassApi
        .ask(
          NodeQuery.Builder()
            .timeout(25)
            .tag("amenity", "post_box")
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery()
        )
        .toString()
    )
  }

  @Test
  fun allAmenities() = runTest {
    println(
      overpassApi
        .ask(
          NodeQuery.Builder()
            .timeout(25)
            .`is`("amenity")
            .around(52.237049, 21.017532, 1000f)
            .build()
            .toQuery()
        )
        .toString()
    )
  }

  @Test
  fun foodAmenities() = runTest {
    val query =
      NodeQuery.Builder()
        .timeout(25)
        .tag(
          "amenity",
          "bar|biergarten|cafe|fast_food|food_court|ice_cream|pub|restaurant",
          Filter.LIKE,
        )
        .around(52.237049, 21.017532, 1000f)
        .build()
        .toQuery()
    println(query)
    println(overpassApi.ask(query).toString())
  }

  @Test
  fun tourismAccommodation() = runTest {
    val query =
      NodeQuery.Builder()
        .timeout(25)
        .tag("tourism", "alpine_hut|apartment|chalet|guest_house|hostel|hotel|motel", Filter.LIKE)
        .around(52.237049, 21.017532, 1000f)
        .build()
        .toQuery()
    println(query)
    println(overpassApi.ask(query).toString())
  }

  @Test
  fun tourismPlacesToSee() = runTest {
    val query =
      NodeQuery.Builder()
        .timeout(25)
        .tag(
          "tourism",
          "artwork|attraction|aquarium|gallery|museum|theme_park|viewpoint|zoo",
          Filter.LIKE,
        )
        .around(52.237049, 21.017532, 1000f)
        .build()
        .toQuery()
    println(query)
    println(overpassApi.ask(query).toString())
  }

  @Test
  fun leisure() = runTest {
    val query =
      NodeQuery.Builder()
        .timeout(25)
        .tag(
          "leisure",
          "adult_gaming_centre|amusement_arcade|beach_resort|bowling_alley|dance|disc_golf_course|dog_park|escape_game|fishing|fitness_centre|fitness_station|garden|golf_course|hackerspace|high_ropes_course|horse_riding|ice_rink|marina|miniature_gold|nature_reserve|park|pitch|playground|resort|sauna|sports_centre|sports_hall|stadium|sunbathing|swimming_pool|track|trampoline_park",
          Filter.LIKE,
        )
        .around(52.237049, 21.017532, 1000f)
        .build()
        .toQuery()
    println(query)
    println(overpassApi.ask(query).toString())
  }

  @Test
  fun generalStores() = runTest {
    val query =
      NodeQuery.Builder()
        .timeout(25)
        .tag("shop", "department_store|general|kiosk|mall|supermarket|wholesale", Filter.LIKE)
        .around(52.237049, 21.017532, 1000f)
        .build()
        .toQuery()
    println(query)
    println(overpassApi.ask(query).toString())
  }
}
