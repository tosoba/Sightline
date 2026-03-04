package com.trm.sightline.core.ar.math

import kotlin.math.cos
import kotlin.math.sin

internal class Trig3 {
  var xSin = 0.0
    private set

  var xCos = 0.0
    private set

  var ySin = 0.0
    private set

  var yCos = 0.0
    private set

  var zSin = 0.0
    private set

  var zCos = 0.0
    private set

  fun setVector3(vector: Vector3) {
    xSin = sin(vector.x)
    ySin = sin(vector.y)
    zSin = sin(vector.z)
    xCos = cos(vector.x)
    yCos = cos(vector.y)
    zCos = cos(vector.z)
  }
}
