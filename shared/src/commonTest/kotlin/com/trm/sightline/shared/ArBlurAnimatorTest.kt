package com.trm.sightline.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Seam: the Swift-facing [ArBlurAnimator] that carries the Android renderer's animated-lod and
 * contrasting-colour state machine across the boundary.
 * These catch constant and transition regressions; the actual GPU blur is a separate concern.
 */
class ArBlurAnimatorTest {
  private val animator = ArBlurAnimator()

  @Test
  fun `starts sharp with the maximum contrasting mix and no colour`() {
    val frame = animator.nextFrame()

    assertFalse(frame.shouldBlur)
    assertFalse(frame.isAnimatingLod)
    assertFalse(frame.usesContrastingColor)
    assertEquals(MIN_LOD, frame.lod)
    assertEquals(MAX_MIX, frame.contrastingColorMix)
  }

  @Test
  fun `stays sharp while idle`() {
    val first = animator.nextFrame()
    val second = animator.nextFrame()

    assertEquals(first.lod, second.lod)
    assertEquals(first.contrastingColorMix, second.contrastingColorMix)
    assertFalse(second.shouldBlur)
  }

  @Test
  fun `enabling blur ramps lod up and the contrasting mix down over eighteen frames`() {
    animator.setBlurEnabled(enabled = true, animated = true)

    val first = animator.nextFrame()
    assertTrue(first.shouldBlur)
    assertTrue(first.isAnimatingLod)
    assertEquals(MIN_LOD + LOD_STEP, first.lod, TOLERANCE)
    assertEquals(MAX_MIX - MIX_STEP, first.contrastingColorMix, TOLERANCE)

    var frame = first
    repeat(BLUR_ANIMATION_FRAMES - 1) { frame = animator.nextFrame() }

    assertFalse(frame.isAnimatingLod)
    assertTrue(frame.shouldBlur)
    assertEquals(MAX_LOD, frame.lod, TOLERANCE)
    assertEquals(MIN_MIX, frame.contrastingColorMix, TOLERANCE)
  }

  @Test
  fun `enabling blur without animation jumps straight to maximum lod`() {
    animator.setBlurEnabled(enabled = true, animated = false)

    val frame = animator.nextFrame()

    assertTrue(frame.shouldBlur)
    assertFalse(frame.isAnimatingLod)
    assertEquals(MAX_LOD, frame.lod, TOLERANCE)
    assertEquals(MIN_MIX, frame.contrastingColorMix, TOLERANCE)
  }

  @Test
  fun `disabling animated blur ramps back down and then renders sharp`() {
    animator.setBlurEnabled(enabled = true, animated = false)
    animator.nextFrame()

    animator.setBlurEnabled(enabled = false, animated = true)
    var frame = animator.nextFrame()
    assertTrue(frame.shouldBlur)

    repeat(BLUR_ANIMATION_FRAMES - 1) { frame = animator.nextFrame() }

    assertFalse(frame.shouldBlur)
    assertFalse(frame.isAnimatingLod)
    assertEquals(MIN_LOD, frame.lod, TOLERANCE)
    assertEquals(MAX_MIX, frame.contrastingColorMix, TOLERANCE)
  }

  @Test
  fun `the first contrasting colour applies immediately`() {
    animator.setContrastingColor(red = 0.25f, green = 0.5f, blue = 0.75f)

    val frame = animator.nextFrame()

    assertTrue(frame.usesContrastingColor)
    assertFalse(frame.isAnimatingContrastingColor)
    assertEquals(0.25f, frame.contrastingRed)
    assertEquals(0.5f, frame.contrastingGreen)
    assertEquals(0.75f, frame.contrastingBlue)
  }

  @Test
  fun `a later contrasting colour animates over sixty frames`() {
    animator.setContrastingColor(red = 0f, green = 0f, blue = 0f)
    animator.nextFrame()

    animator.setContrastingColor(red = 0.6f, green = 0.6f, blue = 0.6f)
    val first = animator.nextFrame()
    assertTrue(first.isAnimatingContrastingColor)
    assertEquals(0.6f / CONTRAST_ANIMATION_FRAMES, first.contrastingRed, TOLERANCE)
    assertEquals(0.6f / CONTRAST_ANIMATION_FRAMES, first.contrastingGreen, TOLERANCE)
    assertEquals(0.6f / CONTRAST_ANIMATION_FRAMES, first.contrastingBlue, TOLERANCE)

    var frame = first
    repeat(CONTRAST_ANIMATION_FRAMES - 1) { frame = animator.nextFrame() }

    assertFalse(frame.isAnimatingContrastingColor)
    assertEquals(0.6f, frame.contrastingRed, TOLERANCE)
    assertEquals(0.6f, frame.contrastingGreen, TOLERANCE)
    assertEquals(0.6f, frame.contrastingBlue, TOLERANCE)
  }

  @Test
  fun `reset returns to the initial sharp state`() {
    animator.setBlurEnabled(enabled = true, animated = false)
    animator.setContrastingColor(red = 0.1f, green = 0.2f, blue = 0.3f)
    animator.nextFrame()

    animator.reset()
    val frame = animator.nextFrame()

    assertFalse(frame.shouldBlur)
    assertFalse(frame.usesContrastingColor)
    assertEquals(MIN_LOD, frame.lod)
    assertEquals(MAX_MIX, frame.contrastingColorMix)
  }

  private companion object {
    private const val TOLERANCE = 1e-6f
    private const val MIN_LOD = -2f
    private const val MAX_LOD = 2f
    private const val MIN_MIX = 0f
    private const val MAX_MIX = 0.025f
    private const val BLUR_ANIMATION_FRAMES = 18
    private const val CONTRAST_ANIMATION_FRAMES = 60
    private const val LOD_STEP = (MAX_LOD - MIN_LOD) / BLUR_ANIMATION_FRAMES
    private const val MIX_STEP = (MAX_MIX - MIN_MIX) / BLUR_ANIMATION_FRAMES
  }
}
