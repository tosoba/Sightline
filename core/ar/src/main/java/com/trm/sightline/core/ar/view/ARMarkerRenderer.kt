package com.trm.sightline.core.ar.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.core.graphics.withSave
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
import com.trm.sightline.core.common.util.tourismOrLeisure
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.icon
import java.util.Objects
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ARMarkerRenderer(private val context: Context) {
  private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
  private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
  private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

  internal val markerHeightPx: Float
  internal val markerWidthPx: Float

  private val overlapGuardPx: Float
  private val cameraViewHeightPx: Float

  // Minimum height must accommodate: (a) one name line + distance line + padding, and
  // (b) the icon box — box height = markerHeight - 2×padding, so MIN_LEFT_BOX_SIZE_DP
  // drives a larger lower bound. maxOf() picks whichever constraint is tighter.
  private val minMarkerHeightPx: Float
    get() {
      val nameLineH = markerTitleTextSizePx * LINE_HEIGHT_MULTIPLIER
      val distLineH = markerDistanceTextSizePx * LINE_HEIGHT_MULTIPLIER
      val minBoxPx = context.dpToPx(MIN_LEFT_BOX_SIZE_DP)
      return maxOf(nameLineH + distLineH + markerPaddingPx * 2, minBoxPx + markerPaddingPx * 2)
    }

  private val numberOfRows: Int
    get() =
      (cameraViewHeightPx / (minMarkerHeightPx + MARKER_VERTICAL_SPACING_PX))
        .toInt()
        .coerceIn(MIN_ROWS, MAX_ROWS)

  private val markerWidthDivisor: Int
    get() {
      val screenWidthDp = context.resources.displayMetrics.run { widthPixels / density }
      return (screenWidthDp / TARGET_MARKER_WIDTH_DP)
        .toInt()
        .coerceIn(MIN_WIDTH_DIVISOR, MAX_WIDTH_DIVISOR)
    }

  init {
    val displayMetrics = context.resources.displayMetrics
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

  // Per-category path cache: each PlaceCategory's ImageVector is baked exactly once
  // (group transforms folded in via Matrix) and stored here so per-frame drawing is
  // allocation-free regardless of how many different categories appear on screen.
  private val iconPathCache = HashMap<PlaceCategory, BakedIcon>()

  /**
   * Returns the [BakedIcon] for [category], baking it on first access. All Material Icons share a
   * 24x24 viewport and no top-level rotation, so the fast path (cache hit) dominates after the
   * first frame per category.
   */
  private fun bakedIconFor(category: PlaceCategory): BakedIcon =
    iconPathCache.getOrPut(category) {
      val vector = category.icon
      BakedIcon(viewportWidth = vector.viewportWidth, paths = vector.bakeToAndroidPaths())
    }

  private val pagedMarkers = HashMap<Long, PagedMarker>()

  private var lastCanvasWidth: Int = 0
  private var lastCanvasHeight: Int = 0

  private var markerAngularWidthDeg: Double = 0.0

  // ── Paints ─────────────────────────────────────────────────────────────────

  // Primary text: joined "Place Name · tourism type"
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

    val screenRatioX = (canvasWidth + canvasHeight) / 2.0
    val effectiveScreenRatioX = screenRatioX * cos(MAX_EXPECTED_ROLL_RADIANS)

    markerAngularWidthDeg =
      Math.toDegrees(2.0 * atan((markerWidthPx + overlapGuardPx) / (2.0 * effectiveScreenRatioX)))

    val baseY =
      context.statusBarTopInsetPx + markerHeightPx / 2f + context.cameraPreviewVerticalPaddingPx

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
      canvas.drawMarkerContent(marker, markerRectF)

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

  // ── Marker content drawing ─────────────────────────────────────────────────

  /**
   * Draws a [PlaceListItem]-style layout:
   * ```
   * ┌──────────────────────────────────────────────┐
   * │  ┌──────────┐  Place Name · restaurant       │
   * │  │  [icon]  │  350 m                         │
   * │  └──────────┘                                │
   * └──────────────────────────────────────────────┘
   * ```
   *
   * Left box: category [ImageVector] icon rendered from pre-baked [android.graphics.Path]s. Right
   * column: name joined with tourism/leisure type (highest priority, up to 2 lines), then distance
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

    // Draw the pre-baked icon paths for this marker's own category, scaled to
    // ICON_BOX_FILL_FRACTION of the box. Each category is baked once on first use
    // and retrieved from iconPathCache on subsequent frames.
    val baked = bakedIconFor(marker.place.category)
    if (baked.paths.isNotEmpty()) {
      val iconDisplaySize = leftBoxSize * ICON_BOX_FILL_FRACTION
      val iconScale = iconDisplaySize / baked.viewportWidth.coerceAtLeast(1f)
      val iconLeft = leftBoxRectF.left + (leftBoxSize - iconDisplaySize) / 2f
      val iconTop = leftBoxRectF.top + (leftBoxSize - iconDisplaySize) / 2f
      withSave {
        translate(iconLeft, iconTop)
        scale(iconScale, iconScale)
        baked.paths.forEach { path -> drawPath(path, iconPaint) }
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

    // Name joined with tourism/leisure — e.g. "Le Bistro · restaurant"
    val displayName = buildString {
      append(marker.place.name)
      marker.place.tourismOrLeisure?.let { append(" · $it") }
    }

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
    if (consumedHeight + distLineH <= textAreaHeight) {
      val ellipsized =
        TextUtils.ellipsize(
          marker.formattedDistance(),
          distanceLabelPaint,
          textEllipsisWidth,
          TextUtils.TruncateAt.END,
        )
      // Convert top-of-line to Canvas baseline (fontMetrics.ascent is negative).
      val baseline = textAreaTop + consumedHeight - distanceLabelPaint.fontMetrics.ascent
      drawText(ellipsized, 0, ellipsized.length, textLeft, baseline, distanceLabelPaint)
    }
  }

  // ── ImageVector → Android Path baking ─────────────────────────────────────

  /**
   * Converts an [ImageVector] to a list of [android.graphics.Path]s in the icon's viewport
   * coordinate space. Group transforms (rotation, scale, translation) are folded into each path via
   * [Matrix] so that drawing only needs a single canvas-level scale/translate per frame.
   */
  private fun ImageVector.bakeToAndroidPaths(): List<Path> {
    val result = mutableListOf<Path>()
    bakeVectorNode(root, Matrix(), result)
    return result
  }

  private fun bakeVectorNode(node: VectorNode, parentMatrix: Matrix, result: MutableList<Path>) {
    when (node) {
      is VectorGroup -> {
        val m = Matrix(parentMatrix)
        if (node.rotation != 0f) m.preRotate(node.rotation, node.pivotX, node.pivotY)
        if (node.scaleX != 1f || node.scaleY != 1f)
          m.preScale(node.scaleX, node.scaleY, node.pivotX, node.pivotY)
        if (node.translationX != 0f || node.translationY != 0f)
          m.preTranslate(node.translationX, node.translationY)
        node.forEach { child -> bakeVectorNode(child, m, result) }
      }
      is VectorPath -> {
        val path = node.pathData.toAndroidPath()
        path.fillType =
          if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD
          else Path.FillType.WINDING
        if (!parentMatrix.isIdentity) path.transform(parentMatrix)
        result.add(path)
      }
    }
  }

  /**
   * Converts Compose [PathNode] path data to an [android.graphics.Path].
   *
   * All 19 SVG path command variants are handled. For SVG arc commands, the standard endpoint →
   * center parameterisation (SVG spec §B.2.4) is used to produce an Android `arcTo` call. Rotated
   * arcs (xRotation ≠ 0) fall back to a straight line; Material Icons do not use them.
   *
   * Reflective curve/quad commands track the previous control point so that the reflection is
   * geometrically correct, not just an approximation.
   */
  private fun List<PathNode>.toAndroidPath(): Path {
    val path = Path()
    var cx = 0f
    var cy = 0f // current point
    var mx = 0f
    var my = 0f // last moveTo (for Close)
    var lastCubicCx = Float.NaN
    var lastCubicCy = Float.NaN
    var lastQuadCx = Float.NaN
    var lastQuadCy = Float.NaN

    fun clearCubic() {
      lastCubicCx = Float.NaN
    }
    fun clearQuad() {
      lastQuadCx = Float.NaN
    }

    for (node in this) {
      when (node) {
        is PathNode.MoveTo -> {
          path.moveTo(node.x, node.y)
          cx = node.x
          cy = node.y
          mx = cx
          my = cy
          clearCubic()
          clearQuad()
        }
        is PathNode.RelativeMoveTo -> {
          cx += node.dx
          cy += node.dy
          path.moveTo(cx, cy)
          mx = cx
          my = cy
          clearCubic()
          clearQuad()
        }
        is PathNode.LineTo -> {
          path.lineTo(node.x, node.y)
          cx = node.x
          cy = node.y
          clearCubic()
          clearQuad()
        }
        is PathNode.RelativeLineTo -> {
          cx += node.dx
          cy += node.dy
          path.lineTo(cx, cy)
          clearCubic()
          clearQuad()
        }
        is PathNode.HorizontalTo -> {
          path.lineTo(node.x, cy)
          cx = node.x
          clearCubic()
          clearQuad()
        }
        is PathNode.RelativeHorizontalTo -> {
          cx += node.dx
          path.lineTo(cx, cy)
          clearCubic()
          clearQuad()
        }
        is PathNode.VerticalTo -> {
          path.lineTo(cx, node.y)
          cy = node.y
          clearCubic()
          clearQuad()
        }
        is PathNode.RelativeVerticalTo -> {
          cy += node.dy
          path.lineTo(cx, cy)
          clearCubic()
          clearQuad()
        }
        is PathNode.CurveTo -> {
          path.cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
          lastCubicCx = node.x2
          lastCubicCy = node.y2
          cx = node.x3
          cy = node.y3
          clearQuad()
        }
        is PathNode.RelativeCurveTo -> {
          path.rCubicTo(node.dx1, node.dy1, node.dx2, node.dy2, node.dx3, node.dy3)
          lastCubicCx = cx + node.dx2
          lastCubicCy = cy + node.dy2
          cx += node.dx3
          cy += node.dy3
          clearQuad()
        }
        is PathNode.ReflectiveCurveTo -> {
          // Reflect ctrl2 of the previous cubic across the current point.
          val c1x = if (lastCubicCx.isNaN()) cx else 2f * cx - lastCubicCx
          val c1y = if (lastCubicCy.isNaN()) cy else 2f * cy - lastCubicCy
          path.cubicTo(c1x, c1y, node.x1, node.y1, node.x2, node.y2)
          lastCubicCx = node.x1
          lastCubicCy = node.y1
          cx = node.x2
          cy = node.y2
          clearQuad()
        }
        is PathNode.RelativeReflectiveCurveTo -> {
          val c1x = if (lastCubicCx.isNaN()) 0f else cx - lastCubicCx
          val c1y = if (lastCubicCy.isNaN()) 0f else cy - lastCubicCy
          path.rCubicTo(c1x, c1y, node.dx1, node.dy1, node.dx2, node.dy2)
          lastCubicCx = cx + node.dx1
          lastCubicCy = cy + node.dy1
          cx += node.dx2
          cy += node.dy2
          clearQuad()
        }
        is PathNode.QuadTo -> {
          path.quadTo(node.x1, node.y1, node.x2, node.y2)
          lastQuadCx = node.x1
          lastQuadCy = node.y1
          cx = node.x2
          cy = node.y2
          clearCubic()
        }
        is PathNode.RelativeQuadTo -> {
          path.rQuadTo(node.dx1, node.dy1, node.dx2, node.dy2)
          lastQuadCx = cx + node.dx1
          lastQuadCy = cy + node.dy1
          cx += node.dx2
          cy += node.dy2
          clearCubic()
        }
        is PathNode.ReflectiveQuadTo -> {
          val c1x = if (lastQuadCx.isNaN()) cx else 2f * cx - lastQuadCx
          val c1y = if (lastQuadCy.isNaN()) cy else 2f * cy - lastQuadCy
          path.quadTo(c1x, c1y, node.x, node.y)
          lastQuadCx = c1x
          lastQuadCy = c1y
          cx = node.x
          cy = node.y
          clearCubic()
        }
        is PathNode.RelativeReflectiveQuadTo -> {
          val c1x = if (lastQuadCx.isNaN()) 0f else cx - lastQuadCx
          val c1y = if (lastQuadCy.isNaN()) 0f else cy - lastQuadCy
          path.rQuadTo(c1x, c1y, node.dx, node.dy)
          lastQuadCx = cx + c1x
          lastQuadCy = cy + c1y
          cx += node.dx
          cy += node.dy
          clearCubic()
        }
        is PathNode.ArcTo -> {
          svgArcTo(
            path,
            cx,
            cy,
            node.horizontalEllipseRadius,
            node.verticalEllipseRadius,
            node.theta,
            node.isMoreThanHalf,
            node.isPositiveArc,
            node.arcStartX,
            node.arcStartY,
          )
          cx = node.arcStartX
          cy = node.arcStartY
          clearCubic()
          clearQuad()
        }
        is PathNode.RelativeArcTo -> {
          val endX = cx + node.arcStartDx
          val endY = cy + node.arcStartDy
          svgArcTo(
            path,
            cx,
            cy,
            node.horizontalEllipseRadius,
            node.verticalEllipseRadius,
            node.theta,
            node.isMoreThanHalf,
            node.isPositiveArc,
            endX,
            endY,
          )
          cx = endX
          cy = endY
          clearCubic()
          clearQuad()
        }
        PathNode.Close -> {
          path.close()
          cx = mx
          cy = my
          clearCubic()
          clearQuad()
        }
      }
    }
    return path
  }

  /**
   * Converts an SVG arc (endpoint parameterisation) to an Android [android.graphics.Path.arcTo]
   * call using the standard centre-parameterisation derivation from SVG spec §B.2.4.
   *
   * Rotated arcs (xRotation ≠ 0) are extremely rare in Material Icons and fall back to a straight
   * line — implementing canvas-rotation for baked paths would require decomposing into cubic
   * Béziers, which is unnecessary complexity for this use case.
   */
  private fun svgArcTo(
    path: Path,
    x0: Float,
    y0: Float,
    rx: Float,
    ry: Float,
    xRotation: Float,
    largeArc: Boolean,
    sweep: Boolean,
    x1: Float,
    y1: Float,
  ) {
    if (x0 == x1 && y0 == y1) return
    if (rx == 0f || ry == 0f) {
      path.lineTo(x1, y1)
      return
    }
    if (xRotation != 0f) {
      path.lineTo(x1, y1)
      return
    } // see kdoc above

    val dx = (x0 - x1) / 2.0
    val dy = (y0 - y1) / 2.0
    // (x1', y1') in rotated frame — with xRotation=0 the frame is unchanged
    val x1p = dx
    val y1p = dy
    val x1pSq = x1p * x1p
    val y1pSq = y1p * y1p

    var rxD = abs(rx.toDouble())
    var ryD = abs(ry.toDouble())
    // Scale up radii if they are too small
    val lambda = x1pSq / (rxD * rxD) + y1pSq / (ryD * ryD)
    if (lambda > 1.0) {
      val s = sqrt(lambda)
      rxD *= s
      ryD *= s
    }
    val rxSq = rxD * rxD
    val rySq = ryD * ryD

    // Centre in the rotated frame (cx', cy')
    val num = rxSq * rySq - rxSq * y1pSq - rySq * x1pSq
    val den = rxSq * y1pSq + rySq * x1pSq
    val sq = if (den == 0.0) 0.0 else sqrt(maxOf(0.0, num / den))
    val sign = if (largeArc == sweep) -1.0 else 1.0
    val cxp = sign * sq * rxD * y1p / ryD
    val cyp = sign * sq * -ryD * x1p / rxD

    // Centre in user space (xRotation = 0 → no rotation matrix)
    val cx = cxp + (x0 + x1) / 2.0
    val cy = cyp + (y0 + y1) / 2.0

    // Angles
    val startAngle = svgAngle(1.0, 0.0, (x1p - cxp) / rxD, (y1p - cyp) / ryD)
    var sweepAngle =
      svgAngle((x1p - cxp) / rxD, (y1p - cyp) / ryD, (-x1p - cxp) / rxD, (-y1p - cyp) / ryD)
    if (!sweep && sweepAngle > 0) sweepAngle -= 360.0
    if (sweep && sweepAngle < 0) sweepAngle += 360.0

    val oval =
      RectF((cx - rxD).toFloat(), (cy - ryD).toFloat(), (cx + rxD).toFloat(), (cy + ryD).toFloat())
    path.arcTo(oval, startAngle.toFloat(), sweepAngle.toFloat())
  }

  /** Signed angle (degrees) from vector (ux,uy) to vector (vx,vy). */
  private fun svgAngle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
    val dot = ux * vx + uy * vy
    val len = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
    val angle = Math.toDegrees(acos((dot / len).coerceIn(-1.0, 1.0)))
    return if (ux * vy - uy * vx < 0) -angle else angle
  }

  // ── Instance state ──────────────────────────────────────────────────────────

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

  /** Viewport width and pre-baked [android.graphics.Path] list for one [PlaceCategory] icon. */
  private data class BakedIcon(val viewportWidth: Float, val paths: List<Path>)

  private enum class SavedStateKeys {
    LAST_DRAWN_MARKER_IDS_BITS
  }

  companion object {
    private const val MARKER_VERTICAL_SPACING_PX = 50f
    private const val LINE_HEIGHT_MULTIPLIER = 1.2f

    private const val MIN_ROWS = 1
    private const val MAX_ROWS = 6

    private const val TARGET_MARKER_WIDTH_DP = 160f
    private const val MIN_WIDTH_DIVISOR = 2
    private const val MAX_WIDTH_DIVISOR = 5

    private const val MARKER_OVERLAP_GUARD_FRACTION = 0.5f
    private const val MAX_EXPECTED_ROLL_RADIANS = Math.PI / 6

    private const val MARKER_PADDING_DP = 16f
    private const val ELLIPSIS_WIDTH_PX = 10f

    // Text sizes: title matches PlaceListItem's titleMedium (16sp);
    // distance uses the same size as labelLarge (14sp).
    private const val MARKER_TITLE_TEXT_SIZE_SP = 16f
    private const val MARKER_DISTANCE_TEXT_SIZE_SP = 14f

    private const val MARKER_RECT_F_CORNER_RADIUS_DP = 16f

    // Left icon box must be at least this tall so the icon is recognisable.
    // Mirrors the 64dp Surface in PlaceListItem, scaled down for the AR context.
    private const val MIN_LEFT_BOX_SIZE_DP = 48f

    // Fraction of the box side that the icon fills, leaving even padding on all sides.
    private const val ICON_BOX_FILL_FRACTION = 0.60f

    private const val LOCATION_REASSIGN_THRESHOLD_METERS = 10f
  }
}
