package com.trm.sightline.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
  val userLocationEnabled: Boolean = false,
  val customLocationLatitude: Double? = null,
  val customLocationLongitude: Double? = null,
  val customLocationAddress: String? = null,
  val customLocationTimestampMillis: Long? = null,
  val lastMapLatitude: Double? = null,
  val lastMapLongitude: Double? = null,
  val lastMapZoom: Double? = null,
  val lastMapBearing: Double? = null,
  val lastMapPitch: Double? = null,
  val searchRadiusMeters: Int? = null,
)
