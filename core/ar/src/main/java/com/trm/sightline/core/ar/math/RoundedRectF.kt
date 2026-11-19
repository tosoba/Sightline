package com.trm.sightline.core.ar.math

/** Rectangle in screen pixels with rounded corners, expressed without Android's `RectF`. */
data class RoundedRectF(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float,
  val cornerRadius: Float,
) {
  val width: Float
    get() = right - left

  val height: Float
    get() = bottom - top
}
