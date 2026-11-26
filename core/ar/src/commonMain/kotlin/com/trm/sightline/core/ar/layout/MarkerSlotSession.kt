package com.trm.sightline.core.ar.layout

import com.trm.sightline.core.ar.math.MarkerSlot
import com.trm.sightline.core.ar.math.MarkerSlotAssignment
import com.trm.sightline.core.ar.math.MarkerSlotGeometry
import com.trm.sightline.core.ar.math.MarkerSlotTarget
import com.trm.sightline.core.ar.math.ScreenRotation
import com.trm.sightline.core.model.GeoLocation

/** Result of one [MarkerSlotSession.update] pass. */
data class MarkerSlotUpdate(
  val slots: Map<String, MarkerSlot>,
  val currentPage: Int,
  val totalPages: Int,
  val didReassign: Boolean,
)

/**
 * Stateful owner of the AR Marker slot cache and page state.
 *
 * The session composes [MarkerSlotAssignment] and delegates geometry to [MarkerLayoutCalculator].
 * It reuses its cached slots while the Place ID list, layout geometry, Screen Rotation and canvas
 * size are unchanged and the Active Location moved less than [LOCATION_REASSIGN_THRESHOLD_METERS].
 * Every reassignment follows the page holding the most previously visible Place IDs (ties to the
 * lowest page) and clamps [currentPage] into `0..totalPages - 1`; when no previously visible Place
 * survives, the current page is kept.
 *
 * Not thread-safe: drive it from the same thread as the renderer that owns it (Android's main
 * thread, iOS's main actor), mirroring [MarkerSlotAssignment] and
 * [com.trm.sightline.core.ar.math.ArProjection].
 */
class MarkerSlotSession(
  private val locationReassignThresholdMeters: Float = LOCATION_REASSIGN_THRESHOLD_METERS,
) {
  var currentPage: Int = 0
    private set

  var totalPages: Int = 1
    private set

  var slots: Map<String, MarkerSlot> = emptyMap()
    private set

  private var cache: SlotCache? = null

  fun update(
    povLocation: GeoLocation,
    targets: List<MarkerSlotTarget>,
    geometry: MarkerLayoutGeometry,
    screenRotation: ScreenRotation,
    canvasWidth: Int,
    canvasHeight: Int,
    previouslyVisibleIds: Set<String>,
  ): MarkerSlotUpdate {
    if (canvasWidth <= 0 || canvasHeight <= 0) {
      return MarkerSlotUpdate(slots, currentPage, totalPages, didReassign = false)
    }

    val placeIds = targets.map { it.id }
    val cached = cache
    if (
      cached != null &&
        cached.placeIds == placeIds &&
        cached.geometry == geometry &&
        cached.screenRotation == screenRotation &&
        cached.canvasWidth == canvasWidth &&
        cached.canvasHeight == canvasHeight &&
        cached.location.distanceTo(povLocation) <= locationReassignThresholdMeters
    ) {
      slots = cached.slots
      applyPageBounds(cached.slots)
      return MarkerSlotUpdate(slots, currentPage, totalPages, didReassign = false)
    }

    val assigned =
      MarkerSlotAssignment.assign(
        povLocation = povLocation,
        targets = targets,
        geometry =
          MarkerSlotGeometry(
            markerWidthPx = geometry.markerWidth,
            markerHeightPx = geometry.markerHeight,
            overlapGuardPx = geometry.overlapGuard,
            verticalSpacingPx = geometry.verticalSpacing,
            rows = geometry.rows,
            baseY = geometry.baseY,
          ),
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
      )

    slots = assigned
    cache =
      SlotCache(
        location = povLocation,
        geometry = geometry,
        screenRotation = screenRotation,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        placeIds = placeIds,
        slots = assigned,
      )

    val followedPage = followedPage(previouslyVisibleIds, assigned)
    if (followedPage != null) currentPage = followedPage
    applyPageBounds(assigned)
    return MarkerSlotUpdate(slots, currentPage, totalPages, didReassign = true)
  }

  /** Selects [page] clamped into the pager; returns whether the current page changed. */
  fun selectPage(page: Int): Boolean {
    val clamped = page.coerceIn(0, totalPages - 1)
    if (clamped == currentPage) return false
    currentPage = clamped
    return true
  }

  private fun applyPageBounds(slots: Map<String, MarkerSlot>) {
    val maxPage = slots.values.maxOfOrNull { it.page } ?: 0
    totalPages = maxPage + 1
    currentPage = currentPage.coerceIn(0, maxPage)
  }

  /** The page holding the most previously visible IDs, or `null` when none survived. */
  private fun followedPage(
    previouslyVisibleIds: Set<String>,
    slots: Map<String, MarkerSlot>,
  ): Int? {
    if (previouslyVisibleIds.isEmpty()) return null
    val counts = HashMap<Int, Int>()
    previouslyVisibleIds.forEach { id ->
      slots[id]?.let { slot -> counts[slot.page] = (counts[slot.page] ?: 0) + 1 }
    }
    return counts.entries
      .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
      .firstOrNull()
      ?.key
  }

  private data class SlotCache(
    val location: GeoLocation,
    val geometry: MarkerLayoutGeometry,
    val screenRotation: ScreenRotation,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val placeIds: List<String>,
    val slots: Map<String, MarkerSlot>,
  )

  companion object {
    const val LOCATION_REASSIGN_THRESHOLD_METERS = 10f
  }
}
