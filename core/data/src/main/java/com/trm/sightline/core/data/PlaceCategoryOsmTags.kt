package com.trm.sightline.core.data

import com.trm.sightline.core.model.PlaceCategory

internal val PlaceCategory.osmTags: List<String>
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks ->
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
        )
      PlaceCategory.ArtsCulture ->
        listOf("tourism:museum", "tourism:gallery", "amenity:theatre", "amenity:cinema")
      PlaceCategory.FoodDrink ->
        listOf(
          "amenity:restaurant",
          "amenity:cafe",
          "amenity:bar",
          "amenity:biergarten",
          "amenity:pub",
          "amenity:fast_food",
          "amenity:food_court",
          "amenity:ice_cream",
        )
      PlaceCategory.Nightlife -> listOf("amenity:nightclub", "amenity:casino")
      PlaceCategory.Shopping ->
        listOf(
          "shop:mall",
          "shop:department_store",
          "shop:supermarket",
          "shop:general",
          "shop:kiosk",
          "shop:wholesale",
          "amenity:marketplace",
        )
      PlaceCategory.NatureOutdoors ->
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
        )
      PlaceCategory.Library -> listOf("amenity:library")
      PlaceCategory.Accommodation ->
        listOf(
          "tourism:hotel",
          "tourism:hostel",
          "tourism:guest_house",
          "tourism:motel",
          "tourism:apartment",
          "tourism:chalet",
          "tourism:alpine_hut",
        )
      PlaceCategory.GettingAround ->
        listOf(
          "railway:station",
          "railway:halt",
          "railway:tram_stop",
          "railway:subway_entrance",
          "amenity:bus_station",
          "amenity:taxi",
          "amenity:bicycle_rental",
        )
      PlaceCategory.RoadTrip ->
        listOf(
          "amenity:fuel",
          "amenity:charging_station",
          "amenity:parking",
          "amenity:car_rental",
          "amenity:car_sharing",
        )
      PlaceCategory.MoneyBanking ->
        listOf("amenity:atm", "amenity:bank", "amenity:bureau_de_change")
      PlaceCategory.Health -> listOf("amenity:pharmacy", "amenity:hospital", "amenity:clinic")
      PlaceCategory.Practical -> listOf("amenity:toilets")
    }
