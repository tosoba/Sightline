package com.trm.sightline.core.data

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.trm.sightline.core.model.CustomLocation

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
}
