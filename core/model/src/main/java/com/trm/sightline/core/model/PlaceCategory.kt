package com.trm.sightline.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory {
  Attractions,
  Food,
  Accommodation,
  Stores,

  // Transportation
  BikeRental,
  BusStation,
  CarRental,
  CarWash,
  ChargingStation,
  Fuel,
  Parking,
  Taxi,

  // Financial
  Atm,
  Bank,
  CurrencyExchange,

  // Healthcare
  Doctors,
  Hospital,
  Pharmacy,
  Veterinary,

  // Entertainment, Arts & Culture
  Casino,
  Cinema,
  CommunityCentre,
  Library,
  Nightclub,
  Theatre,

  // Public service/Facilities
  FireStation,
  ParcelLocker,
  Police,
  PostBox,
  PostOffice,
  Toilets;

  val label: String
    get() =
      name.replace("(?<=.)(?=[A-Z])".toRegex(), " ").lowercase().replaceFirstChar { it.uppercase() }
}
