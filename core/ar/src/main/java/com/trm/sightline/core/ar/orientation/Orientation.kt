package com.trm.sightline.core.ar.orientation

data class Orientation(var pitch: Float = 0f, var roll: Float = 0f, var azimuth: Float = 0f)

val Orientation.pitchWithinLimit: Boolean
  get() = pitch in -PITCH_LIMIT_RADIANS..PITCH_LIMIT_RADIANS

private const val PITCH_LIMIT_RADIANS = Math.PI / 4
