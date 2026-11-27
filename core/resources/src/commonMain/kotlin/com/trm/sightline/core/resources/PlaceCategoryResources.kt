package com.trm.sightline.core.resources

import com.trm.sightline.core.model.PlaceCategory
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource

val PlaceCategory.label: StringResource
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks -> MR.strings.place_category_sights_landmarks
      PlaceCategory.ArtsCulture -> MR.strings.place_category_arts_culture
      PlaceCategory.FoodDrink -> MR.strings.place_category_food_drink
      PlaceCategory.Nightlife -> MR.strings.place_category_nightlife
      PlaceCategory.Shopping -> MR.strings.place_category_shopping
      PlaceCategory.NatureOutdoors -> MR.strings.place_category_nature_outdoors
      PlaceCategory.Library -> MR.strings.place_category_library
      PlaceCategory.Accommodation -> MR.strings.place_category_accommodation
      PlaceCategory.GettingAround -> MR.strings.place_category_getting_around
      PlaceCategory.RoadTrip -> MR.strings.place_category_road_trip
      PlaceCategory.MoneyBanking -> MR.strings.place_category_money_banking
      PlaceCategory.Health -> MR.strings.place_category_health
      PlaceCategory.Practical -> MR.strings.place_category_practical
    }

val PlaceCategory.icon: ImageResource
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks -> MR.images.place_category_sights_landmarks
      PlaceCategory.ArtsCulture -> MR.images.place_category_arts_culture
      PlaceCategory.FoodDrink -> MR.images.place_category_food_drink
      PlaceCategory.Nightlife -> MR.images.place_category_nightlife
      PlaceCategory.Shopping -> MR.images.place_category_shopping
      PlaceCategory.NatureOutdoors -> MR.images.place_category_nature_outdoors
      PlaceCategory.Library -> MR.images.place_category_library
      PlaceCategory.Accommodation -> MR.images.place_category_accommodation
      PlaceCategory.GettingAround -> MR.images.place_category_getting_around
      PlaceCategory.RoadTrip -> MR.images.place_category_road_trip
      PlaceCategory.MoneyBanking -> MR.images.place_category_money_banking
      PlaceCategory.Health -> MR.images.place_category_health
      PlaceCategory.Practical -> MR.images.place_category_practical
    }
