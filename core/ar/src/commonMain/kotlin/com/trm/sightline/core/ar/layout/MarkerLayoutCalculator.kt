package com.trm.sightline.core.ar.layout

/**
 * How [MarkerLayoutCalculator] turns the usable viewport height into card height and row count.
 *
 * These are the two sizing strategies the platform models have always used. The mode is an explicit
 * behavior-preserving platform strategy rather than two algorithms landing here by accident:
 * Android has always grown rows first from a text-metric minimum, while iOS has always divided the
 * height by a fixed target row count and then derived the rows that fit.
 */
enum class MarkerSizingMode {
  /** Rows fit at the minimum card height first, then cards grow to fill the remaining height. */
  FitRows,

  /**
   * Card height targets `usableHeight / targetRows`, then the rows that fit become the row count.
   */
  TargetHeightFirst,
}

/**
 * Platform-dependent bounds for the adaptive AR Marker card layout.
 *
 * Both platforms share one calculator ([MarkerLayoutCalculator]); these values differ per platform,
 * so they carry every platform value: pixel/point card bounds, the target width that drives the
 * column divisor, the row range, the vertical gap between stacked rows, the overlap-guard fraction
 * used by [com.trm.sightline.core.ar.math.MarkerSlotAssignment], the padding below the top inset
 * and the [sizingMode] selector. [FitRows] is the default because it reproduces Android's current
 * geometry; iOS opts into [MarkerSizingMode.TargetHeightFirst] with [targetRows].
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
  val sizingMode: MarkerSizingMode = MarkerSizingMode.FitRows,
  val targetRows: Int = 6,
) {
  init {
    require(minMarkerHeight <= maxMarkerHeight)
    require(minMarkerWidth <= maxMarkerWidth)
    require(targetMarkerWidth > 0f)
    require(minWidthDivisor in 1..maxWidthDivisor)
    require(minRows in 1..maxRows)
    require(verticalSpacing >= 0f)
    require(overlapGuardFraction >= 0f)
    require(topPadding >= 0f)
    require(targetRows >= 1)
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
 * Width is shared by both [MarkerSizingMode]s:
 * - `divisor = clamp((usableWidth / targetMarkerWidth).toInt(), minWidthDivisor, maxWidthDivisor)`
 * - `markerWidth = clamp(usableWidth / divisor, minMarkerWidth, maxMarkerWidth)`
 *
 * Height depends on the platform's [MarkerSizingMode]:
 * - [MarkerSizingMode.FitRows] (default, Android): `rows = clamp((usableHeight / (minMarkerHeight +
 *   verticalSpacing)).toInt(), minRows, maxRows)` then `markerHeight = clamp(usableHeight / rows -
 *   verticalSpacing, minMarkerHeight, maxMarkerHeight)`.
 * - [MarkerSizingMode.TargetHeightFirst] (iOS): `markerHeight = clamp(usableHeight / targetRows,
 *   minMarkerHeight, maxMarkerHeight)` then `rows = clamp((usableHeight + verticalSpacing) /
 *   (markerHeight + verticalSpacing)).toInt(), minRows, maxRows)`.
 *
 * Both modes finish with `overlapGuard = markerWidth * overlapGuardFraction` and `baseY =
 * topInset + topPadding + markerHeight / 2`.
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
    val markerWidth = (width / divisor).coerceIn(config.minMarkerWidth, config.maxMarkerWidth)

    val rows: Int
    val markerHeight: Float
    when (config.sizingMode) {
      MarkerSizingMode.FitRows -> {
        rows =
          (height / (config.minMarkerHeight + config.verticalSpacing))
            .toInt()
            .coerceIn(config.minRows, config.maxRows)
        markerHeight =
          (height / rows - config.verticalSpacing).coerceIn(
            config.minMarkerHeight,
            config.maxMarkerHeight,
          )
      }
      MarkerSizingMode.TargetHeightFirst -> {
        markerHeight =
          (height / config.targetRows).coerceIn(config.minMarkerHeight, config.maxMarkerHeight)
        rows =
          ((height + config.verticalSpacing) / (markerHeight + config.verticalSpacing))
            .toInt()
            .coerceIn(config.minRows, config.maxRows)
      }
    }

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
