package com.trm.sightline.core.data

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

internal object FeatureMapper {
  fun toAddress(feature: Feature): String? =
    listOfNotNull(
        feature.getStringProperty("name"),
        feature.getStringProperty("district"),
        feature.getStringProperty("locality"),
        feature.getStringProperty("city"),
        feature.getStringProperty("country"),
      )
      .filter(String::isNotBlank)
      .takeUnless(List<String>::isEmpty)
      ?.joinToString(separator = ", ")

  fun toCustomLocation(feature: Feature): CustomLocation? {
    val address = toAddress(feature) ?: return null
    val point = feature.geometry() as? Point ?: return null
    return CustomLocation(
      latitude = point.latitude(),
      longitude = point.longitude(),
      address = address,
      timestamp = System.currentTimeMillis(),
    )
  }

  fun toPlace(feature: Feature, category: PlaceCategory): Place? {
    val name = feature.getStringProperty("name")?.trim()
    if (name.isNullOrBlank()) return null
    val osmType = feature.getStringProperty("osm_type") ?: return null
    val osmId = feature.getNumberProperty("osm_id") ?: return null
    val point = feature.geometry() as? Point ?: return null
    return Place(
      id = "$osmType:${osmId.toLong()}",
      name = name,
      latitude = point.latitude(),
      longitude = point.longitude(),
      category = category,
    )
  }
}
