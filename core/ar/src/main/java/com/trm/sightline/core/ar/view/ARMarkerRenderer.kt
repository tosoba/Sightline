package com.trm.sightline.core.ar.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import com.trm.sightline.core.ar.model.ARMarker
import com.trm.sightline.core.ar.model.MarkersPagingState
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.util.bottomSheetHeightPx
import com.trm.sightline.core.ar.util.cameraPreviewVerticalPaddingPx
import com.trm.sightline.core.ar.util.dpToPx
import com.trm.sightline.core.ar.util.drawMultilineText
import com.trm.sightline.core.ar.util.isCompactHeight
import com.trm.sightline.core.ar.util.navigationBarsBottomInsetPx
import com.trm.sightline.core.ar.util.spToPx
import com.trm.sightline.core.ar.util.statusBarTopInsetPx
import com.trm.sightline.core.common.util.roundToDecimalPlaces
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Objects
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt

class ARMarkerRenderer(private val context: Context) {
  private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
  private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
  private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

  internal val markerHeightPx: Float
  internal val markerWidthPx: Float

  private val overlapGuardPx: Float
  private val cameraViewHeightPx: Float

  // Minimum height for a marker to be legible: 2 title lines + 1 distance line +
  // top and bottom padding. Derived from actual text sizes so it scales with
  // font size and display density rather than a fixed breakpoint.
  private val minMarkerHeightPx: Float
    get() {
      val titleLineHeightPx = markerTitleTextSizePx * LINE_HEIGHT_MULTIPLIER
      val distanceLineHeightPx = markerDistanceTextSizePx * LINE_HEIGHT_MULTIPLIER
      return titleLineHeightPx * 2 + distanceLineHeightPx + markerPaddingPx * 2
    }

  // How many rows fit given the available height and the minimum legible marker height.
  // MIN_ROWS = 1 so that in landscape (compact height) we always produce at least one
  // fully-sized row rather than two undersized ones. MAX_ROWS guards against
  // unusually tall screens producing excessive rows.
  private val numberOfRows: Int
    get() =
      (cameraViewHeightPx / (minMarkerHeightPx + MARKER_VERTICAL_SPACING_PX))
        .toInt()
        .coerceIn(MIN_ROWS, MAX_ROWS)

  // How many markers fit side-by-side given the screen width and the minimum
  // readable marker width. Scales continuously across phones, tablets and foldables
  // rather than snapping at a single compact/non-compact breakpoint.
  private val markerWidthDivisor: Int
    get() {
      val screenWidthDp = context.resources.displayMetrics.run { widthPixels / density }
      return (screenWidthDp / TARGET_MARKER_WIDTH_DP)
        .toInt()
        .coerceIn(MIN_WIDTH_DIVISOR, MAX_WIDTH_DIVISOR)
    }

  init {
    val displayMetrics = context.resources.displayMetrics
    // In compact height (landscape) the bottom sheet is not shown, so subtract only
    // the navigation bar inset. Subtracting the full bottomSheetHeightPx (~234dp)
    // from an already-small landscape height would leave almost no room for markers.
    cameraViewHeightPx =
      displayMetrics.heightPixels -
        context.statusBarTopInsetPx -
        context.cameraPreviewVerticalPaddingPx -
        if (context.isCompactHeight) context.navigationBarsBottomInsetPx
        else context.bottomSheetHeightPx
    markerHeightPx = cameraViewHeightPx / numberOfRows - MARKER_VERTICAL_SPACING_PX
    markerWidthPx = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
    overlapGuardPx = markerWidthPx * MARKER_OVERLAP_GUARD_FRACTION
  }

  var povLocation: Location? = null
    @MainThread
    set(value) {
      val previous = field
      field = value
      if (value != null) {
        val shouldReassign =
          previous == null || previous.distanceTo(value) > LOCATION_REASSIGN_THRESHOLD_METERS
        if (shouldReassign) reassignSlots(value)
      }
    }

  var currentPage: Int = 0
    @MainThread
    set(value) {
      assert(value >= 0)
      field = value
    }

  private var maxPage: Int = 0

