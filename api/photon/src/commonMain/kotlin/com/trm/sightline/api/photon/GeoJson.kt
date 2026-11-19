package com.trm.sightline.api.photon

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class FeatureCollection(val features: List<Feature> = emptyList())

@Serializable
data class Feature(
  val properties: JsonObject = JsonObject(emptyMap()),
  val geometry: Geometry? = null,
)

@Serializable
data class Geometry(val type: String, val coordinates: JsonElement? = null)
