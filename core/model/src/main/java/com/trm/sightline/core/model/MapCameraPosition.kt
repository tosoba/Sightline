package com.trm.sightline.core.model

data class MapCameraPosition(
  val latitude: Double,
  val longitude: Double,
  val zoom: Double,
  val bearing: Double,
  val tilt: Double,
)
