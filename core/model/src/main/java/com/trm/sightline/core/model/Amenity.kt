package com.trm.sightline.core.model

enum class Amenity {
  // Food/drink
  Bar, // pub|bar|biergarten
  Cafe, // cafe
  Restaurant, // restaurant|fast_food|ice_cream|food_court

  // Transportation
  BikeRental, // bicycle_rental
  BusStation, // bus_station
  CarRental, // car_rental|car_sharing
  CarWash, // car_wash
  ChargingStation, // charging_station
  Fuel, // fuel
  Parking, // parking|motorcycle_parking
  Taxi, // taxi

  // Financial
  ATM, // atm|payment_terminal
  Bank, // bank
  CurrencyExchange, // bureau_de_change

  // Healthcare
  Doctors, // doctors
  Hospital, // hospital|clinic
  Pharmacy, // pharmacy
  Veterinary, // veterinary

  // Entertainment, Arts & Culture
  Casino, // casino
  Cinema, // cinema
  CommunityCentre, // community_centre
  Library, // library
  Nightclub, // nightclub
  Theatre, // theatre

  // Public service/Facilities
  FireStation, // fire_station
  ParcelLocker, // parcel_locker
  Police, // police
  PostBox, // post_box
  PostOffice, // post_office
  Toilets, // toilets
}
