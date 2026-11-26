package com.trm.sightline.core.common.util

import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun <T : Number> T.roundToDecimalPlaces(places: Int): Double {
  val factor = 10.0.pow(places)
  return (toDouble() * factor).roundToLong() / factor
}

fun formatDistance(meters: Float): String =
  formatDistanceWith(meters, keepWholeKilometerDecimal = false)

/** AR Marker card variant: whole kilometres keep one decimal ("1.0 km"), matching the renderer. */
fun formatMarkerDistance(meters: Float): String =
  formatDistanceWith(meters, keepWholeKilometerDecimal = true)

fun Float.formattedDistance(): String = formatDistance(meters = this)

val PlaceSearchRadius.formattedLabel: String
  get() = formatDistance(meters = meters.toFloat())

private fun formatDistanceWith(meters: Float, keepWholeKilometerDecimal: Boolean): String {
  if (meters < 1_000) return "${meters.roundToInt()} m"

  val kilometers = (meters / 1_000).roundToDecimalPlaces(1)
  return when {
    keepWholeKilometerDecimal -> "$kilometers km"
    kilometers == floor(kilometers) -> "${kilometers.toInt()} km"
    else -> "$kilometers km"
  }
}
