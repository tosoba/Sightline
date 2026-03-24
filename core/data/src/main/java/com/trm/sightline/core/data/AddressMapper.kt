package com.trm.sightline.core.data

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.trm.sightline.core.model.SearchResult

internal object AddressMapper {
  fun toAddress(feature: Feature): String? {
    val parts =
      listOfNotNull(
          feature.getStringProperty("street"),
          feature.getStringProperty("district") ?: feature.getStringProperty("locality"),
          feature.getStringProperty("city"),
          feature.getStringProperty("country"),
        )
        .filter(String::isNotBlank)
    if (parts.isEmpty()) return null
    return parts.joinToString(", ")
  }

  fun toSearchResult(feature: Feature): SearchResult? {
    val address = toAddress(feature) ?: return null
    val point = feature.geometry() as? Point ?: return null
    return SearchResult(
      latitude = point.latitude(),
      longitude = point.longitude(),
      address = address,
    )
  }
}
