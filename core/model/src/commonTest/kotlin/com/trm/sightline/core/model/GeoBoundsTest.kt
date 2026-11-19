package com.trm.sightline.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeoBoundsTest {
  @Test
  fun `bounds of an empty place list are null`() {
    assertNull(GeoBounds.paddedAround(emptyList(), percentageIncrease = 0.1))
  }

  @Test
  fun `bounds without padding match the place extremes`() {
    val bounds =
      GeoBounds.paddedAround(
        places =
          listOf(
            place(id = "a", latitude = 50.0614, longitude = 19.9366),
            place(id = "b", latitude = 50.0700, longitude = 19.9200),
            place(id = "c", latitude = 50.0500, longitude = 19.9500),
          ),
        percentageIncrease = 0.0,
      )

    assertEquals(19.9200, bounds?.west)
    assertEquals(50.0500, bounds?.south)
    assertEquals(19.9500, bounds?.east)
    assertEquals(50.0700, bounds?.north)
  }

  @Test
  fun `padding extends each side by half of the percentage increase`() {
    val bounds =
      GeoBounds.paddedAround(
        places =
          listOf(
            place(id = "south-west", latitude = 50.0, longitude = 19.0),
            place(id = "north-east", latitude = 50.2, longitude = 20.0),
          ),
        percentageIncrease = 0.1,
      )

    assertEquals(18.95, bounds?.west)
    assertEquals(49.99, bounds?.south)
    assertEquals(20.05, bounds?.east)
    assertEquals(50.21, bounds?.north)
  }

  @Test
  fun `a single place produces a degenerate box around it`() {
    val bounds =
      GeoBounds.paddedAround(
        places = listOf(place(id = "only", latitude = 50.0614, longitude = 19.9366)),
        percentageIncrease = 0.5,
      )

    assertEquals(19.9366, bounds?.west)
    assertEquals(50.0614, bounds?.south)
    assertEquals(19.9366, bounds?.east)
    assertEquals(50.0614, bounds?.north)
  }

  @Test
  fun `bounds center is the middle of the box`() {
    val bounds =
      GeoBounds.paddedAround(
        places =
          listOf(
            place(id = "south-west", latitude = 50.0, longitude = 19.0),
            place(id = "north-east", latitude = 50.2, longitude = 20.0),
          ),
        percentageIncrease = 0.0,
      )

    assertEquals(50.1, bounds?.center?.latitude)
    assertEquals(19.5, bounds?.center?.longitude)
  }

  private fun place(id: String, latitude: Double, longitude: Double) =
    Place(
      id = id,
      name = id,
      latitude = latitude,
      longitude = longitude,
      category = PlaceCategory.SightsLandmarks,
    )
}
