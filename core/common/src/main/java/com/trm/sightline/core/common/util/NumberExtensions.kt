package com.trm.sightline.core.common.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun <T : Number> T.roundToDecimalPlaces(places: Int): BigDecimal {
  var rounded = BigDecimal(toString())
  rounded = rounded.setScale(places, RoundingMode.HALF_UP)
  return rounded
}

fun Float.formattedDistance(): String =
  if (this >= 1_000) {
    val km = (this / 1_000).roundToDecimalPlaces(1)
    if (km.stripTrailingZeros().scale() <= 0) "${km.toInt()} km" else "$km km"
  } else {
    "${this.roundToInt()} m"
  }
