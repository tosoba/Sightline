package com.trm.sightline.core.ui

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
