package com.trm.sightline.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

interface CameraPermissionState {
  val isGranted: Boolean

  fun launchRequest()
}

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
  val context = LocalContext.current
  var isGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { isGranted = it },
    )

  return remember(launcher) {
    object : CameraPermissionState {
      override val isGranted: Boolean
        get() = isGranted

      override fun launchRequest() {
        launcher.launch(Manifest.permission.CAMERA)
      }
    }
  }
}
