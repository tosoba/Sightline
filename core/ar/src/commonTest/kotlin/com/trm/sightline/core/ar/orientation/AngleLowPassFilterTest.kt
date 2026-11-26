package com.trm.sightline.core.ar.orientation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Seam: the circular low-pass filter shared by the Android sensor manager and the iOS orientation
 * tracker. These catch smoothing-factor and wrap-around regressions; they say nothing about how the
 * sensor feeds it.
 */
class AngleLowPassFilterTest {
  @Test
  fun `the first reading passes through unchanged`() {
    assertEquals(1.25f, AngleLowPassFilter().filter(1.25f))
  }

  @Test
  fun `a small change is scaled by the smoothing factor`() {
    val filter = AngleLowPassFilter()
    filter.filter(0f)

    assertEquals(0.12f * 0.2f, filter.filter(0.2f), TOLERANCE)
  }

  @Test
  fun `a change above the smoothing threshold snaps to the new value`() {
    val filter = AngleLowPassFilter()
    filter.filter(0f)

    assertEquals(0.5f, filter.filter(0.5f), TOLERANCE)
  }

  @Test
  fun `a wrap across the pi boundary smooths by the short way round`() {
    val filter = AngleLowPassFilter()
    filter.filter((PI - 0.01).toFloat())

    val actual = filter.filter((-PI + 0.01).toFloat())

    assertAngle(PI - 0.01 + 0.12 * 0.02, actual)
  }

  @Test
  fun `a wrap back across the pi boundary smooths by the short way round`() {
    val filter = AngleLowPassFilter()
    filter.filter((-PI + 0.01).toFloat())

    val actual = filter.filter((PI - 0.01).toFloat())

    assertAngle(-PI + 0.01 - 0.12 * 0.02, actual)
  }

  @Test
  fun `reset makes the next reading pass through unchanged`() {
    val filter = AngleLowPassFilter()
    filter.filter(0f)
    filter.filter(1f)

    filter.reset()

    assertEquals(1.5f, filter.filter(1.5f))
  }

  private fun assertAngle(expected: Double, actual: Float) {
    val circle = 2 * PI
    val rawDifference = abs((expected % circle) - (actual.toDouble() % circle))
    val difference = minOf(rawDifference, circle - rawDifference)
    assertEquals(0.0, difference, 1e-5)
  }

  private companion object {
    private const val TOLERANCE = 1e-6f
  }
}
