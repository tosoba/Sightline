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
  if (this >= 1_000) "${(this / 1_000).roundToDecimalPlaces(1)} km" else "${this.roundToInt()} m"
