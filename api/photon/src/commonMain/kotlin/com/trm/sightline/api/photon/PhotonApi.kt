package com.trm.sightline.api.photon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PhotonApi(
  private val client: HttpClient,
  private val baseUrl: String = DEFAULT_BASE_URL,
) {
  suspend fun search(
    query: String,
    lat: Double? = null,
    lon: Double? = null,
    limit: Int? = null,
    lang: String? = null,
  ): FeatureCollection =
    client
      .get("$baseUrl/api") {
        parameter("q", query)
        parameterIfPresent("lat", lat)
        parameterIfPresent("lon", lon)
        parameterIfPresent("limit", limit)
        parameterIfPresent("lang", lang)
      }
      .body()

  suspend fun reverse(
    lat: Double,
    lon: Double,
    limit: Int? = null,
    lang: String? = null,
    radius: Double? = null,
    osmTag: List<String>? = null,
  ): FeatureCollection =
    client
      .get("$baseUrl/reverse") {
        parameter("lat", lat)
        parameter("lon", lon)
        parameterIfPresent("limit", limit)
        parameterIfPresent("lang", lang)
        parameterIfPresent("radius", radius)
        osmTag?.forEach { tag -> parameter("osm_tag", tag) }
      }
      .body()

  private fun HttpRequestBuilder.parameterIfPresent(key: String, value: Any?) {
    if (value != null) parameter(key, value)
  }

  companion object {
    const val DEFAULT_BASE_URL = "https://photon.komoot.io"
  }
}
