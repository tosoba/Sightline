package com.trm.sightline.api.nominatim.models.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OSMGeocodeJson(
  @Json(name = "type") val type: String = "FeatureCollection",
  @Json(name = "geocoding") val geocoding: GeocodingMetadata,
  @Json(name = "features") val features: List<Feature> = emptyList(),
  @Json(name = "licence") val licence: String? = null,
  @Json(name = "attribution") val attribution: String? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingMetadata(
  @Json(name = "version") val version: String,
  @Json(name = "licence") val licence: String? = null,
  @Json(name = "attribution") val attribution: String? = null,
  @Json(name = "query") val query: String? = null,
)

@JsonClass(generateAdapter = true)
data class Feature(
  @Json(name = "type") val type: String = "Feature",
  @Json(name = "geometry") val geometry: Geometry?,
  @Json(name = "properties") val properties: FeatureProperties,
  @Json(name = "bbox") val bbox: List<Double>? = null,
)

@JsonClass(generateAdapter = true)
data class Geometry(
  @Json(name = "type") val type: String,
  // Can be List<Double> for Point, List<List<Double>> for LineString, etc.
  @Json(name = "coordinates") val coordinates: List<Any>,
)

@JsonClass(generateAdapter = true)
data class FeatureProperties(@Json(name = "geocoding") val geocoding: FeatureGeocoding)

@JsonClass(generateAdapter = true)
data class FeatureGeocoding(
  @Json(name = "place_id") val placeId: Long? = null,
  @Json(name = "osm_type") val osmType: String? = null,
  @Json(name = "osm_id") val osmId: Long? = null,
  @Json(name = "osm_key") val osmKey: String? = null,
  @Json(name = "osm_value") val osmValue: String? = null,
  @Json(name = "type") val type: String,
  @Json(name = "accuracy") val accuracy: Double? = null,
  @Json(name = "label") val label: String? = null,
  @Json(name = "name") val name: String? = null,
  @Json(name = "housenumber") val housenumber: String? = null,
  @Json(name = "street") val street: String? = null,
  @Json(name = "locality") val locality: String? = null,
  @Json(name = "postcode") val postcode: String? = null,
  @Json(name = "city") val city: String? = null,
  @Json(name = "district") val district: String? = null,
  @Json(name = "county") val county: String? = null,
  @Json(name = "state") val state: String? = null,
  @Json(name = "country") val country: String? = null,
  @Json(name = "admin") val admin: Map<String, String>? = null,
)
