package com.trm.sightline.api.nominatim.models.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DetailsResponse(
  @Json(name = "place_id") val placeId: Long,
  @Json(name = "parent_place_id") val parentPlaceId: Long? = null,
  @Json(name = "osm_type") val osmType: String,
  @Json(name = "osm_id") val osmId: Long,
  @Json(name = "category") val category: String? = null,
  @Json(name = "type") val type: String? = null,
  @Json(name = "admin_level") val adminLevel: Int? = null,
  @Json(name = "localname") val localname: String? = null,
  @Json(name = "names") val names: Map<String, String>? = null,
  @Json(name = "addresstags") val addresstags: Map<String, String>? = null,
  @Json(name = "calculated_postcode") val calculatedPostcode: String? = null,
  @Json(name = "country_code") val countryCode: String? = null,
  @Json(name = "indexed_date") val indexedDate: String? = null,
  @Json(name = "importance") val importance: Double? = null,
  @Json(name = "calculated_importance") val calculatedImportance: Double? = null,
  @Json(name = "extratags") val extratags: Map<String, String>? = null,
  @Json(name = "rank_address") val rankAddress: Int? = null,
  @Json(name = "rank_search") val rankSearch: Int? = null,
  @Json(name = "isarea") val isarea: Boolean? = null,
  @Json(name = "centroid") val centroid: Geometry? = null,
  @Json(name = "geometry") val geometry: Geometry? = null,
  @Json(name = "address") val address: List<AddressEntry>? = null,
)

@JsonClass(generateAdapter = true)
data class AddressEntry(
  @Json(name = "localname") val localname: String? = null,
  @Json(name = "place_id") val placeId: Long? = null,
  @Json(name = "osm_id") val osmId: Long? = null,
  @Json(name = "osm_type") val osmType: String? = null,
  @Json(name = "class") val className: String? = null,
  @Json(name = "type") val type: String? = null,
  @Json(name = "admin_level") val adminLevel: Int? = null,
  @Json(name = "rank_address") val rankAddress: Int? = null,
  @Json(name = "distance") val distance: Double? = null,
  @Json(name = "isaddress") val isaddress: Boolean? = null,
)
