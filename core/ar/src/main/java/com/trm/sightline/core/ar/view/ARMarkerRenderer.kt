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
import com.trm.sightline.core.ar.util.isCompactWidth
import com.trm.sightline.core.ar.util.navigationBarsBottomInsetPx
import com.trm.sightline.core.ar.util.preciseFormattedDistance
import com.trm.sightline.core.ar.util.spToPx
import com.trm.sightline.core.ar.util.statusBarTopInsetPx
import java.util.Objects
import java.util.TreeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ARMarkerRenderer(private val context: Context) {
  private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
  private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
  private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

  internal val markerHeightPx: Float
  internal val markerWidthPx: Float

  init {
    val displayMetrics = context.resources.displayMetrics
    val cameraViewHeight =
      displayMetrics.heightPixels -
        context.statusBarTopInsetPx -
        context.cameraPreviewVerticalPaddingPx -
        if (context.isCompactHeight) context.navigationBarsBottomInsetPx
        else context.bottomSheetHeightPx
    markerHeightPx = cameraViewHeight / numberOfRows - MARKER_VERTICAL_SPACING_PX
    val markerWidthDivisor =
      if (context.isCompactWidth) MARKER_WIDTH_DIVISOR_COMPACT_WIDTH
      else MARKER_WIDTH_DIVISOR_NON_COMPACT_WIDTH
    markerWidthPx = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
  }

  var povLocation: Location? = null
    @MainThread set

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
  private val pagedMarkerPositions = TreeMap<Float, MutableSet<PagedYPosition>>()

  private val numberOfRows: Int
    get() =
      if (context.isCompactHeight) NUMBER_OF_ROWS_COMPACT_HEIGHT
      else NUMBER_OF_ROWS_NON_COMPACT_HEIGHT

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

  internal fun draw(markers: List<ARMarker>, canvas: Canvas) {
    if (disabled) return

    pagedMarkerPositions.clear()
    val drawnRects = mutableListOf<RoundedRectF>()
    val drawnMarkerIds = HashSet<Long>()
    var maxPageThisFrame = 0
    var currentPageAfterScreenRotation = Int.MAX_VALUE

    fun drawMarker(marker: ARMarker, lastDrawn: Boolean) {
      val pagedMarker = pagedMarkers[marker.place.id] ?: return

      val pagedPosition =
        pagedPositionOf(
          pagedMarker = pagedMarker,
          requireAlreadyCalculated = lastDrawn && !firstFrame,
        )
      marker.y = pagedPosition.y
      storeMarkerPosition(pagedMarker)
      pagedMarker.position?.page?.let { if (it > maxPageThisFrame) maxPageThisFrame = it }
      if (
        firstFrame &&
          lastDrawnMarkerIds.contains(marker.place.id) &&
          pagedPosition.page < currentPageAfterScreenRotation
      ) {
        currentPageAfterScreenRotation = pagedPosition.page
      }
      if (pagedMarker.position?.page != currentPage) return

      drawnMarkerIds.add(marker.place.id)

      val markerRectF = marker.rectF
      val canvasRectF = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
      if (!RectF.intersects(canvasRectF, markerRectF)) return

      val cornerRadiusPx = context.dpToPx(MARKER_RECT_F_CORNER_RADIUS_DP)
      canvas.drawRoundRect(markerRectF, cornerRadiusPx, cornerRadiusPx, borderPaint)
      canvas.drawTitleText(marker, markerRectF)
      canvas.drawDistanceText(marker, markerRectF)

      drawnRects.add(RoundedRectF(markerRectF, cornerRadiusPx))
    }

    val (lastDrawnMarkers, newlyAppearedMarkers) =
      markers.partition {
        lastDrawnMarkerIds.contains(it.place.id) && pagedMarkers[it.place.id]?.position != null
      }
    lastDrawnMarkers.forEach { drawMarker(it, lastDrawn = true) }
    newlyAppearedMarkers.forEach { drawMarker(it, lastDrawn = false) }

    maxPage = maxPageThisFrame
    if (firstFrame) currentPage = currentPageAfterScreenRotation
    if (currentPage > maxPage) currentPage = maxPage

    lastDrawnMarkerIds = drawnMarkerIds
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
    currentPage = 0
  }

  internal fun isOnCurrentPage(marker: ARMarker): Boolean =
    pagedMarkers[marker.place.id]?.position?.page == currentPage

  private fun pagedPositionOf(
    pagedMarker: PagedMarker,
    requireAlreadyCalculated: Boolean,
  ): PagedYPosition {
    val takenPositions =
      pagedMarkerPositions
        .subMap(pagedMarker.marker.x - markerWidthPx, pagedMarker.marker.x + markerWidthPx)
        .values
        .flatten()
        .toSet()
    if (requireAlreadyCalculated) {
      val existing =
        checkNotNull(pagedMarker.position) { "Last drawn marker's paged position is null." }
      if (!takenPositions.contains(existing)) return existing
    } else {
      pagedMarker.position?.let { if (!takenPositions.contains(it)) return it }
    }

    val baseY =
      context.statusBarTopInsetPx + markerHeightPx / 2f + context.cameraPreviewVerticalPaddingPx
    val position = PagedYPosition(baseY, 0)
    var row = 0
    while (takenPositions.contains(position)) {
      position.y += markerHeightPx + MARKER_VERTICAL_SPACING_PX
      ++row
      if (row >= numberOfRows) {
        row = 0
        position.y = baseY
        ++position.page
      }
    }
    pagedMarker.position = position
    return position
  }

  private fun storeMarkerPosition(marker: PagedMarker) {
    val pagedPosition = checkNotNull(marker.position) { "Marker must have a PagedPosition." }
    val existingMarkerSet = pagedMarkerPositions[marker.marker.x]
    existingMarkerSet?.add(pagedPosition)
      ?: run { pagedMarkerPositions[marker.marker.x] = mutableSetOf(pagedPosition) }
  }

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
        marker.distance.preciseFormattedDistance,
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

  private class PagedMarker(val marker: ARMarker, var position: PagedYPosition? = null) {
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
    private const val NUMBER_OF_ROWS_NON_COMPACT_HEIGHT = 5
    private const val NUMBER_OF_ROWS_COMPACT_HEIGHT = 2
    private const val MARKER_WIDTH_DIVISOR_COMPACT_WIDTH = 2
    private const val MARKER_WIDTH_DIVISOR_NON_COMPACT_WIDTH = 4
    private const val MARKER_PADDING_DP = 16f
    private const val ELLIPSIS_WIDTH_PX = 10f
    private const val MARKER_TITLE_TEXT_SIZE_SP = 16f
    private const val MARKER_DISTANCE_TEXT_SIZE_SP = 14f
    private const val MARKER_RECT_F_CORNER_RADIUS_DP = 16f
  }
}
