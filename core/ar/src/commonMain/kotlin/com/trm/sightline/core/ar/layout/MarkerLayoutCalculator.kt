package com.trm.sightline.core.ar.layout

/**
 * Platform-dependent bounds for the adaptive AR Marker card layout.
 *
 * Both platforms share one sizing formula ([MarkerLayoutCalculator]); only these bounds differ, so
 * they carry every platform value: pixel/point card bounds, the target width that drives the column
 * divisor, the row range, the vertical gap between stacked rows, the overlap-guard fraction used by
 * [com.trm.sightline.core.ar.math.MarkerSlotAssignment] and the padding below the top inset.
 */
data class MarkerLayoutConfig(
  val minMarkerHeight: Float,
  val maxMarkerHeight: Float,
  val minMarkerWidth: Float,
  val maxMarkerWidth: Float,
  val targetMarkerWidth: Float,
  val minWidthDivisor: Int,
  val maxWidthDivisor: Int,
  val minRows: Int,
  val maxRows: Int,
  val verticalSpacing: Float,
  val overlapGuardFraction: Float,
  val topPadding: Float,
) {
  init {
    require(minMarkerHeight <= maxMarkerHeight)
    require(minMarkerWidth <= maxMarkerWidth)
    require(targetMarkerWidth > 0f)
    require(minWidthDivisor >= 1 && minWidthDivisor <= maxWidthDivisor)
    require(minRows >= 1 && minRows <= maxRows)
    require(verticalSpacing >= 0f)
    require(overlapGuardFraction >= 0f)
    require(topPadding >= 0f)
  }
}

/** Resolved pixel/point geometry of one adaptive AR Marker card layout. */
data class MarkerLayoutGeometry(
  val markerWidth: Float,
  val markerHeight: Float,
  val rows: Int,
  val divisor: Int,
  val baseY: Float,
  val overlapGuard: Float,
  val verticalSpacing: Float,
)

/**
 * Computes the adaptive AR Marker card layout from the usable viewport.
 *
 * Canonical formula, shared by Android and iOS:
 * - `divisor = clamp((usableWidth / targetMarkerWidth).toInt(), minWidthDivisor, maxWidthDivisor)`
 * - `markerWidth = clamp(usableWidth / divisor, minMarkerWidth, maxMarkerWidth)`
 * - `rows = clamp((usableHeight / (minMarkerHeight + verticalSpacing)).toInt(), minRows, maxRows)`
 * - `markerHeight = clamp(usableHeight / rows - verticalSpacing, minMarkerHeight, maxMarkerHeight)`
 * - `overlapGuard = markerWidth * overlapGuardFraction`
 * - `baseY = topInset + topPadding + markerHeight / 2`
 *
 * Rows fit at the minimum card height first, then the cards grow to consume the remaining height
 * up to the platform maximum. That reproduces Android's text-metric-driven card height exactly and
 * keeps iOS inside its 72...88 pt bounds; only a viewport shorter than six minimum rows can grow
 * iOS cards slightly above the old fixed 72 pt minimum.
 */
object MarkerLayoutCalculator {
  fun calculate(
    config: MarkerLayoutConfig,
    usableWidth: Float,
    usableHeight: Float,
    topInset: Float,
  ): MarkerLayoutGeometry {
    val width = usableWidth.coerceAtLeast(0f)
    val height = usableHeight.coerceAtLeast(0f)

    val divisor =
      (width / config.targetMarkerWidth)
        .toInt()
        .coerceIn(config.minWidthDivisor, config.maxWidthDivisor)
    val markerWidth =
      (width / divisor).coerceIn(config.minMarkerWidth, config.maxMarkerWidth)

    val rows =
      (height / (config.minMarkerHeight + config.verticalSpacing))
        .toInt()
        .coerceIn(config.minRows, config.maxRows)
    val markerHeight =
      (height / rows - config.verticalSpacing)
        .coerceIn(config.minMarkerHeight, config.maxMarkerHeight)

    return MarkerLayoutGeometry(
      markerWidth = markerWidth,
      markerHeight = markerHeight,
      rows = rows,
      divisor = divisor,
      baseY = topInset + config.topPadding + markerHeight / 2f,
      overlapGuard = markerWidth * config.overlapGuardFraction,
      verticalSpacing = config.verticalSpacing,
    )
  }
}
