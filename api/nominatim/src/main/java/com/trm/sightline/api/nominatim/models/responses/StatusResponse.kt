package com.trm.sightline.api.nominatim.models.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusResponse(
  @Json(name = "status") val status: Int,
  @Json(name = "message") val message: String,
  @Json(name = "data_updated") val dataUpdated: String? = null,
  @Json(name = "software_version") val softwareVersion: String? = null,
  @Json(name = "database_version") val databaseVersion: String? = null,
)
