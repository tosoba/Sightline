package com.trm.sightline.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoLocationTest {
  private val berlin = GeoLocation(latitude = 52.5200, longitude = 13.4050)
  private val hamburg = GeoLocation(latitude = 53.5511, longitude = 9.9937)

  @Test
  fun `distanceTo returns haversine distance in meters`() {
    assertEquals(255_000.0, berlin.distanceTo(hamburg).toDouble(), 1_000.0)
  }

  @Test
  fun `distanceTo of a point to itself is zero`() {
    assertEquals(0.0, berlin.distanceTo(berlin).toDouble(), 0.001)
  }

  @Test
  fun `bearingTo north is zero degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(0.0, origin.bearingTo(GeoLocation(latitude = 1.0, longitude = 0.0)).toDouble(), 0.001)
  }

  @Test
  fun `bearingTo east is ninety degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(90.0, origin.bearingTo(GeoLocation(latitude = 0.0, longitude = 1.0)).toDouble(), 0.001)
  }

  @Test
  fun `bearingTo south is one hundred eighty degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(
      180.0,
      origin.bearingTo(GeoLocation(latitude = -1.0, longitude = 0.0)).toDouble(),
      0.001,
    )
  }

  @Test
  fun `bearingTo west is two hundred seventy degrees`() {
    val origin = GeoLocation(latitude = 0.0, longitude = 0.0)
    assertEquals(
      270.0,
      origin.bearingTo(GeoLocation(latitude = 0.0, longitude = -1.0)).toDouble(),
      0.001,
    )
  }
}
