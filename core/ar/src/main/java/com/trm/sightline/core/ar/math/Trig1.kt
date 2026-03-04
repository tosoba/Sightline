package com.trm.sightline.core.ar.math

import kotlin.math.cos
import kotlin.math.sin

internal class Trig1 {
  var sin = 0.0
    private set

  var cos = 0.0
    private set

  fun setVector1(vector: Vector1) {
    sin = sin(vector.v)
    cos = cos(vector.v)
  }
}
