package com.trm.sightline.core.ar.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkerLayoutCalculatorTest {
  private val phoneConfig =
    MarkerLayoutConfig(
      minMarkerHeight = 72f,
      maxMarkerHeight = 88f,
      minMarkerWidth = 150f,
      maxMarkerWidth = 220f,
      targetMarkerWidth = 160f,
      minWidthDivisor = 2,
      maxWidthDivisor = 6,
      minRows = 1,
      maxRows = 6,
      verticalSpacing = 16f,
      overlapGuardFraction = 0.5f,
      topPadding = 12f,
    )

  private val androidConfig =
    MarkerLayoutConfig(
      minMarkerHeight = 189f,
      maxMarkerHeight = 1617.75f,
      minMarkerWidth = 0f,
      maxMarkerWidth = 1080f,
      targetMarkerWidth = 420f,
      minWidthDivisor = 2,
      maxWidthDivisor = 5,
      minRows = 1,
      maxRows = 6,
      verticalSpacing = 50f,
      overlapGuardFraction = 0.5f,
      topPadding = 42f,
    )

  @Test
  fun `phone viewport sizes cards from the target width divisor`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = phoneConfig,
        usableWidth = 393f,
        usableHeight = 759f,
        topInset = 59f,
      )

    assertEquals(2, geometry.divisor)
    assertEquals(196.5f, geometry.markerWidth)
    assertEquals(6, geometry.rows)
    assertEquals(88f, geometry.markerHeight)
    assertEquals(98.25f, geometry.overlapGuard)
    assertEquals(115f, geometry.baseY)
  }

  @Test
  fun `tablet viewport caps the width divisor and card height`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = phoneConfig,
        usableWidth = 1024f,
        usableHeight = 1322f,
        topInset = 24f,
      )

    assertEquals(6, geometry.divisor)
    assertEquals(1024f / 6f, geometry.markerWidth, 1e-4f)
    assertEquals(6, geometry.rows)
    assertEquals(88f, geometry.markerHeight)
    assertEquals(24f + 12f + 44f, geometry.baseY)
  }

  @Test
  fun `android viewport keeps the text metric minimum height`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = androidConfig,
        usableWidth = 1080f,
        usableHeight = 1617.75f,
        topInset = 63f,
      )

    assertEquals(2, geometry.divisor)
    assertEquals(540f, geometry.markerWidth)
    assertEquals(6, geometry.rows)
    assertEquals(219.625f, geometry.markerHeight)
    assertEquals(270f, geometry.overlapGuard)
    assertEquals(214.8125f, geometry.baseY)
  }

  @Test
  fun `ios target height mode restores the four seventy two point rows in landscape`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config =
          phoneConfig.copy(
            sizingMode = MarkerSizingMode.TargetHeightFirst,
            targetRows = 6,
          ),
        usableWidth = 852f,
        usableHeight = 381f,
        topInset = 0f,
      )

    assertEquals(4, geometry.rows)
    assertEquals(72f, geometry.markerHeight)
  }

  @Test
  fun `ios target height mode caps the card height on tall viewports`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config =
          phoneConfig.copy(
            sizingMode = MarkerSizingMode.TargetHeightFirst,
            targetRows = 6,
          ),
        usableWidth = 393f,
        usableHeight = 800f,
        topInset = 0f,
      )

    assertEquals(6, geometry.rows)
    assertEquals(88f, geometry.markerHeight)
  }

  @Test
  fun `default fit rows mode keeps android cards at the minimum on short viewports`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = androidConfig,
        usableWidth = 1080f,
        usableHeight = 200f,
        topInset = 0f,
      )

    assertEquals(1, geometry.rows)
    assertEquals(189f, geometry.markerHeight)
  }

  @Test
  fun `default fit rows mode still sizes a short phone viewport from the minimum row`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = phoneConfig,
        usableWidth = 852f,
        usableHeight = 381f,
        topInset = 0f,
      )

    assertEquals(4, geometry.rows)
    assertEquals(79.25f, geometry.markerHeight)
  }

  @Test
  fun `tiny viewport falls back to one row and the minimum card height`() {
    val geometry =
      MarkerLayoutCalculator.calculate(
        config = phoneConfig,
        usableWidth = 50f,
        usableHeight = 50f,
        topInset = 0f,
      )

    assertEquals(1, geometry.rows)
    assertEquals(72f, geometry.markerHeight)
    assertEquals(150f, geometry.markerWidth)
    assertEquals(12f + 36f, geometry.baseY)
  }
}
