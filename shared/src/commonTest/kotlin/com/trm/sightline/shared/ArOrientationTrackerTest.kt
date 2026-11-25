package com.trm.sightline.shared

import com.trm.sightline.core.ar.math.ScreenRotation
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Seam: the Swift-facing [ArOrientationTracker] that turns a Core Motion rotation matrix into the
 * Android-equivalent `(pitch, roll, azimuth)` triple plus the Android low-pass.
 * These catch remapping sign/axis errors and smoothing drift; they cannot prove the sensor feeds
 * a real device pose (that needs a phone with a magnetometer).
 */
class ArOrientationTrackerTest {
  @Test
  fun `an upright portrait phone facing north reads as zero pose`() {
    val orientation = trackerReads(uprightFacing(bearingRadians = 0.0))

    assertAngle(0.0, orientation.pitch)
    assertAngle(0.0, orientation.roll)
    assertAngle(0.0, orientation.azimuth)
  }

  @Test
  fun `an upright portrait phone facing east reads a quarter turn of azimuth`() {
    val orientation = trackerReads(uprightFacing(bearingRadians = PI / 2))

    assertAngle(0.0, orientation.pitch)
    assertAngle(0.0, orientation.roll)
    assertAngle(PI / 2, orientation.azimuth)
  }

  @Test
  fun `a phone lying flat screen up looking down reads a quarter turn of pitch`() {
    val orientation = trackerReads(flatScreenUpTopNorth())

    assertAngle(PI / 2, orientation.pitch)
    assertAngle(0.0, orientation.roll)
    assertAngle(0.0, orientation.azimuth)
  }

  @Test
  fun `pitching the camera down tilts pitch and leaves azimuth and roll alone`() {
    val orientation = trackerReads(pitchedDownFacing(bearingRadians = 0.0, downRadians = PI / 6))

    assertAngle(PI / 6, orientation.pitch)
    assertAngle(0.0, orientation.roll)
    assertAngle(0.0, orientation.azimuth)
  }

  @Test
  fun `rolling the phone counter clockwise reads a negative roll`() {
    val orientation = trackerReads(rolledFacing(bearingRadians = 0.0, rollRadians = PI / 9))

    assertAngle(0.0, orientation.pitch)
    assertAngle(-PI / 9, orientation.roll)
    assertAngle(0.0, orientation.azimuth)
  }

  @Test
  fun `the first reading passes through unchanged`() {
    val tracker = ArOrientationTracker()

    val orientation = tracker.update(uprightFacing(bearingRadians = 0.3))

    assertAngle(0.3, orientation.azimuth)
  }

  @Test
  fun `a small change is smoothed by the Android low pass factor`() {
    val tracker = ArOrientationTracker()
    tracker.update(uprightFacing(bearingRadians = 0.0))

    val orientation = tracker.update(uprightFacing(bearingRadians = toRadians(5.0)))

    assertAngle(0.12 * toRadians(5.0), orientation.azimuth)
  }

  @Test
  fun `a change above the smoothing threshold snaps to the new value`() {
    val tracker = ArOrientationTracker()
    tracker.update(uprightFacing(bearingRadians = 0.0))

    val orientation = tracker.update(uprightFacing(bearingRadians = toRadians(30.0)))

    assertAngle(toRadians(30.0), orientation.azimuth)
  }

  @Test
  fun `azimuth wraps across the pi boundary without a full turn jump`() {
    val tracker = ArOrientationTracker()
    tracker.update(uprightFacing(bearingRadians = PI - 0.01))

    val orientation = tracker.update(uprightFacing(bearingRadians = -PI + 0.01))

    assertAngle(PI - 0.01 + 0.12 * 0.02, orientation.azimuth)
  }

  @Test
  fun `the tracker smooths pitch and roll independently of azimuth`() {
    val tracker = ArOrientationTracker()
    tracker.update(pitchedDownFacing(bearingRadians = 0.0, downRadians = 0.0))

    val orientation = tracker.update(pitchedDownFacing(bearingRadians = 0.0, downRadians = 0.02))

    assertTrue(orientation.pitch > 0f)
    assertTrue(orientation.pitch < 0.02f)
    assertAngle(0.0, orientation.azimuth)
  }

  private fun trackerReads(matrix: RotationMatrix) = ArOrientationTracker().update(matrix)

