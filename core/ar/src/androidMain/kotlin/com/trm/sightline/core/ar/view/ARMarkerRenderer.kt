package com.trm.sightline.core.ar.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withSave
import com.trm.sightline.core.ar.layout.MarkerLayoutCalculator
import com.trm.sightline.core.ar.layout.MarkerLayoutConfig
import com.trm.sightline.core.ar.layout.MarkerLayoutGeometry
import com.trm.sightline.core.ar.layout.MarkerSlotSession
import com.trm.sightline.core.ar.math.MarkerSlotTarget
import com.trm.sightline.core.ar.math.RoundedRectF
import com.trm.sightline.core.ar.math.markerRect
import com.trm.sightline.core.ar.model.ARMarker
import com.trm.sightline.core.ar.model.MarkersPagingState
import com.trm.sightline.core.ar.util.bottomSheetHeightPx
import com.trm.sightline.core.ar.util.cameraPreviewVerticalPaddingPx
import com.trm.sightline.core.ar.util.dpToPx
import com.trm.sightline.core.ar.util.drawMultilineText
import com.trm.sightline.core.ar.util.isCompactHeight
import com.trm.sightline.core.ar.util.navigationBarsBottomInsetPx
import com.trm.sightline.core.ar.util.screenRotation
import com.trm.sightline.core.ar.util.spToPx
import com.trm.sightline.core.ar.util.statusBarTopInsetPx
import com.trm.sightline.core.common.util.formatMarkerDistance
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.R
import com.trm.sightline.core.resources.icon
import com.trm.sightline.core.ui.sideSheetWidthDp
import java.util.Objects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ARMarkerRenderer(private val context: Context) {
  private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
  private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
  private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

  // Minimum height must accommodate: (a) one name line + distance line + padding, and
  // (b) the icon box — box height = markerHeight - 2×padding, so MIN_LEFT_BOX_SIZE_DP
  // drives a larger lower bound. maxOf() picks whichever constraint is tighter.
  private val minMarkerHeightPx: Float =
    maxOf(
      markerTitleTextSizePx * LINE_HEIGHT_MULTIPLIER +
        markerDistanceTextSizePx * LINE_HEIGHT_MULTIPLIER +
        markerPaddingPx * 2,
      context.dpToPx(MIN_LEFT_BOX_SIZE_DP) + markerPaddingPx * 2,
    )

  private val cameraViewHeightPx: Float =
    context.resources.displayMetrics.heightPixels.toFloat() -
      context.statusBarTopInsetPx -
      context.cameraPreviewVerticalPaddingPx -
      (if (context.isCompactHeight) context.navigationBarsBottomInsetPx
      else context.bottomSheetHeightPx)

  private val layoutConfig =
    MarkerLayoutConfig(
      minMarkerHeight = minMarkerHeightPx,
      maxMarkerHeight = maxOf(cameraViewHeightPx, minMarkerHeightPx),
      minMarkerWidth = 0f,
      maxMarkerWidth = context.resources.displayMetrics.widthPixels.toFloat(),
      targetMarkerWidth = context.dpToPx(TARGET_MARKER_WIDTH_DP),
      minWidthDivisor = MIN_WIDTH_DIVISOR,
      maxWidthDivisor = MAX_WIDTH_DIVISOR,
      minRows = MIN_ROWS,
      maxRows = MAX_ROWS,
      verticalSpacing = MARKER_VERTICAL_SPACING_PX,
      overlapGuardFraction = MARKER_OVERLAP_GUARD_FRACTION,
      topPadding = context.cameraPreviewVerticalPaddingPx,
    )

  private val layoutGeometry: MarkerLayoutGeometry =
    MarkerLayoutCalculator.calculate(
      config = layoutConfig,
      usableWidth = context.resources.displayMetrics.widthPixels.toFloat(),
      usableHeight = cameraViewHeightPx,
      topInset = context.statusBarTopInsetPx,
    )

  internal val markerHeightPx: Float = layoutGeometry.markerHeight
  internal val markerWidthPx: Float = layoutGeometry.markerWidth

  private val slotSession = MarkerSlotSession()

  var povLocation: GeoLocation? = null
    @MainThread
    set(value) {
      field = value
      value?.let { reassignSlots(it) }
    }

  var currentPage: Int
    get() = slotSession.currentPage
    @MainThread
    set(value) {
      assert(value >= 0)
      slotSession.selectPage(value)
    }

  private var lastDrawnMarkerIds = HashSet<String>()

  private val _markersPagingState = MutableStateFlow(MarkersPagingState(currentPage, 0))
  val markersPagingState: StateFlow<MarkersPagingState> = _markersPagingState.asStateFlow()

  private val _drawnMarkerRectFs = MutableStateFlow<List<RoundedRectF>>(emptyList())
  val drawnMarkerRectFs: StateFlow<List<RoundedRectF>> = _drawnMarkerRectFs.asStateFlow()

  var disabled: Boolean = false
    @MainThread set

  // Per-category drawable cache: each PlaceCategory's vector is inflated once and reused, so
  // per-frame drawing only sets bounds and tint.
  private val iconDrawableCache = HashMap<PlaceCategory, VectorDrawable>()

  private fun iconDrawableFor(category: PlaceCategory): VectorDrawable? =
    iconDrawableCache.getOrPut(category) {
      ResourcesCompat.getDrawable(context.resources, category.icon.drawableResId, context.theme)
        as? VectorDrawable ?: return null
    }

  private val pagedMarkers = HashMap<String, PagedMarker>()

  private var lastCanvasWidth: Int = 0
  private var lastCanvasHeight: Int = 0

  // ── Paints ─────────────────────────────────────────────────────────────────

  // Primary text: place name
  private val titleTextPaint: TextPaint by
    lazy(LazyThreadSafetyMode.NONE) {
      TextPaint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = markerTitleTextSizePx
        textAlign = Paint.Align.LEFT
        typeface =
          try {
            Typeface.create(ResourcesCompat.getFont(context, R.font.inter), Typeface.BOLD)
          } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
          }
        isLinearText = true

        setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
      }
    }

  // Secondary text: distance, shown below the name line
  private val distanceLabelPaint: TextPaint by
    lazy(LazyThreadSafetyMode.NONE) {
      TextPaint().apply {
        color = Color.WHITE
        alpha = (0.75f * 255).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = markerDistanceTextSizePx
        textAlign = Paint.Align.LEFT
        typeface =
          try {
            ResourcesCompat.getFont(context, R.font.roboto)
          } catch (_: Exception) {
            Typeface.DEFAULT
          }
        isLinearText = true

        setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
      }
    }

  // Fill for the left icon box — mirrors Material surfaceVariant at ~18 % opacity.
  private val surfaceBackgroundPaint: Paint by
    lazy(LazyThreadSafetyMode.NONE) {
      Paint().apply {
        color = Color.WHITE
        alpha = (0.18f * 255).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
      }
    }

  // Solid white fill for all icon paths; tint is applied uniformly to match the
  // "onSurface" role used by the Compose icon in PlaceCategoryHeader.
  private val iconPaint: Paint by
    lazy(LazyThreadSafetyMode.NONE) {
      Paint().apply {
        color = Color.WHITE
        alpha = (0.90f * 255).toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
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
    get() {
      val bounds =
        markerRect(
          centerX = x,
          centerY = y,
          widthPx = markerWidthPx,
          heightPx = markerHeightPx,
          cornerRadiusPx = 0f,
        )
      return RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

  private fun reassignSlots(
    povLocation: GeoLocation,
    canvasWidth: Int = lastCanvasWidth,
    canvasHeight: Int = lastCanvasHeight,
    previouslyVisibleIds: Set<String> = lastDrawnMarkerIds,
  ) {
    val update =
      slotSession.update(
        povLocation = povLocation,
        targets =
          pagedMarkers.values.map { pagedMarker ->
            MarkerSlotTarget(
              id = pagedMarker.marker.place.id,
              location = pagedMarker.marker.location,
              distance = pagedMarker.marker.distance,
            )
          },
        geometry = layoutGeometry,
        screenRotation = context.screenRotation,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        previouslyVisibleIds = previouslyVisibleIds,
      )

    update.slots.forEach { (id, slot) ->
      pagedMarkers[id]?.apply {
        bearing = slot.bearing
        position = PagedYPosition(slot.y, slot.page)
      }
    }
  }

  internal fun draw(markers: List<ARMarker>, canvas: Canvas) {
    if (disabled) return

    if (canvas.width != lastCanvasWidth || canvas.height != lastCanvasHeight) {
      lastCanvasWidth = canvas.width
      lastCanvasHeight = canvas.height
      povLocation?.let { reassignSlots(it, canvas.width, canvas.height) }
    }

    val drawnRectFs = mutableListOf<RoundedRectF>()
    val renderedMarkerIds = HashSet<String>()

    // Clip markers to everything left of the side sheet
    val markerClipRight =
      if (context.isCompactHeight) canvas.width - context.dpToPx(sideSheetWidthDp.toFloat())
      else canvas.width.toFloat()

    markers.forEach { marker ->
      val pagedMarker = pagedMarkers[marker.place.id] ?: return@forEach
      val position = pagedMarker.position ?: return@forEach

      marker.y = position.y

      if (position.page != currentPage) return@forEach
      if (!marker.isDrawn) return@forEach

      val markerRectF = marker.rectF
      val canvasRectF = RectF(0f, 0f, markerClipRight, canvas.height.toFloat())
      if (!RectF.intersects(canvasRectF, markerRectF)) return@forEach

      renderedMarkerIds.add(marker.place.id)

      val cornerRadiusPx = context.dpToPx(MARKER_RECT_F_CORNER_RADIUS_DP)

      canvas.withSave {
        clipRect(0f, 0f, markerClipRight, canvas.height.toFloat())
        drawRoundRect(markerRectF, cornerRadiusPx, cornerRadiusPx, borderPaint)
        drawMarkerContent(marker, markerRectF)
      }

      drawnRectFs.add(
        RoundedRectF(
          left = markerRectF.left,
          top = markerRectF.top,
          right = markerRectF.right,
          bottom = markerRectF.bottom,
          cornerRadius = cornerRadiusPx,
        ),
      )
    }

    lastDrawnMarkerIds = renderedMarkerIds
    _markersPagingState.value =
      MarkersPagingState(slotSession.currentPage, slotSession.totalPages - 1)
    _drawnMarkerRectFs.value = drawnRectFs
  }

  // ── Marker content drawing ─────────────────────────────────────────────────

  /**
   * Draws a [PlaceListItem]-style layout:
   * ```
   * ┌──────────────────────────────────────────────┐
   * │  ┌──────────┐  Place Name                    │
   * │  │  [icon]  │  350 m                         │
   * │  └──────────┘                                │
   * └──────────────────────────────────────────────┘
   * ```
   *
   * Left box: category vector drawable icon. Right column: name (up to 2 lines), then distance
   * (shown when vertical space permits).
   */
  private fun Canvas.drawMarkerContent(marker: ARMarker, rectF: RectF) {
    val cornerRadiusPx = context.dpToPx(MARKER_RECT_F_CORNER_RADIUS_DP)

    // ── Left icon box ──────────────────────────────────────────────────────
    // Square, vertically centred. Size = inner marker height, clamped so the
    // icon stays legible even on the shortest markers.
    val leftBoxSize =
      (markerHeightPx - markerPaddingPx * 2).coerceAtLeast(context.dpToPx(MIN_LEFT_BOX_SIZE_DP))
    val leftBoxLeft = rectF.left + markerPaddingPx
    val leftBoxTop = rectF.centerY() - leftBoxSize / 2f
    val leftBoxRectF =
      RectF(leftBoxLeft, leftBoxTop, leftBoxLeft + leftBoxSize, leftBoxTop + leftBoxSize)

    drawRoundRect(leftBoxRectF, cornerRadiusPx, cornerRadiusPx, surfaceBackgroundPaint)

    // Draw the vector icon for this marker's own category, scaled to ICON_BOX_FILL_FRACTION of
    // the box. Each category's drawable is inflated once and reused.
    val iconDrawable = iconDrawableFor(marker.place.category)
    if (iconDrawable != null) {
      val iconDisplaySize = leftBoxSize * ICON_BOX_FILL_FRACTION
      val iconScale = iconDisplaySize / ICON_VIEWPORT_SIZE
      val iconLeft = leftBoxRectF.left + (leftBoxSize - iconDisplaySize) / 2f
      val iconTop = leftBoxRectF.top + (leftBoxSize - iconDisplaySize) / 2f
      withSave {
        translate(iconLeft, iconTop)
        scale(iconScale, iconScale)
        iconDrawable.setTint(iconPaint.color)
        iconDrawable.alpha = iconPaint.alpha
        iconDrawable.setBounds(0, 0, ICON_VIEWPORT_SIZE.toInt(), ICON_VIEWPORT_SIZE.toInt())
        iconDrawable.draw(this)
      }
    }

    // ── Right text column ──────────────────────────────────────────────────
    val textLeft = leftBoxRectF.right + markerPaddingPx / 2f
    val textRight = rectF.right - markerPaddingPx
    val textWidth = (textRight - textLeft).coerceAtLeast(0f)
    val textEllipsisWidth = (textWidth - ELLIPSIS_WIDTH_PX).coerceAtLeast(0f)
    val textAreaTop = rectF.top + markerPaddingPx
    val textAreaBottom = rectF.bottom - markerPaddingPx
    val textAreaHeight = textAreaBottom - textAreaTop

    val nameLineH = markerTitleTextSizePx * LINE_HEIGHT_MULTIPLIER
    val distLineH = markerDistanceTextSizePx * LINE_HEIGHT_MULTIPLIER

    val displayName = marker.place.name

    // Allow 2 name lines if text wraps and the height budget allows it.
    val nameWraps = titleTextPaint.measureText(displayName) > textWidth
    val maxNameLines = if (nameWraps && nameLineH * 2 <= textAreaHeight) 2 else 1
    drawMultilineText(
      text = displayName,
      textPaint = titleTextPaint,
      width = textEllipsisWidth.toInt().coerceAtLeast(1),
      x = textLeft,
      y = textAreaTop,
      ellipsize = TextUtils.TruncateAt.END,
      maxLines = maxNameLines,
    )
    val consumedHeight = nameLineH * maxNameLines

    // Distance — drawn below the name when vertical space remains.
    // NAME_DISTANCE_GAP_DP adds a small breathing gap so the two labels don't feel cramped.
    val nameDistanceGapPx = context.dpToPx(NAME_DISTANCE_GAP_DP)
    if (consumedHeight + nameDistanceGapPx + distLineH <= textAreaHeight) {
      val ellipsized =
        TextUtils.ellipsize(
          formatMarkerDistance(meters = marker.distance),
          distanceLabelPaint,
          textEllipsisWidth,
          TextUtils.TruncateAt.END,
        )
      // Convert top-of-line to Canvas baseline (fontMetrics.ascent is negative).
      val baseline =
        textAreaTop + consumedHeight + nameDistanceGapPx - distanceLabelPaint.fontMetrics.ascent
      drawText(ellipsized, 0, ellipsized.length, textLeft, baseline, distanceLabelPaint)
    }
  }

  // ── Instance state ──────────────────────────────────────────────────────────

  internal fun onSaveInstanceState(): Bundle =
    Bundle().apply {
      putStringArray(SavedStateKeys.LAST_DRAWN_MARKER_IDS.name, lastDrawnMarkerIds.toTypedArray())
    }

  internal fun onRestoreInstanceState(bundle: Bundle?) {
    lastDrawnMarkerIds =
      bundle?.getStringArray(SavedStateKeys.LAST_DRAWN_MARKER_IDS.name)?.toHashSet() ?: return
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
    povLocation?.let { reassignSlots(it, previouslyVisibleIds = emptySet()) }
    slotSession.selectPage(0)
  }

  internal fun isOnCurrentPage(marker: ARMarker): Boolean =
    pagedMarkers[marker.place.id]?.position?.page == currentPage

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
    LAST_DRAWN_MARKER_IDS,
  }

  companion object {
    private const val MARKER_VERTICAL_SPACING_PX = 50f
    private const val LINE_HEIGHT_MULTIPLIER = 1.1f

    private const val MIN_ROWS = 1
    private const val MAX_ROWS = 6

    private const val TARGET_MARKER_WIDTH_DP = 160f
    private const val MIN_WIDTH_DIVISOR = 2
    private const val MAX_WIDTH_DIVISOR = 5

    private const val MARKER_OVERLAP_GUARD_FRACTION = 0.5f

    private const val MARKER_PADDING_DP = 12f
    private const val ELLIPSIS_WIDTH_PX = 10f

    // Extra vertical gap inserted between the name block and the distance label.
    // Keeps the two from feeling cramped without pushing distance out of short markers.
    private const val NAME_DISTANCE_GAP_DP = 8f

    private const val MARKER_TITLE_TEXT_SIZE_SP = 14f
    private const val MARKER_DISTANCE_TEXT_SIZE_SP = 12f

    private const val MARKER_RECT_F_CORNER_RADIUS_DP = 16f

    // Left icon box must be at least this tall so the icon is recognizable.
    // Mirrors the 64dp Surface in PlaceListItem, scaled down for the AR context.
    private const val MIN_LEFT_BOX_SIZE_DP = 48f

    // Fraction of the box side that the icon fills, leaving even padding on all sides.
    private const val ICON_BOX_FILL_FRACTION = 0.60f

    // Every shared category vector declares this viewport; bounds are set to it and the canvas
    // applies the display scale.
    private const val ICON_VIEWPORT_SIZE = 24f
  }
}
