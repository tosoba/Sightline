package com.trm.sightline.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wc
import androidx.compose.ui.graphics.vector.ImageVector
import com.trm.sightline.core.model.PlaceCategory

val PlaceCategory.icon: ImageVector
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks -> Icons.Default.Place
      PlaceCategory.ArtsCulture -> Icons.Default.Museum
      PlaceCategory.FoodDrink -> Icons.Default.Restaurant
      PlaceCategory.Nightlife -> Icons.Default.Nightlife
      PlaceCategory.Shopping -> Icons.Default.Storefront
      PlaceCategory.NatureOutdoors -> Icons.Default.Park
      PlaceCategory.Library -> Icons.Default.LocalLibrary
      PlaceCategory.Accommodation -> Icons.Default.Hotel
      PlaceCategory.GettingAround -> Icons.Default.DirectionsBus
      PlaceCategory.RoadTrip -> Icons.Default.DirectionsCar
      PlaceCategory.MoneyBanking -> Icons.Default.AccountBalance
      PlaceCategory.Health -> Icons.Default.LocalHospital
      PlaceCategory.Practical -> Icons.Default.Wc
    }

@get:StringRes
val PlaceCategory.labelRes: Int
  get() =
    when (this) {
      PlaceCategory.SightsLandmarks -> R.string.place_category_sights_landmarks
      PlaceCategory.ArtsCulture -> R.string.place_category_arts_culture
      PlaceCategory.FoodDrink -> R.string.place_category_food_drink
      PlaceCategory.Nightlife -> R.string.place_category_nightlife
      PlaceCategory.Shopping -> R.string.place_category_shopping
      PlaceCategory.NatureOutdoors -> R.string.place_category_nature_outdoors
      PlaceCategory.Library -> R.string.place_category_library
      PlaceCategory.Accommodation -> R.string.place_category_accommodation
      PlaceCategory.GettingAround -> R.string.place_category_getting_around
      PlaceCategory.RoadTrip -> R.string.place_category_road_trip
      PlaceCategory.MoneyBanking -> R.string.place_category_money_banking
      PlaceCategory.Health -> R.string.place_category_health
      PlaceCategory.Practical -> R.string.place_category_practical
    }
