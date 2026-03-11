package com.trm.sightline.core.ar.model

import java.util.Objects

class ARMarker(val wrapped: Marker) {
  var x = 0f
  var y = 0f
  var distance = 0f
  var isDrawn = true

  override fun equals(other: Any?): Boolean =
    this === other || (other is ARMarker && other.wrapped == wrapped)

  override fun hashCode(): Int = Objects.hash(wrapped)
}
