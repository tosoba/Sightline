package com.trm.sightline.core.ar.orientation

import kotlin.math.PI
import kotlin.math.abs

/**
 * Circular low-pass smoothing for angles in radians. A change smaller than
 * [SMOOTH_THRESHOLD] is eased by [SMOOTH_FACTOR], anything larger snaps, and changes that wrap
 * across the pi boundary are measured the short way round.
 *
 * Stateful, not thread-safe: create one per angle channel and drive it from one thread.
 */
class AngleLowPassFilter(
  private val smoothThreshold: Float = SMOOTH_THRESHOLD,
  private val smoothFactor: Float = SMOOTH_FACTOR,
) {
  private var hasPrevious = false
  private var previous = 0f

  fun filter(newValue: Float): Float {
    if (!hasPrevious) {
      hasPrevious = true
      previous = newValue
      return newValue
    }

    val delta = newValue - previous
    val smoothed =
      if (abs(delta) < CIRCLE / 2) {
        if (abs(delta) > smoothThreshold) newValue else previous + smoothFactor * delta
      } else {
        val wrappedDelta = CIRCLE - abs(delta)
        when {
          wrappedDelta > smoothThreshold -> newValue
          previous > newValue -> (previous + smoothFactor * wrappedDelta + CIRCLE) % CIRCLE
          else -> (previous - smoothFactor * wrappedDelta + CIRCLE) % CIRCLE
        }
      }

    previous = smoothed
    return smoothed
  }

  fun reset() {
    hasPrevious = false
    previous = 0f
  }

  companion object {
    val CIRCLE: Float = (PI * 2).toFloat()
    val SMOOTH_THRESHOLD: Float = CIRCLE / 20f
    const val SMOOTH_FACTOR: Float = 0.12f
  }
}
