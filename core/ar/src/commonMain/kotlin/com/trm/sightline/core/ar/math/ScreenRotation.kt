package com.trm.sightline.core.ar.math

/**
 * Rotation of the screen relative to the device's natural orientation, mirroring
 * `android.view.Surface.ROTATION_*` without depending on the Android SDK.
 */
enum class ScreenRotation {
  ROTATION_0,
  ROTATION_90,
  ROTATION_180,
  ROTATION_270,
}
