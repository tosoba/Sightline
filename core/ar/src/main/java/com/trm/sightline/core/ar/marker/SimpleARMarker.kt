package com.trm.sightline.core.ar.marker

import com.trm.sightline.core.ar.model.Marker
import java.util.Objects

class SimpleARMarker(override val wrapped: Marker) : ARMarker {
  override var x = 0f
  override var y = 0f
  override var distance = 0f
  override var isDrawn = true

  override fun equals(other: Any?): Boolean =
    this === other || (other is SimpleARMarker && other.wrapped == wrapped)

  override fun hashCode(): Int = Objects.hash(wrapped)
}
