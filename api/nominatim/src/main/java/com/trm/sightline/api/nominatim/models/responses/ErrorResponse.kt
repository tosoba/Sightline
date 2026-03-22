package com.trm.sightline.api.nominatim.models.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorResponse(
  @Json(name = "code") val code: Int? = null,
  @Json(name = "message") val message: String? = null,
)
