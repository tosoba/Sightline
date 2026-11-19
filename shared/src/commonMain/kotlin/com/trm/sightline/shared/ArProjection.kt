package com.trm.sightline.shared

import com.trm.sightline.core.ar.math.MarkerProjection
import com.trm.sightline.core.ar.math.ProjectedMarker
import com.trm.sightline.core.ar.math.RoundedRectF
import com.trm.sightline.core.ar.math.ScreenRotation
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

  fun projectAll(locations: List<GeoLocation>): List<ProjectedMarker> =
    locations.map(projection::project)

  /**
   * Screen-space hole for one marker, centred on its projected position. The overlay draws its card
   * into this rect and the Metal renderer punches the matching stencil hole, so both stay aligned.
   */
  fun markerRect(
    marker: ProjectedMarker,
    widthPx: Float,
    heightPx: Float,
    cornerRadiusPx: Float,
  ): RoundedRectF =
    RoundedRectF(
      left = marker.screenX - widthPx / 2f,
      top = marker.screenY - heightPx / 2f,
      right = marker.screenX + widthPx / 2f,
      bottom = marker.screenY + heightPx / 2f,
      cornerRadius = cornerRadiusPx,
    )
}
