package com.trm.sightline.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoLocationTest {
  private val berlin = GeoLocation(latitude = 52.5200, longitude = 13.4050)
  private val hamburg = GeoLocation(latitude = 53.5511, longitude = 9.9937)

  @Test
  fun `distanceTo returns haversine distance in meters`() {
    assertEquals(255_000f, berlin.distanceTo(hamburg), 1_000f)
  }

  @Test
  fun `distanceTo of a point to itself is zero`() {
    assertEquals(0f, berlin.distanceTo(berlin), 0.001f)
  }

  @Test
  fun `bearingTo north is zero degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(0f, origin.bearingTo(GeoLocation(latitude = 1.0, longitude = 0.0)), 0.001f)
  }

  @Test
  fun `bearingTo east is ninety degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(90f, origin.bearingTo(GeoLocation(latitude = 0.0, longitude = 1.0)), 0.001f)
  }

  @Test
  fun `bearingTo south is one hundred eighty degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(180f, origin.bearingTo(GeoLocation(latitude = -1.0, longitude = 0.0)), 0.001f)
  }

  @Test
  fun `bearingTo west is two hundred seventy degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(270f, origin.bearingTo(GeoLocation(latitude = 0.0, longitude = -1.0)), 0.001f)
  }
}
