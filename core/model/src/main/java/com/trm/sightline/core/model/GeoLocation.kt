package com.trm.sightline.core.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoLocation(
  val latitude: Double,
  val longitude: Double,
  val altitude: Double = 0.0,
  val horizontalAccuracyMeters: Double? = null,
  val timestampMillis: Long = 0L,
) {
  fun distanceTo(other: GeoLocation): Float = haversineMeters(other).toFloat()
  fun bearingTo(other: GeoLocation): Float = initialBearingDegrees(other).toFloat()

  private fun haversineMeters(other: GeoLocation): Double {
    val lat1 = latitude * PI / 180.0
    val lat2 = other.latitude * PI / 180.0
    val dLat = (other.latitude - latitude) * PI / 180.0
    val dLon = (other.longitude - longitude) * PI / 180.0

    val sinDLat = sin(dLat / 2)
    val sinDLon = sin(dLon / 2)
    val a = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
    return EARTH_RADIUS_METERS * 2 * asin(sqrt(a))
  }

  private fun initialBearingDegrees(other: GeoLocation): Double {
    val lat1 = latitude * PI / 180.0
    val lat2 = other.latitude * PI / 180.0
    val dLon = (other.longitude - longitude) * PI / 180.0

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
  }

  private companion object {
    const val EARTH_RADIUS_METERS = 6371000.0
  }
}
