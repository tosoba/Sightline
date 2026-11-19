package com.trm.sightline.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Place(
  val id: String,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val category: PlaceCategory,
) {
  override fun equals(other: Any?): Boolean = this === other || (other is Place && other.id == id)

  override fun hashCode(): Int = id.hashCode()
}
