package com.trm.sightline.core.ar.model

import android.location.Location
import com.trm.sightline.core.model.Place
import java.util.Objects

class ARMarker(val place: Place) {
  var x = 0f
  var y = 0f
  var distance = 0f
  var isDrawn = true

  val location: Location
    get() =
      Location(null).apply {
        latitude = place.latitude
        longitude = place.longitude
      }

  override fun equals(other: Any?): Boolean =
    this === other || (other is ARMarker && other.place == place)

  override fun hashCode(): Int = Objects.hash(place)
}
