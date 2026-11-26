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
  Practical,
}
