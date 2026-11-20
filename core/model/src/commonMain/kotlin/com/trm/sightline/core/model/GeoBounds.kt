package com.trm.sightline.core.model

data class GeoBounds(
  val west: Double,
  val south: Double,
  val east: Double,
  val north: Double,
) {
  companion object {
    fun paddedAround(places: List<Place>, percentageIncrease: Double = 0.0): GeoBounds? {
      if (places.isEmpty()) return null

      val minLat = places.minOf(Place::latitude)
      val maxLat = places.maxOf(Place::latitude)
      val minLon = places.minOf(Place::longitude)
      val maxLon = places.maxOf(Place::longitude)

      val latDelta = maxLat - minLat
      val lonDelta = maxLon - minLon
      val paddingFactor = percentageIncrease / 2.0

      return GeoBounds(
        west = minLon - lonDelta * paddingFactor,
        south = minLat - latDelta * paddingFactor,
        east = maxLon + lonDelta * paddingFactor,
        north = maxLat + latDelta * paddingFactor,
      )
    }
  }
}
