package com.trm.sightline.core.ar.math

import com.trm.sightline.core.model.GeoLocation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos

/** Screen-space slot a marker was assigned to. */
data class MarkerSlot(val bearing: Float, val y: Float, val page: Int)

/** Marker input for slot assignment: identity plus the values the layout depends on. */
data class MarkerSlotTarget(val id: String, val location: GeoLocation, val distance: Float)

/** Pixel geometry of the marker overlay, computed by the platform renderer. */
data class MarkerSlotGeometry(
  val markerWidthPx: Float,
  val markerHeightPx: Float,
  val overlapGuardPx: Float,
  val verticalSpacingPx: Float,
  val rows: Int,
  val baseY: Float,
)

/**
 * Assigns every marker to a vertical slot so markers with overlapping bearings stack into rows and,
 * once the rows run out, into pages.
 */
object MarkerSlotAssignment {
  fun assign(
    povLocation: GeoLocation,
    targets: List<MarkerSlotTarget>,
    geometry: MarkerSlotGeometry,
    canvasWidth: Int,
    canvasHeight: Int,
  ): Map<String, MarkerSlot> {
    if (targets.isEmpty()) return emptyMap()
    if (canvasWidth == 0 || canvasHeight == 0) return emptyMap()

    val screenRatioX = (canvasWidth + canvasHeight) / 2.0
    val effectiveScreenRatioX = screenRatioX * cos(MAX_EXPECTED_ROLL_RADIANS)
    val angularWidthDeg =
      degrees(
        2.0 *
          atan((geometry.markerWidthPx + geometry.overlapGuardPx) / (2.0 * effectiveScreenRatioX)),
      )

    val assigned = mutableListOf<Pair<Float, SlotPosition>>()
    val slots = LinkedHashMap<String, MarkerSlot>(targets.size)

    targets
      .sortedBy { it.distance }
      .forEach { target ->
        val bearing = normalizeBearing(povLocation.bearingTo(target.location))

        val conflicting =
          assigned
            .filter { (assignedBearing, _) ->
              angularDistanceDeg(bearing, assignedBearing) < angularWidthDeg
            }
            .map { (_, position) -> position }
            .toSet()

        val position = SlotPosition(geometry.baseY, 0)
        var row = 0
        while (conflicting.contains(position)) {
          position.y += geometry.markerHeightPx + geometry.verticalSpacingPx
          ++row
          if (row >= geometry.rows) {
            row = 0
            position.y = geometry.baseY
            ++position.page
          }
        }
        assigned.add(Pair(bearing, position))
        slots[target.id] = MarkerSlot(bearing, position.y, position.page)
      }

    return slots
  }

  private fun normalizeBearing(bearing: Float): Float = (bearing + 360f) % 360f

  private fun angularDistanceDeg(a: Float, b: Float): Double {
    val diff = abs(a - b) % 360f
    return (if (diff > 180f) 360f - diff else diff).toDouble()
  }

  private fun degrees(radians: Double): Double = radians * 180.0 / PI

  private data class SlotPosition(var y: Float, var page: Int)

  private const val MAX_EXPECTED_ROLL_RADIANS = PI / 6
}
