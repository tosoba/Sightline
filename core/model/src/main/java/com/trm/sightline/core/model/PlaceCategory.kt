package com.trm.sightline.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory {
  SightsLandmarks,
  ArtsCulture,
  FoodDrink,
  Nightlife,
  Shopping,
  NatureOutdoors,
  Library,
  Accommodation,
  GettingAround,
  RoadTrip,
  MoneyBanking,
  Health,
  Practical;

  val label: String
    get() =
      name.replace("(?<=.)(?=[A-Z])".toRegex(), " ").lowercase().replaceFirstChar { it.uppercase() }
}
