package com.trm.sightline.core.model

import java.util.Objects
import java.util.UUID

data class Marker(
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val tags: Map<String, String> = emptyMap(),
  val id: UUID = UUID.randomUUID(),
) {
  override fun equals(other: Any?): Boolean = this === other || (other is Marker && other.id == id)

  override fun hashCode(): Int = Objects.hash(id)
}
