package com.trm.sightline.core.common.util

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun <T : Number> T.roundToDecimalPlaces(places: Int): Double {
  val factor = 10.0.pow(places)
  return (toDouble() * factor).roundToLong() / factor
}

fun Float.formattedDistance(): String =
  if (this >= 1_000) {
    val km = (this / 1_000).roundToDecimalPlaces(1)
    if (km == floor(km)) "${km.toInt()} km" else "$km km"
  } else {
    "${this.roundToInt()} m"
  }
