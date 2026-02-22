package com.trm.sightline.core.ar.model

import android.location.Location
import java.util.Objects
import java.util.UUID

data class Marker(
  val name: String,
  val location: Location,
  val tags: Map<String, String> = emptyMap(),
  val id: UUID = UUID.randomUUID(),
) {
  override fun equals(other: Any?): Boolean = this === other || (other is Marker && other.id == id)

  override fun hashCode(): Int = Objects.hash(id)
}
