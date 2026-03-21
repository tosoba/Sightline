package com.trm.sightline.feature.camera

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.view.ARMarkerRenderer
import com.trm.sightline.core.common.PermissionStatus
import com.trm.sightline.core.model.Place

@Composable
fun CameraContent(
  previewEnabled: Boolean,
  previewBlurred: Boolean,
  location: Location,
  places: List<Place>,
  blurredRectFs: List<RoundedRectF>,
  onCameraPreviewTouch: () -> Unit,
  overlayContent: @Composable BoxScope.(ARMarkerRenderer) -> Unit = {},
) {
  val context = LocalContext.current
  val cameraPermissionState = rememberCameraPermissionState()
  var showSettingsDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (cameraPermissionState.status == PermissionStatus.Unknown) {
      cameraPermissionState.launchRequest()
    }
  }

  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = { Text("Camera permission required") },
      text = { Text("Camera access was permanently denied. Open Settings to grant it.") },
      confirmButton = {
        TextButton(
          onClick = {
            showSettingsDialog = false
            context.startActivity(
              Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
            )
          }
        ) {
          Text("Open Settings")
        }
      },
      dismissButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") } },
    )
  }

  AnimatedContent(targetState = cameraPermissionState.isGranted) { granted ->
    if (granted) {
      CameraPreview(
        enabled = previewEnabled,
        blurred = previewBlurred,
        location = location,
        places = places,
        blurredRectFs = blurredRectFs,
        onCameraPreviewTouch = onCameraPreviewTouch,
        overlayContent = overlayContent,
      )
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
          onClick = {
            if (cameraPermissionState.status == PermissionStatus.PermanentlyDenied) {
              showSettingsDialog = true
            } else {
              cameraPermissionState.launchRequest()
            }
          }
        ) {
          Text("Grant camera permission")
        }
      }
    }
  }
}
