package com.trm.sightline.core.ar.marker

import com.trm.sightline.core.ar.model.Marker

interface ARMarker {
  val wrapped: Marker
  var distance: Float
  var x: Float
  var y: Float
  var isDrawn: Boolean
}
