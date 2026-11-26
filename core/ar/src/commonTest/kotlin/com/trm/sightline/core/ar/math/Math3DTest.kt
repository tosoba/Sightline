package com.trm.sightline.core.ar.math

import com.trm.sightline.core.ar.orientation.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Characterization tests: the expected values were produced by the pre-extraction implementation,
 * so they pin the Android rendering behaviour across the move to commonMain.
 */
class Math3DTest {
  private val orientation = Orientation(pitch = 0.3f, roll = -0.2f, azimuth = 1.1f)

  private class CamRotation(
    val camRot: Vector3,
    val camTrig: Trig3,
    val screenRot: Vector1,
    val screenRotTrig: Trig1,
  )

  private fun camRotation(rotation: ScreenRotation): CamRotation {
    val camRot = Vector3()
    val camTrig = Trig3()
    val screenRot = Vector1()
    val screenRotTrig = Trig1()
    Math3D.getCamRotation(orientation, rotation, camRot, camTrig, screenRot, screenRotTrig)
    return CamRotation(camRot, camTrig, screenRot, screenRotTrig)
  }

  @Test
  fun `getCamRotation maps pitch azimuth and roll onto the 3d axes`() {
    val result = camRotation(ScreenRotation.ROTATION_0)

    assertEquals(-0.30000001192092896, result.camRot.x, 1e-12)
    assertEquals(4.241592677431651, result.camRot.y, 1e-12)
    assertEquals(0.0, result.camRot.z, 0.0)
    assertEquals(0.20000000298023224, result.screenRot.v, 1e-12)
  }

  @Test
  fun `getCamRotation adds a quarter turn for a clockwise landscape screen`() {
    val result = camRotation(ScreenRotation.ROTATION_90)

    assertEquals(-1.3707963238146643, result.screenRot.v, 1e-12)
  }

  @Test
  fun `getCamRotation adds three quarters of a turn for a counter clockwise landscape screen`() {
    val result = camRotation(ScreenRotation.ROTATION_270)

    assertEquals(1.7707963297751288, result.screenRot.v, 1e-12)
  }

  @Test
  fun `getCamRotation flips the screen angle on an upside down screen`() {
    val result = camRotation(ScreenRotation.ROTATION_180)

    assertEquals(3.3415926565700254, result.screenRot.v, 1e-12)
  }

  @Test
  fun `getCamRotation derives the camera trig from the 3d rotation angles`() {
    val result = camRotation(ScreenRotation.ROTATION_0)

    assertEquals(-0.29552021804983797, result.camTrig.xSin, 1e-12)
    assertEquals(0.9553364856027305, result.camTrig.xCos, 1e-12)
    assertEquals(-0.8912073708760093, result.camTrig.ySin, 1e-12)
    assertEquals(-0.45359610017753815, result.camTrig.yCos, 1e-12)
    assertEquals(0.0, result.camTrig.zSin, 0.0)
    assertEquals(1.0, result.camTrig.zCos, 0.0)
  }

  @Test
  fun `getCamRotation derives the screen trig from the screen angle`() {
    val result = camRotation(ScreenRotation.ROTATION_0)

    assertEquals(0.19866933371588721, result.screenRotTrig.sin, 1e-12)
    assertEquals(0.9800665772491609, result.screenRotTrig.cos, 1e-12)
  }

  @Test
  fun `getRelativeTranslationInMeters converts degrees to meters around the camera`() {
    val point = Vector3(x = 13.4050, y = 34.0, z = 52.52)
    val camera = Vector3(x = 13.4, y = 30.0, z = 52.5)
    val out = Vector3()

    Math3D.getRelativeTranslationInMeters(point, camera, out)

    assertEquals(-555.4438927035401, out.x, 1e-9)
    assertEquals(-4.0, out.y, 1e-12)
    assertEquals(-2222.220000000347, out.z, 1e-6)
  }

  @Test
  fun `getRelativeRotation rotates the point into the camera frame`() {
    val camTrig = camRotation(ScreenRotation.ROTATION_0).camTrig
    val relativePos = Vector3(x = 10.0, y = -3.0, z = 12.0)
    val out = Vector3()

    Math3D.getRelativeRotation(relativePos, camTrig, out)

    assertEquals(6.158527448736729, out.x, 1e-9)
    assertEquals(1.3762503300530855, out.y, 1e-9)
    assertEquals(-14.600632681229435, out.z, 1e-9)
  }

  @Test
  fun `convert3dTo2d projects a point in front of the camera onto the screen`() {
    val screenPos = Vector2()
    val drawn =
      Math3D.convert3dTo2d(
        inRelativePos = Vector3(x = 2.0, y = 1.0, z = 5.0),
        inScreenSize =
          Vector2().apply {
            x = 1080.0
            y = 2400.0
          },
        inScreenRatio = Vector3(x = 1740.0, y = 1740.0, z = 1.0),
        inScreenRotTrig = Trig1().apply { setVector1(Vector1().apply { v = 0.2 }) },
        outScreenPos = screenPos,
      )

    assertTrue(drawn)
    assertEquals(1152.989411060823, screenPos.x, 1e-9)
    assertEquals(1679.3370233221149, screenPos.y, 1e-9)
  }

  @Test
  fun `convert3dTo2d reports a point beyond the horizontal bounds as not drawn`() {
    val screenPos = Vector2()
    val drawn =
      Math3D.convert3dTo2d(
        inRelativePos = Vector3(x = 500.0, y = 1.0, z = 5.0),
        inScreenSize =
          Vector2().apply {
            x = 1080.0
            y = 2400.0
          },
        inScreenRatio = Vector3(x = 1740.0, y = 1740.0, z = 1.0),
        inScreenRotTrig = Trig1().apply { setVector1(Vector1().apply { v = 0.2 }) },
        outScreenPos = screenPos,
      )

    assertFalse(drawn)
    assertEquals(171002.44761725934, screenPos.x, 1e-6)
    assertEquals(36109.5267274294, screenPos.y, 1e-6)
  }

  @Test
  fun `convert3dTo2d reports a point level with the camera as not drawn`() {
    val screenPos = Vector2()
    val drawn =
      Math3D.convert3dTo2d(
        inRelativePos = Vector3(x = 2.0, y = 1.0, z = 0.0),
        inScreenSize =
          Vector2().apply {
            x = 1080.0
            y = 2400.0
          },
        inScreenRatio = Vector3(x = 1740.0, y = 1740.0, z = 1.0),
        inScreenRotTrig = Trig1(),
        outScreenPos = screenPos,
      )

    assertFalse(drawn)
    assertEquals(0.0, screenPos.x, 0.0)
    assertEquals(0.0, screenPos.y, 0.0)
  }

  @Test
  fun `convert3dTo2d reports a point behind the camera as not drawn`() {
    val screenPos = Vector2()
    val drawn =
      Math3D.convert3dTo2d(
        inRelativePos = Vector3(x = 2.0, y = 1.0, z = -5.0),
        inScreenSize =
          Vector2().apply {
            x = 1080.0
            y = 2400.0
          },
        inScreenRatio = Vector3(x = 1740.0, y = 1740.0, z = 1.0),
        inScreenRotTrig = Trig1(),
        outScreenPos = screenPos,
      )

    assertFalse(drawn)
    assertEquals(0.0, screenPos.x, 0.0)
    assertEquals(0.0, screenPos.y, 0.0)
  }
}
