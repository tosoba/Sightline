package com.trm.sightline.core.common

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

interface PermissionState {
  val status: PermissionStatus

  val isGranted: Boolean
    get() = status == PermissionStatus.Granted

  fun launchRequest()
}

@Composable
fun rememberPermissionState(permission: String): PermissionState {
  val context = LocalContext.current
  val activity = requireNotNull(LocalActivity.current)

  fun isGranted(): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

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
            activity.shouldShowRequestPermissionRationale(permission) -> {
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
    object : PermissionState {
      override val status: PermissionStatus
        get() = status

      override fun launchRequest() {
        launcher.launch(permission)
      }
    }
  }
}
