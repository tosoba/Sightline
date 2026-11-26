package com.trm.sightline.core.ar.math

import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.model.GeoLocation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
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

  /** 100 m ahead, 10 degrees to the right of the camera direction. */
  private val horizontal = GeoLocation(
    latitude = camera.latitude + cos(10.0 * PI / 180.0) * 100.0 / METERS_PER_DEGREE,
    longitude = camera.longitude + sin(10.0 * PI / 180.0) * 100.0 / METERS_PER_DEGREE,
    altitude = camera.altitude,
  )

  /** 100 m dead ahead, 20 m above the camera so the offset projects vertically. */
  private val vertical = GeoLocation(
    latitude = camera.latitude + 100.0 / METERS_PER_DEGREE,
    longitude = camera.longitude,
    altitude = camera.altitude + 20.0,
  )

  private fun projection(
    orientation: Orientation = facingNorth,
    rotation: ScreenRotation = ScreenRotation.ROTATION_0,
  ): ArProjection =
    ArProjection().apply {
      val viewport = viewportFor(rotation)
      updateViewport(widthPx = viewport.width, heightPx = viewport.height)
      updateCamera(camera, orientation, rotation)
    }

  private data class Viewport(
    val width: Int,
    val height: Int,
    val centreX: Double,
    val centreY: Double,
  )

  private fun viewportFor(rotation: ScreenRotation): Viewport =
    when (rotation) {
      ScreenRotation.ROTATION_0, ScreenRotation.ROTATION_180 -> Viewport(1080, 2400, 540.0, 1200.0)
      ScreenRotation.ROTATION_90,
      ScreenRotation.ROTATION_270 -> Viewport(2400, 1080, 1200.0, 540.0)
    }

  /** A physically rotated device reports the Screen Rotation as roll; both feed the projection. */
  private fun orientationFor(rotation: ScreenRotation): Orientation =
    facingNorth.copy(roll = rotation.rollOffsetRadians)

  private fun project(location: GeoLocation, rotation: ScreenRotation): ProjectedMarker =
    projection(orientation = orientationFor(rotation), rotation = rotation).project(location)

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

  @Test
  fun `a horizontal place offset stays on screen x across every screen rotation`() {
    val expectedScreenX =
      mapOf(
        ScreenRotation.ROTATION_0 to 846.8,
        ScreenRotation.ROTATION_90 to 1506.8,
        ScreenRotation.ROTATION_180 to 846.8,
        ScreenRotation.ROTATION_270 to 1506.8,
      )

    for ((rotation, screenX) in expectedScreenX) {
      val viewport = viewportFor(rotation)
      val marker = project(horizontal, rotation)

      assertTrue(marker.isInFront)
      assertTrue(marker.isDrawn)
      assertEquals(screenX, marker.screenX.toDouble(), AXIS_TOLERANCE)
      assertEquals(viewport.centreY, marker.screenY.toDouble(), CENTRE_TOLERANCE)
    }
  }

  @Test
  fun `a vertical place offset stays on screen y across every screen rotation`() {
    val expectedScreenY =
      mapOf(
        ScreenRotation.ROTATION_0 to 852.0,
        ScreenRotation.ROTATION_90 to 192.0,
        ScreenRotation.ROTATION_180 to 852.0,
        ScreenRotation.ROTATION_270 to 192.0,
      )

    for ((rotation, screenY) in expectedScreenY) {
      val viewport = viewportFor(rotation)
      val marker = project(vertical, rotation)

      assertTrue(marker.isInFront)
      assertTrue(marker.isDrawn)
      assertEquals(viewport.centreX, marker.screenX.toDouble(), CENTRE_TOLERANCE)
      assertEquals(screenY, marker.screenY.toDouble(), AXIS_TOLERANCE)
    }
  }

  private companion object {
    private const val CENTRE_TOLERANCE = 0.01
    private const val AXIS_TOLERANCE = 1.0
    private const val METERS_PER_DEGREE = 111111.0
  }
}
