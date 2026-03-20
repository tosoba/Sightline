package com.trm.sightline

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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

interface LocationPermissionState {
  val isGranted: Boolean
  val shouldShowRationale: Boolean

  fun launchRequest()
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
  val context = LocalContext.current
  val activity = requireNotNull(LocalActivity.current)

  var isGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var shouldShowRationale by remember {
    mutableStateOf(
      ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION,
      )
    )
  }

  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
      onResult = { permissions ->
        isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        shouldShowRationale =
          ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
          )
      },
    )

  return remember(launcher) {
    object : LocationPermissionState {
      override val isGranted: Boolean
        get() = isGranted

      override val shouldShowRationale: Boolean
        get() = shouldShowRationale

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
