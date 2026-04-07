package com.trm.sightline.core.model

enum class PlaceSearchRadius(val meters: Int) {
  FiveHundred(500),
  OneKilometer(1000),
  TwoKilometers(2000),
  FiveKilometers(5000);

  companion object {
    fun fromMeters(meters: Int): PlaceSearchRadius =
      entries.find { it.meters == meters } ?: OneKilometer
  }
}
