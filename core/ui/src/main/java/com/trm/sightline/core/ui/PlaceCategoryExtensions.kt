package com.trm.sightline.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Wc
import androidx.compose.ui.graphics.vector.ImageVector
import com.trm.sightline.core.model.PlaceCategory

val PlaceCategory.icon: ImageVector
  get() =
    when (this) {
      PlaceCategory.Attractions -> Icons.Default.Place
      PlaceCategory.Food -> Icons.Default.Restaurant
      PlaceCategory.Accommodation -> Icons.Default.Hotel
      PlaceCategory.Stores -> Icons.Default.Storefront
      PlaceCategory.BikeRental -> Icons.AutoMirrored.Filled.DirectionsBike
      PlaceCategory.BusStation -> Icons.Default.DirectionsBus
      PlaceCategory.CarRental -> Icons.Default.DirectionsCar
      PlaceCategory.CarWash -> Icons.Default.LocalCarWash
      PlaceCategory.ChargingStation -> Icons.Default.EvStation
      PlaceCategory.Fuel -> Icons.Default.LocalGasStation
      PlaceCategory.Parking -> Icons.Default.LocalParking
      PlaceCategory.Taxi -> Icons.Default.LocalTaxi
      PlaceCategory.Atm -> Icons.Default.Atm
      PlaceCategory.Bank -> Icons.Default.AccountBalance
      PlaceCategory.CurrencyExchange -> Icons.Default.CurrencyExchange
      PlaceCategory.Doctors -> Icons.Default.MedicalServices
      PlaceCategory.Hospital -> Icons.Default.LocalHospital
      PlaceCategory.Pharmacy -> Icons.Default.LocalPharmacy
      PlaceCategory.Veterinary -> Icons.Default.Pets
      PlaceCategory.Casino -> Icons.Default.Casino
      PlaceCategory.Cinema -> Icons.Default.Movie
      PlaceCategory.CommunityCentre -> Icons.Default.Groups
      PlaceCategory.Library -> Icons.Default.LocalLibrary
      PlaceCategory.Nightclub -> Icons.Default.Nightlife
      PlaceCategory.Theatre -> Icons.Default.TheaterComedy
      PlaceCategory.FireStation -> Icons.Default.LocalFireDepartment
      PlaceCategory.ParcelLocker -> Icons.Default.Inventory
      PlaceCategory.Police -> Icons.Default.LocalPolice
      PlaceCategory.PostBox -> Icons.Default.MarkunreadMailbox
      PlaceCategory.PostOffice -> Icons.Default.LocalPostOffice
      PlaceCategory.Toilets -> Icons.Default.Wc
    }
