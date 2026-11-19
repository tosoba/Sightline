package com.trm.sightline.shared

/**
 * One frame of blur state, handed to the Metal renderer.
 * [shouldBlur] is true while blur is enabled or the lod is still animating out.
 */
data class ArBlurFrame(
  val shouldBlur: Boolean,
  val isAnimatingLod: Boolean,
  val isAnimatingContrastingColor: Boolean,
  val lod: Float,
  val contrastingColorMix: Float,
  val contrastingRed: Float,
  val contrastingGreen: Float,
  val contrastingBlue: Float,
  val usesContrastingColor: Boolean,
)

/**
 * Port of the animated-lod and contrasting-colour state machine from the Android OpenGL renderer
 * (`opengl_renderer_jni.cpp`): lod sweeps [-2, 2] over 18 frames, the contrasting mix sweeps
 * [0, 0.025] over the same 18 frames, and a changed contrasting colour eases over 60 frames.
 *
 * The sentinel colour (-1, -1, -1) means "do not tint", matching the shader's `contrastingColor !=
 * vec3(-1)` check. Not thread-safe: drive it from the renderer thread.
 */
class ArBlurAnimator {
  private var blurEnabled = false
  private var lod = MIN_LOD
  private var blurAnimationFrame = -1

  private var contrastingColorMix = MAX_CONTRASTING_COLOR_MIX
  private var contrastingColorAnimationFrame = CONTRASTING_COLOR_ANIMATION_FRAMES
  private var targetRed = NOT_SET
  private var targetGreen = NOT_SET
  private var targetBlue = NOT_SET
  private var red = NOT_SET
  private var green = NOT_SET
  private var blue = NOT_SET

  val isBlurEnabled: Boolean
    get() = blurEnabled

  fun setBlurEnabled(enabled: Boolean, animated: Boolean) {
    if (blurEnabled == enabled) return
    blurEnabled = enabled
    if (enabled && blurAnimationFrame == -1) {
      if (animated) {
        blurAnimationFrame = 0
      } else {
        blurAnimationFrame = BLUR_ANIMATION_FRAMES
        lod = MAX_LOD
        contrastingColorMix = MIN_CONTRASTING_COLOR_MIX
      }
    } else if (!enabled && blurAnimationFrame == BLUR_ANIMATION_FRAMES) {
      if (animated) {
        blurAnimationFrame = BLUR_ANIMATION_FRAMES - 1
      } else {
        blurAnimationFrame = -1
        lod = MIN_LOD
        contrastingColorMix = MAX_CONTRASTING_COLOR_MIX
      }
    }
  }

  fun setContrastingColor(red: Float, green: Float, blue: Float) {
    targetRed = red
    targetGreen = green
    targetBlue = blue
    if (this.red == NOT_SET && this.green == NOT_SET && this.blue == NOT_SET) {
      this.red = red
      this.green = green
      this.blue = blue
    } else {
      contrastingColorAnimationFrame = 0
    }
  }

  fun nextFrame(): ArBlurFrame {
    if (isAnimatingLod) animateLod()
    if (isAnimatingContrastingColor) animateContrastingColor()
    return ArBlurFrame(
      shouldBlur = blurEnabled || isAnimatingLod,
      isAnimatingLod = isAnimatingLod,
      isAnimatingContrastingColor = isAnimatingContrastingColor,
      lod = lod,
      contrastingColorMix = contrastingColorMix,
      contrastingRed = red,
      contrastingGreen = green,
      contrastingBlue = blue,
      usesContrastingColor = !(red == NOT_SET && green == NOT_SET && blue == NOT_SET),
    )
  }

  fun reset() {
    blurEnabled = false
    lod = MIN_LOD
    blurAnimationFrame = -1
    contrastingColorMix = MAX_CONTRASTING_COLOR_MIX
    contrastingColorAnimationFrame = CONTRASTING_COLOR_ANIMATION_FRAMES
    targetRed = NOT_SET
    targetGreen = NOT_SET
    targetBlue = NOT_SET
    red = NOT_SET
    green = NOT_SET
    blue = NOT_SET
  }

  private val isAnimatingLod: Boolean
    get() = blurAnimationFrame > -1 && blurAnimationFrame < BLUR_ANIMATION_FRAMES

  private val isAnimatingContrastingColor: Boolean
    get() = contrastingColorAnimationFrame < CONTRASTING_COLOR_ANIMATION_FRAMES

  private fun animateLod() {
    if (blurEnabled) {
      if (lod < MAX_LOD) lod += LOD_INCREMENT
      if (contrastingColorMix > MIN_CONTRASTING_COLOR_MIX) {
        contrastingColorMix -= CONTRASTING_COLOR_MIX_INCREMENT
      }
      blurAnimationFrame++
    } else {
      if (lod > MIN_LOD) lod -= LOD_INCREMENT
      if (contrastingColorMix < MAX_CONTRASTING_COLOR_MIX) {
        contrastingColorMix += CONTRASTING_COLOR_MIX_INCREMENT
      }
      blurAnimationFrame--
    }
  }

  private fun animateContrastingColor() {
    val fraction =
      (contrastingColorAnimationFrame + 1).toFloat() / CONTRASTING_COLOR_ANIMATION_FRAMES
    red = (targetRed - red) * fraction + red
    green = (targetGreen - green) * fraction + green
    blue = (targetBlue - blue) * fraction + blue
    contrastingColorAnimationFrame++
  }

  private companion object {
    private const val NOT_SET = -1f
    private const val MIN_LOD = -2f
    private const val MAX_LOD = 2f
    private const val BLUR_ANIMATION_FRAMES = 18
    private const val LOD_INCREMENT = (MAX_LOD - MIN_LOD) / BLUR_ANIMATION_FRAMES
    private const val MIN_CONTRASTING_COLOR_MIX = 0f
    private const val MAX_CONTRASTING_COLOR_MIX = 0.025f
    private const val CONTRASTING_COLOR_MIX_INCREMENT =
      (MAX_CONTRASTING_COLOR_MIX - MIN_CONTRASTING_COLOR_MIX) / BLUR_ANIMATION_FRAMES
    private const val CONTRASTING_COLOR_ANIMATION_FRAMES = 60
  }
}
