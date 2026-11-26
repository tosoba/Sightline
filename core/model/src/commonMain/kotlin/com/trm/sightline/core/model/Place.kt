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
  val location: GeoLocation
    get() = GeoLocation(latitude = latitude, longitude = longitude)

  val initials: String
    get() {
      val words = name.trim().split(WHITESPACE).filter(String::isNotEmpty)
      return when {
        words.isEmpty() -> "?"
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        else -> words[0].first().toString().uppercase()
      }
    }

  override fun equals(other: Any?): Boolean = this === other || (other is Place && other.id == id)

  override fun hashCode(): Int = id.hashCode()

  private companion object {
    private val WHITESPACE = Regex("\\s+")
  }
}