  private var firstFrame: Boolean = true
  private var lastDrawnMarkerIds = HashSet<Long>()

  private val _markersPagingState = MutableStateFlow(MarkersPagingState(currentPage, maxPage))
  val markersPagingState: StateFlow<MarkersPagingState> = _markersPagingState.asStateFlow()

  private val _drawnMarkerRectFs = MutableStateFlow<List<RoundedRectF>>(emptyList())
  val drawnMarkerRectFs: StateFlow<List<RoundedRectF>> = _drawnMarkerRectFs.asStateFlow()

  var disabled: Boolean = false
    @MainThread set

  private val pagedMarkers = HashMap<Long, PagedMarker>()

  private var lastCanvasWidth: Int = 0
  private var lastCanvasHeight: Int = 0

  // Computed from actual canvas dimensions in reassignSlots.
  //
  // Full derivation:
  //   In convert3dTo2d, the screen X separation for two markers in the same row is:
  //     ΔscreenX = ΔvpX · cos(roll) - ΔvpY · sin(roll)
  //   where ΔvpX = 2·tan(Δβ/2)·screenRatioX is minimised when the camera azimuth
  //   bisects the two bearings, and ΔvpY is non-zero when places differ in elevation.
  //
  //   Worst case: roll at MAX_EXPECTED_ROLL_RADIANS shrinks the ΔvpX contribution by
  //   cos(maxRoll) and adds a ΔvpY·sin(roll) cross-term. Both are absorbed by
  //   overlapGuardPx = markerWidthPx * MARKER_OVERLAP_GUARD_FRACTION:
  //
  //     Δβ ≥ 2·atan((markerWidthPx + overlapGuardPx) / (2·screenRatioX·cos(maxRoll)))
  private var markerAngularWidthDeg: Double = 0.0

