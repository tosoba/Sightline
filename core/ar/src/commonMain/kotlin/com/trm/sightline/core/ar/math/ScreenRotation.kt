package com.trm.sightline.core.ar.math

import kotlin.math.PI

/**
 * Rotation of the screen relative to the device's natural orientation, mirroring
 * `android.view.Surface.ROTATION_*` without depending on the Android SDK.
 *
 * [rollOffsetRadians] is the roll a device physically rotated to this Screen Rotation reports,
 * which the scripted simulator feed applies so landscape poses use the same projection path.
 */
enum class ScreenRotation(val rollOffsetRadians: Float) {
  ROTATION_0(0f),
  ROTATION_90((-PI / 2).toFloat()),
  ROTATION_180(PI.toFloat()),
  ROTATION_270((PI / 2).toFloat()),
}
