package com.trm.sightline.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.trm.sightline.core.common.PermissionStatus

interface CameraPermissionState {
  val status: PermissionStatus

  val isGranted: Boolean
    get() = status == PermissionStatus.Granted

  fun launchRequest()
}

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
  val context = LocalContext.current
  val activity = requireNotNull(LocalActivity.current)

  fun isGranted(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      PackageManager.PERMISSION_GRANTED

  var status by remember {
    mutableStateOf(if (isGranted()) PermissionStatus.Granted else PermissionStatus.Unknown)
  }

  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { granted ->
        status =
          when {
            granted -> {
              PermissionStatus.Granted
            }
            activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
              PermissionStatus.Denied
            }
            else -> {
              PermissionStatus.PermanentlyDenied
            }
          }
      },
    )

  LifecycleResumeEffect(Unit) {
    if (isGranted()) status = PermissionStatus.Granted
    onPauseOrDispose {}
  }

  return remember(launcher) {
    object : CameraPermissionState {
      override val status: PermissionStatus
        get() = status

      override fun launchRequest() {
        launcher.launch(Manifest.permission.CAMERA)
      }
    }
  }
}
