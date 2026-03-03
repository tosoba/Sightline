package com.trm.sightline.core.ar.model

interface ARMarker {
  val wrapped: Marker
  var distance: Float
  var x: Float
  var y: Float
  var isDrawn: Boolean
}
