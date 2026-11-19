package com.trm.sightline.core.data

import com.trm.sightline.api.photon.Feature
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlin.time.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal object FeatureMapper {
  fun toAddress(feature: Feature): String? =
    listOfNotNull(
        feature.stringProperty("name"),
        feature.stringProperty("district"),
        feature.stringProperty("locality"),
        feature.stringProperty("city"),
        feature.stringProperty("country"),
      )
      .filter(String::isNotBlank)
      .takeUnless(List<String>::isEmpty)
      ?.joinToString(separator = ", ")

  fun toCustomLocation(feature: Feature): CustomLocation? {
    val address = toAddress(feature) ?: return null
    val point = feature.point() ?: return null
    return CustomLocation(
      latitude = point.latitude,
      longitude = point.longitude,
      address = address,
      timestamp = Clock.System.now().toEpochMilliseconds(),
    )
  }

  fun toPlace(feature: Feature, category: PlaceCategory): Place? {
    val name = feature.stringProperty("name")?.trim()
    if (name.isNullOrBlank()) return null
    val osmType = feature.stringProperty("osm_type") ?: return null
    val osmId = feature.longProperty("osm_id") ?: return null
    val point = feature.point() ?: return null
    return Place(
      id = "$osmType:$osmId",
      name = name,
      latitude = point.latitude,
      longitude = point.longitude,
      category = category,
    )
  }

  private fun Feature.stringProperty(key: String): String? =
    (properties[key] as? JsonPrimitive)?.contentOrNull

  private fun Feature.longProperty(key: String): Long? =
    (properties[key] as? JsonPrimitive)?.longOrNull

  private fun Feature.point(): Coordinates? {
    val geometry = geometry ?: return null
    if (geometry.type != POINT_TYPE) return null
    val coordinates = geometry.coordinates as? JsonArray ?: return null
    val longitude = (coordinates.getOrNull(0) as? JsonPrimitive)?.doubleOrNull ?: return null
    val latitude = (coordinates.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: return null
    return Coordinates(latitude = latitude, longitude = longitude)
  }

  private data class Coordinates(val latitude: Double, val longitude: Double)

  private const val POINT_TYPE = "Point"
}
