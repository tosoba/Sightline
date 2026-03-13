package com.trm.sightline.feature.camera

import android.location.Location
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.view.ARMarkerRenderer
import com.trm.sightline.core.model.Marker

@Composable
fun CameraContent(
  previewEnabled: Boolean,
  location: Location,
  markers: List<Marker>,
  blurredRectFs: List<RoundedRectF>,
  overlayContent: @Composable BoxScope.(ARMarkerRenderer) -> Unit = {},
) {
  val cameraPermissionState = rememberCameraPermissionState()
  LaunchedEffect(Unit) {
    if (!cameraPermissionState.isGranted) cameraPermissionState.launchRequest()
  }

  AnimatedContent(cameraPermissionState.isGranted) {
    if (it) {
      CameraPreview(
        location = location,
        markers = markers,
        enabled = previewEnabled,
        blurredRectFs = blurredRectFs,
        overlayContent = overlayContent,
      )
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        Button(onClick = cameraPermissionState::launchRequest) {
          Text(text = "Grant camera permission")
        }
      }
    }
  }
}
