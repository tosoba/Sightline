package com.trm.sightline.core.model

import java.util.Objects
import kotlinx.serialization.Serializable

@Serializable
data class Place(
  val id: Long,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val category: PlaceCategory,
  val tags: Map<String, String>,
) {
  override fun equals(other: Any?): Boolean = this === other || (other is Place && other.id == id)

  override fun hashCode(): Int = Objects.hash(id)
}
