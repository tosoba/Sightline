package com.trm.sightline.core.ar.math

import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.model.GeoLocation

/**
 * Swift-facing entry point to the shared marker-projection math.
 *
 * Wraps [MarkerProjection], which keeps mutable scratch vectors and is therefore not thread-safe:
 * keep exactly one [ArProjection] per renderer and touch it from a single thread only (the main or
 * render thread, never both). [project] and [markerRect] rely on the camera pose installed by the
 * last [updateCamera] call.
 */
class ArProjection {
  private val projection = MarkerProjection()

  fun updateViewport(widthPx: Int, heightPx: Int) {
    projection.updateViewport(widthPx, heightPx)
  }

  fun updateCamera(
    location: GeoLocation,
    orientation: Orientation,
    screenRotation: ScreenRotation,
  ) {
    projection.updateCamera(location, orientation, screenRotation)
  }

  fun project(location: GeoLocation): ProjectedMarker = projection.project(location)

  fun markerRect(
    marker: ProjectedMarker,
    widthPx: Float,
    heightPx: Float,
    cornerRadiusPx: Float,
  ): RoundedRectF =
    markerRect(
      centerX = marker.screenX,
      centerY = marker.screenY,
      widthPx = widthPx,
      heightPx = heightPx,
      cornerRadiusPx = cornerRadiusPx,
    )
}
