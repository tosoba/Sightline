package com.trm.sightline.core.data

import com.trm.sightline.core.model.PlaceCategory
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class PlaceCategoryOsmTagsTest {
  @Test
  fun `every category maps to a non-empty tag list`() {
    PlaceCategory.entries.forEach { category ->
      assertTrue(category.osmTags.isNotEmpty(), "${category.name} must map to at least one OSM tag")
    }
  }

  @Test
  fun `tag lists match the agreed mapping exactly`() {
    val expected =
      mapOf(
        PlaceCategory.SightsLandmarks to
          listOf(
            "tourism:attraction",
            "tourism:viewpoint",
            "tourism:artwork",
            "tourism:aquarium",
            "tourism:theme_park",
            "tourism:zoo",
            "historic:monument",
            "historic:memorial",
            "historic:castle",
            "historic:fort",
            "historic:ruins",
            "historic:archaeological_site",
            "historic:city_gate",
            "historic:tower",
          ),
        PlaceCategory.ArtsCulture to
          listOf("tourism:museum", "tourism:gallery", "amenity:theatre", "amenity:cinema"),
        PlaceCategory.FoodDrink to
          listOf(
            "amenity:restaurant",
            "amenity:cafe",
            "amenity:bar",
            "amenity:biergarten",
            "amenity:pub",
            "amenity:fast_food",
            "amenity:food_court",
            "amenity:ice_cream",
          ),
        PlaceCategory.Nightlife to listOf("amenity:nightclub", "amenity:casino"),
        PlaceCategory.Shopping to
          listOf(
            "shop:mall",
            "shop:department_store",
            "shop:supermarket",
            "shop:general",
            "shop:kiosk",
            "shop:wholesale",
            "amenity:marketplace",
          ),
        PlaceCategory.NatureOutdoors to
          listOf(
            "leisure:park",
            "leisure:garden",
            "leisure:nature_reserve",
            "leisure:playground",
            "leisure:pitch",
            "leisure:sports_centre",
            "leisure:swimming_pool",
            "leisure:golf_course",
            "leisure:marina",
            "natural:beach",
            "natural:peak",
            "natural:volcano",
            "natural:cliff",
            "natural:valley",
            "natural:water",
            "tourism:alpine_hut",
            "tourism:wilderness_hut",
          ),
        PlaceCategory.Library to listOf("amenity:library"),
        PlaceCategory.Accommodation to
          listOf(
            "tourism:hotel",
            "tourism:hostel",
            "tourism:guest_house",
            "tourism:motel",
            "tourism:apartment",
            "tourism:chalet",
            "tourism:alpine_hut",
          ),
        PlaceCategory.GettingAround to
          listOf(
            "railway:station",
            "railway:halt",
            "railway:tram_stop",
            "railway:subway_entrance",
            "amenity:bus_station",
            "amenity:taxi",
            "amenity:bicycle_rental",
          ),
        PlaceCategory.RoadTrip to
          listOf(
            "amenity:fuel",
            "amenity:charging_station",
            "amenity:parking",
            "amenity:car_rental",
            "amenity:car_sharing",
          ),
        PlaceCategory.MoneyBanking to
          listOf("amenity:atm", "amenity:bank", "amenity:bureau_de_change"),
        PlaceCategory.Health to listOf("amenity:pharmacy", "amenity:hospital", "amenity:clinic"),
        PlaceCategory.Practical to listOf("amenity:toilets"),
      )

    assertEquals(PlaceCategory.entries.toSet(), expected.keys)
    expected.forEach { (category, tags) -> assertEquals(tags, category.osmTags, category.name) }
  }
}
