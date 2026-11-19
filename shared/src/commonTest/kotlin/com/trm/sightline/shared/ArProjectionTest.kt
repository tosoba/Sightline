package com.trm.sightline.shared

import com.trm.sightline.core.ar.math.ScreenRotation
import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.model.GeoLocation
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Seam: the Swift-facing [ArProjection] facade.
 * These catch wrong state delegation, projection ordering and hole-rect geometry.
 * They say nothing about the Metal renderer that consumes the rects.
 */
class ArProjectionTest {
  private val camera = GeoLocation(latitude = 52.52, longitude = 13.405, altitude = 34.0)
  private val north = GeoLocation(latitude = 52.5202, longitude = 13.405, altitude = 34.0)
  private val south = GeoLocation(latitude = 52.5198, longitude = 13.405, altitude = 34.0)
  private val east = GeoLocation(latitude = 52.52, longitude = 13.415, altitude = 34.0)
  private val facingNorth = Orientation(pitch = 0f, roll = 0f, azimuth = 0f)

  private fun projection(
    orientation: Orientation = facingNorth,
    rotation: ScreenRotation = ScreenRotation.ROTATION_0,
  ): ArProjection =
    ArProjection().apply {
      updateViewport(widthPx = 1080, heightPx = 2400)
      updateCamera(camera, orientation, rotation)
    }

  @Test
  fun `a place dead ahead projects onto the viewport centre`() {
    val marker = projection().project(north)

    assertTrue(marker.isInFront)
    assertTrue(marker.isDrawn)
    assertEquals(540.0, marker.screenX.toDouble(), CENTRE_TOLERANCE)
    assertEquals(1200.0, marker.screenY.toDouble(), CENTRE_TOLERANCE)
  }

  @Test
  fun `updateCamera replaces the pose used by later projections`() {
    val facade = projection()
    facade.updateCamera(
      location = camera,
      orientation = facingNorth.copy(azimuth = (PI / 2).toFloat()),
      screenRotation = ScreenRotation.ROTATION_0,
    )

    val marker = facade.project(east)

    assertTrue(marker.isInFront)
    assertEquals(540.0, marker.screenX.toDouble(), CENTRE_TOLERANCE)
    assertEquals(1200.0, marker.screenY.toDouble(), CENTRE_TOLERANCE)
  }

  @Test
  fun `projectAll keeps the input order and flags targets behind the camera`() {
    val markers = projection().projectAll(listOf(north, south))

    assertEquals(2, markers.size)
    assertEquals(540.0, markers[0].screenX.toDouble(), CENTRE_TOLERANCE)
    assertTrue(markers[0].isInFront)
    assertFalse(markers[1].isInFront)
    assertFalse(markers[1].isDrawn)
  }

  @Test
  fun `markerRect centres a rounded rect on the projected marker`() {
    val facade = projection()
    val marker = facade.project(north)

    val rect = facade.markerRect(marker, widthPx = 240f, heightPx = 64f, cornerRadiusPx = 16f)

    assertEquals(420.0, rect.left.toDouble(), CENTRE_TOLERANCE)
    assertEquals(1168.0, rect.top.toDouble(), CENTRE_TOLERANCE)
    assertEquals(660.0, rect.right.toDouble(), CENTRE_TOLERANCE)
    assertEquals(1232.0, rect.bottom.toDouble(), CENTRE_TOLERANCE)
    assertEquals(16f, rect.cornerRadius)
  }

  private companion object {
    private const val CENTRE_TOLERANCE = 0.01
  }
}
