package com.trm.sightline.core.ar.math

/**
 * Screen-space hole for one marker, centred on ([centerX], [centerY]). The overlay draws its card
 * into this rect and the Metal renderer punches the matching stencil hole, so both stay aligned.
 */
fun markerRect(
  centerX: Float,
  centerY: Float,
  widthPx: Float,
  heightPx: Float,
  cornerRadiusPx: Float,
): RoundedRectF =
  RoundedRectF(
    left = centerX - widthPx / 2f,
    top = centerY - heightPx / 2f,
    right = centerX + widthPx / 2f,
    bottom = centerY + heightPx / 2f,
    cornerRadius = cornerRadiusPx,
  )