  @Test
  fun `each screen rotation reports the roll offset of a physically rotated device`() {
    assertEquals(0f, ScreenRotation.ROTATION_0.rollOffsetRadians, ROLL_TOLERANCE)
    assertEquals(-1.5707964f, ScreenRotation.ROTATION_90.rollOffsetRadians, ROLL_TOLERANCE)
    assertEquals(3.1415927f, ScreenRotation.ROTATION_180.rollOffsetRadians, ROLL_TOLERANCE)
    assertEquals(1.5707964f, ScreenRotation.ROTATION_270.rollOffsetRadians, ROLL_TOLERANCE)
  }

  private fun assertAngle(expected: Double, actual: Float) {
    assertEquals(expected % (2 * PI), actual.toDouble() % (2 * PI), ANGLE_TOLERANCE)
  }

  private companion object {
    private const val ANGLE_TOLERANCE = 1e-5
    private const val ROLL_TOLERANCE = 1e-6f
  }
}

/**
 * Builds the Core Motion rotation matrix for a physical pose.
 *
 * Core Motion's matrix maps the reference frame onto the device frame, so its rows are the device
 * axes expressed in the iOS reference frame (X = magnetic north, Y = west, Z = up).
 * Pose helpers below describe the device axes in ENU (east, north, up) for readability.
 */
internal fun rotationMatrix(right: Enu, top: Enu, out: Enu): RotationMatrix {
  fun row(axis: Enu) = Triple(axis.north, -axis.east, axis.up)
  val r = row(right)
  val u = row(top)
  val o = row(out)
  return RotationMatrix(
    m11 = r.first,
    m12 = r.second,
    m13 = r.third,
    m21 = u.first,
    m22 = u.second,
    m23 = u.third,
    m31 = o.first,
    m32 = o.second,
    m33 = o.third,
  )
}

internal data class Enu(val east: Double, val north: Double, val up: Double)

internal val EAST = Enu(1.0, 0.0, 0.0)
internal val NORTH = Enu(0.0, 1.0, 0.0)
internal val UP = Enu(0.0, 0.0, 1.0)
internal val SOUTH = Enu(0.0, -1.0, 0.0)
internal val WEST = Enu(-1.0, 0.0, 0.0)

internal fun bearingRadians(degrees: Double) = toRadians(degrees)

internal fun toRadians(degrees: Double): Double = degrees * PI / 180.0

/** Screen up, top edge pointing north: the camera looks at the floor. */
internal fun flatScreenUpTopNorth(): RotationMatrix =
  rotationMatrix(right = EAST, top = NORTH, out = UP)

/** Upright portrait, back camera pointing along [bearingRadians]. */
internal fun uprightFacing(bearingRadians: Double): RotationMatrix {
  val camera = Enu(east = kotlin.math.sin(bearingRadians), north = kotlin.math.cos(bearingRadians), up = 0.0)
  val right = Enu(camera.north, -camera.east, 0.0)
  return rotationMatrix(right = right, top = UP, out = Enu(-camera.east, -camera.north, 0.0))
}

/** Upright portrait facing [bearingRadians] with the camera pitched down by [downRadians]. */
internal fun pitchedDownFacing(bearingRadians: Double, downRadians: Double): RotationMatrix {
  val cosDown = kotlin.math.cos(downRadians)
  val sinDown = kotlin.math.sin(downRadians)
  val camera = Enu(
    east = kotlin.math.sin(bearingRadians) * cosDown,
    north = kotlin.math.cos(bearingRadians) * cosDown,
    up = -sinDown,
  )
  val right = Enu(kotlin.math.cos(bearingRadians), -kotlin.math.sin(bearingRadians), 0.0)
  val top = Enu(
    east = kotlin.math.sin(bearingRadians) * sinDown,
    north = kotlin.math.cos(bearingRadians) * sinDown,
    up = cosDown,
  )
  return rotationMatrix(right = right, top = top, out = Enu(-camera.east, -camera.north, -camera.up))
}

/** Upright portrait facing [bearingRadians], rolled by [rollRadians] about the camera axis. */
internal fun rolledFacing(bearingRadians: Double, rollRadians: Double): RotationMatrix {
  val cosRoll = kotlin.math.cos(rollRadians)
  val sinRoll = kotlin.math.sin(rollRadians)
  val cosBearing = kotlin.math.cos(bearingRadians)
  val sinBearing = kotlin.math.sin(bearingRadians)
  val right = Enu(cosBearing * cosRoll, -sinBearing * cosRoll, sinRoll)
  val top = Enu(-cosBearing * sinRoll, sinBearing * sinRoll, cosRoll)
  val out = Enu(-sinBearing, -cosBearing, 0.0)
  return rotationMatrix(right = right, top = top, out = out)
}
