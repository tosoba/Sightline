package com.trm.sightline

import android.location.Location

data class PovLocation(val location: Location, val origin: PovLocationOrigin)

enum class PovLocationOrigin {
  GPS,
  CUSTOM,
}
