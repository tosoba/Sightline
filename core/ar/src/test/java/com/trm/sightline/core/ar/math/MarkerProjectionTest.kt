package com.trm.sightline.core.ar.math

import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.model.GeoLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkerProjectionTest {
  private val camera = GeoLocation(latitude = 52.52, longitude = 13.405, altitude = 34.0)
  private val north = GeoLocation(latitude = 52.5202, longitude = 13.405, altitude = 34.0)
  private val south = GeoLocation(latitude = 52.5198, longitude = 13.405, altitude = 34.0)
  private val facingNorth = Orientation(pitch = 0f, roll = 0f, azimuth = 0f)

  private fun projection(
    orientation: Orientation = facingNorth,
    rotation: ScreenRotation = ScreenRotation.ROTATION_0,
  ): MarkerProjection =
    MarkerProjection().apply {
      updateViewport(width = 1080, height = 2400)
      updateCamera(camera, orientation, rotation)
    }

  @Test
  fun `project maps a target straight ahead onto the screen centre`() {
    val projected = projection().project(north)

    assertTrue(projected.isInFront)
    assertTrue(projected.isDrawn)
    assertEquals(540.0, projected.screenX.toDouble(), SCREEN_TOLERANCE)
    assertEquals(1200.0, projected.screenY.toDouble(), SCREEN_TOLERANCE)
  }

  @Test
  fun `project lifts the target on screen when the camera pitches down`() {
    val projected = projection(orientation = facingNorth.copy(pitch = 0.1f)).project(north)

    assertTrue(projected.isDrawn)
    assertEquals(540.0, projected.screenX.toDouble(), SCREEN_TOLERANCE)
    assertEquals(1025.417667952412, projected.screenY.toDouble(), SCREEN_TOLERANCE)
  }

  @Test
  fun `project reports a target behind the camera as not in front and not drawn`() {
    val projected = projection().project(south)

    assertFalse(projected.isInFront)
    assertFalse(projected.isDrawn)
    assertEquals(0f, projected.screenX)
    assertEquals(0f, projected.screenY)
  }

  @Test
  fun `project keeps a target ahead of the camera when it leaves the screen horizontally`() {
    val projected = projection(orientation = facingNorth.copy(azimuth = 1.2f)).project(north)

    assertTrue(projected.isInFront)
    assertFalse(projected.isDrawn)
    assertEquals(-3935.544454393854, projected.screenX.toDouble(), SCREEN_TOLERANCE)
    assertEquals(1200.0, projected.screenY.toDouble(), SCREEN_TOLERANCE)
  }

  @Test
  fun `project keeps a target that stays inside the horizontal bounds drawn`() {
    val projected = projection(orientation = facingNorth.copy(azimuth = 0.6f)).project(north)

    assertTrue(projected.isDrawn)
    assertEquals(-650.3981074160711, projected.screenX.toDouble(), SCREEN_TOLERANCE)
    assertEquals(1200.0, projected.screenY.toDouble(), SCREEN_TOLERANCE)
  }

  @Test
  fun `project applies the screen rotation`() {
    val projected = projection(rotation = ScreenRotation.ROTATION_90).project(north)

    assertTrue(projected.isDrawn)
    assertEquals(540.0, projected.screenX.toDouble(), SCREEN_TOLERANCE)
    assertEquals(1199.9999999999998, projected.screenY.toDouble(), SCREEN_TOLERANCE)
  }

  private companion object {
    private const val SCREEN_TOLERANCE = 0.001
  }
}
