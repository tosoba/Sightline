package com.trm.sightline.core.ar.util

import android.annotation.SuppressLint
import androidx.camera.core.CameraInfo
import androidx.camera.core.SurfaceRequest
import androidx.camera.view.internal.compat.quirk.DeviceQuirks
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk

fun SurfaceRequest.shouldUseTextureView(): Boolean {
  @SuppressLint("RestrictedApi")
  val isLegacyDevice =
    camera.cameraInfoInternal.implementationType == CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY
  val hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk::class.java) != null
  return isLegacyDevice || hasSurfaceViewQuirk
}
