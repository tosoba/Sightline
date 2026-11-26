package com.trm.sightline.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceTest {
  @Test
  fun `location mirrors the place coordinates`() {
    val place = place(name = "Main Market", latitude = 52.52, longitude = 13.405)

    assertEquals(GeoLocation(latitude = 52.52, longitude = 13.405), place.location)
  }

  @Test
  fun `initials join the first letters of the first two words`() {
    assertEquals("MM", place(name = "Main Market").initials)
    assertEquals("ST", place(name = "St. Thomas Church").initials)
  }

  @Test
  fun `initials uppercase a single word`() {
    assertEquals("S", place(name = "statue").initials)
  }

  @Test
  fun `initials ignore surrounding and repeated whitespace`() {
    assertEquals("MM", place(name = "  Main \t  Market  ").initials)
  }

  @Test
  fun `initials fall back to a placeholder when there are no words`() {
    assertEquals("?", place(name = "   ").initials)
    assertEquals("?", place(name = "").initials)
  }

  private fun place(
    name: String,
    latitude: Double = 52.52,
    longitude: Double = 13.405,
  ): Place =
    Place(
      id = "place-1",
      name = name,
      latitude = latitude,
      longitude = longitude,
      category = PlaceCategory.SightsLandmarks,
    )
}