  private val titleTextPaint: TextPaint by
    lazy(LazyThreadSafetyMode.NONE) {
      TextPaint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = markerTitleTextSizePx
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        isLinearText = true
        setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
      }
    }

  private val distanceTextPaint: TextPaint by
    lazy(LazyThreadSafetyMode.NONE) {
      TextPaint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = markerDistanceTextSizePx
        textAlign = Paint.Align.LEFT
        isLinearText = true
        setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
      }
    }

  private val borderPaint: Paint by
    lazy(LazyThreadSafetyMode.NONE) {
      Paint().apply {
        color = Color.WHITE
        alpha = (.1f * 255).toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
      }
    }

  private val ARMarker.rectF: RectF
    get() =
      RectF(
        x - markerWidthPx / 2,
        y - markerHeightPx / 2,
        x + markerWidthPx / 2,
        y + markerHeightPx / 2,
      )

  private fun reassignSlots(
    povLocation: Location,
    canvasWidth: Int = lastCanvasWidth,
    canvasHeight: Int = lastCanvasHeight,
  ) {
    if (pagedMarkers.isEmpty()) return
    if (canvasWidth == 0 || canvasHeight == 0) return

    // Mirrors Math3D's preDraw formula exactly: screenRatio.x = (view.width + view.height) / 2
    val screenRatioX = (canvasWidth + canvasHeight) / 2.0
    // Divide by cos(maxRoll) so the threshold holds for all device tilts up to
    // MAX_EXPECTED_ROLL_RADIANS, where cos(roll) shrinks on-screen X separation.
    val effectiveScreenRatioX = screenRatioX * cos(MAX_EXPECTED_ROLL_RADIANS)

    markerAngularWidthDeg =
      Math.toDegrees(2.0 * atan((markerWidthPx + overlapGuardPx) / (2.0 * effectiveScreenRatioX)))

    val baseY =
      context.statusBarTopInsetPx + markerHeightPx / 2f + context.cameraPreviewVerticalPaddingPx

    // Closest markers win contested slots — natural UX priority.
    val sorted = pagedMarkers.values.sortedBy { it.marker.distance }
    val assigned = mutableListOf<Pair<Float, PagedYPosition>>()

    sorted.forEach { pagedMarker ->
      val bearing = normalizeBearing(povLocation.bearingTo(pagedMarker.marker.location))
      pagedMarker.bearing = bearing

      val conflicting =
        assigned
          .filter { (b, _) -> angularDistanceDeg(bearing, b) < markerAngularWidthDeg }
          .map { (_, pos) -> pos }
          .toSet()

      val position = PagedYPosition(baseY, 0)
      var row = 0
      while (conflicting.contains(position)) {
        position.y += markerHeightPx + MARKER_VERTICAL_SPACING_PX
        ++row
        if (row >= numberOfRows) {
          row = 0
          position.y = baseY
          ++position.page
        }
      }
      pagedMarker.position = position
      assigned.add(Pair(bearing, position))
    }
  }

  internal fun draw(markers: List<ARMarker>, canvas: Canvas) {
    if (disabled) return

    if (canvas.width != lastCanvasWidth || canvas.height != lastCanvasHeight) {
      lastCanvasWidth = canvas.width
      lastCanvasHeight = canvas.height
      povLocation?.let { reassignSlots(it, canvas.width, canvas.height) }
    }

    val drawnRects = mutableListOf<RoundedRectF>()
    val renderedMarkerIds = HashSet<Long>()
    var maxPageThisFrame = 0
    var currentPageAfterScreenRotation = Int.MAX_VALUE

    markers.forEach { marker ->
      val pagedMarker = pagedMarkers[marker.place.id] ?: return@forEach
      val position = pagedMarker.position ?: return@forEach

      marker.y = position.y

      if (position.page > maxPageThisFrame) maxPageThisFrame = position.page

      if (
        firstFrame &&
          lastDrawnMarkerIds.contains(marker.place.id) &&
          position.page < currentPageAfterScreenRotation
      ) {
        currentPageAfterScreenRotation = position.page
      }

      if (position.page != currentPage) return@forEach
      if (!marker.isDrawn) return@forEach

      val markerRectF = marker.rectF
      val canvasRectF = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
      if (!RectF.intersects(canvasRectF, markerRectF)) return@forEach

      renderedMarkerIds.add(marker.place.id)

      val cornerRadiusPx = context.dpToPx(MARKER_RECT_F_CORNER_RADIUS_DP)
      canvas.drawRoundRect(markerRectF, cornerRadiusPx, cornerRadiusPx, borderPaint)
      canvas.drawTitleText(marker, markerRectF)
      canvas.drawDistanceText(marker, markerRectF)

      drawnRects.add(RoundedRectF(markerRectF, cornerRadiusPx))
    }

    maxPage = maxPageThisFrame
    if (firstFrame) {
      currentPage =
        if (currentPageAfterScreenRotation == Int.MAX_VALUE) 0 else currentPageAfterScreenRotation
    }
    if (currentPage > maxPage) currentPage = maxPage

    lastDrawnMarkerIds = renderedMarkerIds
    _markersPagingState.value = MarkersPagingState(currentPage, maxPage)
    _drawnMarkerRectFs.value = drawnRects
    firstFrame = false
  }

  internal fun onSaveInstanceState(): Bundle =
    Bundle().apply {
      putLongArray(SavedStateKeys.LAST_DRAWN_MARKER_IDS_BITS.name, lastDrawnMarkerIds.toLongArray())
    }

  internal fun onRestoreInstanceState(bundle: Bundle?) {
    lastDrawnMarkerIds =
      bundle?.getLongArray(SavedStateKeys.LAST_DRAWN_MARKER_IDS_BITS.name)?.toHashSet() ?: return
  }

  @MainThread
  internal fun setMarkers(markers: Collection<ARMarker>) {
    if (
      pagedMarkers.keys.containsAll(markers.map { it.place.id }) &&
        pagedMarkers.size == markers.size
    ) {
      return
    }
    pagedMarkers.clear()
    markers.forEach { marker -> pagedMarkers[marker.place.id] = PagedMarker(marker) }
    povLocation?.let { reassignSlots(it) }
    currentPage = 0
  }

  internal fun isOnCurrentPage(marker: ARMarker): Boolean =
    pagedMarkers[marker.place.id]?.position?.page == currentPage

  private fun angularDistanceDeg(a: Float, b: Float): Double {
    val diff = abs(a - b) % 360f
    return (if (diff > 180f) 360f - diff else diff).toDouble()
  }

  private fun normalizeBearing(bearing: Float): Float = (bearing + 360f) % 360f

  private fun Canvas.drawTitleText(marker: ARMarker, rectF: RectF) {
    drawMultilineText(
      text = marker.place.name,
      textPaint = titleTextPaint,
      width = (rectF.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX).toInt(),
      x = marker.x - markerWidthPx / 2 + markerPaddingPx,
      y = marker.y - markerHeightPx / 2 + markerPaddingPx,
      ellipsize = TextUtils.TruncateAt.END,
      maxLines = 2,
    )
  }

  private fun Canvas.drawDistanceText(marker: ARMarker, rectF: RectF) {
    val distance =
      TextUtils.ellipsize(
        marker.formattedDistance(),
        distanceTextPaint,
        rectF.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX,
        TextUtils.TruncateAt.END,
      )
    drawText(
      distance,
      0,
      distance.length,
      marker.x - markerWidthPx / 2 + markerPaddingPx,
      marker.y + markerHeightPx / 2 - markerPaddingPx,
      distanceTextPaint,
    )
  }

  private fun ARMarker.formattedDistance(): String =
    if (distance >= 1_000) "${(distance / 1_000).roundToDecimalPlaces(1)} km"
    else "${distance.roundToInt()} m"

  private class PagedMarker(
    val marker: ARMarker,
    var position: PagedYPosition? = null,
    var bearing: Float = 0f,
  ) {
    override fun equals(other: Any?): Boolean =
      this === other || (other is PagedMarker && other.marker == marker)

    override fun hashCode(): Int = Objects.hash(marker)
  }

  private data class PagedYPosition(var y: Float, var page: Int)

  private enum class SavedStateKeys {
    LAST_DRAWN_MARKER_IDS_BITS
  }

  companion object {
    private const val MARKER_VERTICAL_SPACING_PX = 50f

    // Multiplier applied to text size to get line height, matching typical
    // Android line spacing conventions.
    private const val LINE_HEIGHT_MULTIPLIER = 1.2f

    // MIN_ROWS = 1 so that in landscape (compact height) a single fully-sized row
    // is produced rather than forcing two undersized ones. MAX_ROWS guards against
    // unusually tall screens.
    private const val MIN_ROWS = 1
    private const val MAX_ROWS = 6

    // Target dp width per marker. ~160dp fits ~15 characters at 16sp comfortably.
    // At 360dp screen width: 360/160 = 2 markers across.
    // At 600dp (large phone/tablet): 600/160 = 3 markers across.
    // At 840dp (foldable): 840/160 = 5 markers across.
    private const val TARGET_MARKER_WIDTH_DP = 160f

    // Clamp bounds for the width divisor (min = never wider than half screen,
    // max = never narrower than 1/5 screen).
    private const val MIN_WIDTH_DIVISOR = 2
    private const val MAX_WIDTH_DIVISOR = 5

    // Extra width fraction added to markerWidthPx for the angular conflict threshold.
    // Absorbs: cos(roll) shrinkage, vpY·sin(roll) cross-term, and projection drift.
    // Increase toward 0.75 if residual overlaps remain; decrease toward 0.25 if too
    // many markers spill onto page 2+.
    private const val MARKER_OVERLAP_GUARD_FRACTION = 0.5f

    // Maximum device roll for which overlap-free rendering is guaranteed.
    // cos(30°) ≈ 0.866 — covers typical portrait AR use with slight tilt.
    private const val MAX_EXPECTED_ROLL_RADIANS = Math.PI / 6 // 30°

    private const val MARKER_PADDING_DP = 16f
    private const val ELLIPSIS_WIDTH_PX = 10f
    private const val MARKER_TITLE_TEXT_SIZE_SP = 16f
    private const val MARKER_DISTANCE_TEXT_SIZE_SP = 14f
    private const val MARKER_RECT_F_CORNER_RADIUS_DP = 16f
    private const val LOCATION_REASSIGN_THRESHOLD_METERS = 10f
  }
}
