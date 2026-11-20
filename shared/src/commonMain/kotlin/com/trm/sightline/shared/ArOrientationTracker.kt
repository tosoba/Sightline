package com.trm.sightline.shared

import com.trm.sightline.core.ar.orientation.Orientation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/**
 * A Core Motion `CMAttitude.rotationMatrix` flattened into value form.
 *
 * The matrix maps the iOS reference frame (X = magnetic north, Y = west, Z = up) onto the device
 * frame, so its rows are the device axes expressed in that reference frame.
 */
data class RotationMatrix(
  val m11: Double,
  val m12: Double,
  val m13: Double,
  val m21: Double,
  val m22: Double,
  val m23: Double,
  val m31: Double,
  val m32: Double,
  val m33: Double,
)

/**
 * Converts Core Motion attitude into the triple Android's `SensorManager.getOrientation` produces
 * for `remapCoordinateSystem(R, AXIS_X, AXIS_Z)` (Sightline's `OrientationManager.Mode.AR`), then
 * applies the same low-pass the Android sensor listener applies.
 *
 * The remap makes the new device Y axis `-Z`, i.e. the rear camera axis, so the Android triple
 * means: azimuth = compass bearing of the camera direction, pitch = how far the camera points down,
 * roll = screen roll about the camera axis. Those values are consumed unchanged by the shared
 * `Math3D.getCamRotation`.
 *
 * Stateful low-pass, not thread-safe: create one per renderer and call [update] from one thread.
 */
class ArOrientationTracker {
  private var previous: Orientation? = null

  fun update(rotationMatrix: RotationMatrix): Orientation {
    val raw = remap(rotationMatrix)
    val smoothed = previous?.let { old ->
      Orientation(
        pitch = lowPass(raw.pitch, old.pitch),
        roll = lowPass(raw.roll, old.roll),
        azimuth = lowPass(raw.azimuth, old.azimuth),
      )
    } ?: raw
    previous = smoothed.copy()
    return smoothed
  }

  companion object {
    private const val CIRCLE = (PI * 2).toFloat()
    private const val SMOOTH_THRESHOLD = CIRCLE / 20f
    private const val SMOOTH_FACTOR = 0.12f

    fun remap(matrix: RotationMatrix): Orientation {
      val outEast = matrix.m32
      // Negative zero would make atan2 jump to pi for the degenerate flat-on-a-table pose.
      val outNorth = if (matrix.m31 == 0.0) 0.0 else -matrix.m31
      val rightUp = matrix.m13
      val topUp = matrix.m23

      val azimuth = atan2(outEast, outNorth)
      val pitch = asin(matrix.m33.coerceIn(-1.0, 1.0))
      val roll = atan2(-rightUp, topUp)

      return Orientation(
        pitch = pitch.toFloat(),
        roll = roll.toFloat(),
        azimuth = azimuth.toFloat(),
      )
    }

    private fun lowPass(newValue: Float, oldValue: Float): Float {
      val delta = newValue - oldValue
      return if (abs(delta) < CIRCLE / 2) {
        if (abs(delta) > SMOOTH_THRESHOLD) newValue else oldValue + SMOOTH_FACTOR * delta
      } else {
        val wrappedDelta = CIRCLE - abs(delta)
        when {
          wrappedDelta > SMOOTH_THRESHOLD -> newValue
          oldValue > newValue -> (oldValue + SMOOTH_FACTOR * wrappedDelta + CIRCLE) % CIRCLE
          else -> (oldValue - SMOOTH_FACTOR * wrappedDelta + CIRCLE) % CIRCLE
        }
      }
    }
  }
}
