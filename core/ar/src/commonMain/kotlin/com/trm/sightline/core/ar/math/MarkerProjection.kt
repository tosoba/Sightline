package com.trm.sightline.core.ar.math

import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.model.GeoLocation

/** Screen position of a marker plus the flags the overlay renderer needs to draw it. */
data class ProjectedMarker(
  val screenX: Float,
  val screenY: Float,
  /** The marker projects inside the horizontal bounds, so it can be painted. */
  val isDrawn: Boolean,
  /** The marker is in front of the camera, so its screen position is meaningful. */
  val isInFront: Boolean,
)

/**
 * Maps geographic points to screen pixels for the current viewport, camera pose and screen
 * rotation. Pure geometry: the caller owns the viewport size and the sensor-derived [Orientation].
 */
class MarkerProjection {
  private val camPos = Vector3()
  private val camTrig = Trig3()
  private val camRot = Vector3()
  private val screenRot = Vector1()
  private val screenRotTrig = Trig1()
  private val screenRatio = Vector3()
  private val screenSize = Vector2()

  init {
    screenRatio.z = SCREEN_DEPTH.toDouble()
  }

  fun updateViewport(width: Int, height: Int) {
    val ratio = ((width + height).toFloat() / 2).toDouble()
    screenRatio.x = ratio
    screenRatio.y = ratio
    screenSize.x = width.toDouble()
    screenSize.y = height.toDouble()
  }

  fun updateCamera(
    location: GeoLocation,
    orientation: Orientation,
    screenRotation: ScreenRotation,
  ) {
    Math3D.getCamRotation(orientation, screenRotation, camRot, camTrig, screenRot, screenRotTrig)
    Math3D.convertLocationToPosition(location, camPos)
  }

  fun project(location: GeoLocation): ProjectedMarker {
    val markerPos = Vector3()
    Math3D.convertLocationToPosition(location, markerPos)
    val relativePos = Vector3()
    Math3D.getRelativeTranslationInMeters(markerPos, camPos, relativePos)
    val relativeRotPos = Vector3()
    Math3D.getRelativeRotation(relativePos, camTrig, relativeRotPos)
    val screenPos = Vector2()
    val isDrawn =
      Math3D.convert3dTo2d(relativeRotPos, screenSize, screenRatio, screenRotTrig, screenPos)

    return ProjectedMarker(
      screenX = screenPos.x.toFloat(),
      screenY = screenPos.y.toFloat(),
      isDrawn = isDrawn,
      isInFront = relativeRotPos.z > 0,
    )
  }

  private companion object {
    private const val SCREEN_DEPTH = 1
  }
}
