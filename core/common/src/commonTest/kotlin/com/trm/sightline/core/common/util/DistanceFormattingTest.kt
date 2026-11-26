package com.trm.sightline.core.common.util

import com.trm.sightline.core.model.PlaceSearchRadius
import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceFormattingTest {
  @Test
  fun `formatDistance rounds metres below one kilometre`() {
    assertEquals("950 m", formatDistance(meters = 950f))
    assertEquals("0 m", formatDistance(meters = 0.4f))
  }

  @Test
  fun `formatDistance drops a trailing zero from whole kilometres`() {
    assertEquals("1 km", formatDistance(meters = 1_000f))
    assertEquals("5 km", formatDistance(meters = 5_000f))
  }

  @Test
  fun `formatDistance keeps one decimal for fractional kilometres`() {
    assertEquals("1.5 km", formatDistance(meters = 1_500f))
    assertEquals("1.2 km", formatDistance(meters = 1_234f))
  }

  @Test
  fun `formatMarkerDistance always keeps one decimal for kilometres`() {
    assertEquals("1.0 km", formatMarkerDistance(meters = 1_000f))
    assertEquals("1.5 km", formatMarkerDistance(meters = 1_500f))
    assertEquals("950 m", formatMarkerDistance(meters = 950f))
  }

  @Test
  fun `search radius labels use the shared distance format`() {
    assertEquals("500 m", PlaceSearchRadius.FiveHundred.formattedLabel)
    assertEquals("1 km", PlaceSearchRadius.OneKilometer.formattedLabel)
    assertEquals("2 km", PlaceSearchRadius.TwoKilometers.formattedLabel)
    assertEquals("5 km", PlaceSearchRadius.FiveKilometers.formattedLabel)
  }
}
