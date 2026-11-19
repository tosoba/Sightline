package com.trm.sightline.core.ar.math

import kotlin.test.Test
import kotlin.test.assertEquals

class VectorTrigTest {
  @Test
  fun `Vector3 defaults to the origin`() {
    val vector = Vector3()

    assertEquals(0.0, vector.x, 0.0)
    assertEquals(0.0, vector.y, 0.0)
    assertEquals(0.0, vector.z, 0.0)
  }

  @Test
  fun `Vector3 constructor fills every axis`() {
    val vector = Vector3(x = 1.0, y = -2.0, z = 3.0)

    assertEquals(1.0, vector.x, 0.0)
    assertEquals(-2.0, vector.y, 0.0)
    assertEquals(3.0, vector.z, 0.0)
  }

  @Test
  fun `Vector2 and Vector1 default to zero`() {
    val vector2 = Vector2()
    val vector1 = Vector1()

    assertEquals(0.0, vector2.x, 0.0)
    assertEquals(0.0, vector2.y, 0.0)
    assertEquals(0.0, vector1.v, 0.0)
  }

  @Test
  fun `Trig1 computes the sine and cosine of the stored angle`() {
    val trig = Trig1()

    trig.setVector1(Vector1().apply { v = 0.7 })

    assertEquals(0.644217687237691, trig.sin, 1e-12)
    assertEquals(0.7648421872844885, trig.cos, 1e-12)
  }

  @Test
  fun `Trig3 computes the sine and cosine of every axis`() {
    val trig = Trig3()

    trig.setVector3(Vector3(x = 0.4, y = -0.9, z = 1.3))

    assertEquals(0.3894183423086505, trig.xSin, 1e-12)
    assertEquals(0.9210609940028851, trig.xCos, 1e-12)
    assertEquals(-0.7833269096274834, trig.ySin, 1e-12)
    assertEquals(0.6216099682706644, trig.yCos, 1e-12)
    assertEquals(0.963558185417193, trig.zSin, 1e-12)
    assertEquals(0.26749882862458735, trig.zCos, 1e-12)
  }
}
