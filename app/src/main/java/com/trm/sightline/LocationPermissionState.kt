package com.trm.sightline

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

interface LocationPermissionState {
  val isGranted: Boolean

  fun launchRequest()
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
  val context = LocalContext.current
  var isGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
      onResult = { permissions ->
        isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
      },
    )

  return remember(launcher) {
    object : LocationPermissionState {
      override val isGranted: Boolean
        get() = isGranted

      override fun launchRequest() {
        launcher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          )
        )
      }
    }
  }
}
