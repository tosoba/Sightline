package com.trm.sightline.core.ar.model

import android.location.Location
import com.trm.sightline.core.model.Marker
import java.util.Objects

class ARMarker(val wrapped: Marker) {
  var x = 0f
  var y = 0f
  var distance = 0f
  var isDrawn = true

  val location: Location
    get() =
      Location(null).apply {
        latitude = wrapped.latitude
        longitude = wrapped.longitude
      }

  override fun equals(other: Any?): Boolean =
    this === other || (other is ARMarker && other.wrapped == wrapped)

  override fun hashCode(): Int = Objects.hash(wrapped)
}
