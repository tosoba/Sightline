package com.trm.sightline.feature.camera

import android.location.Location
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.ar.model.RoundedRectF
import com.trm.sightline.core.ar.view.ARMarkerRenderer
import com.trm.sightline.core.model.Place

@Composable
fun CameraContent(
  previewEnabled: Boolean,
  previewBlurred: Boolean,
  location: Location?,
  places: List<Place>,
  blurredRectFs: List<RoundedRectF>,
  padding: PaddingValues,
  cameraPermissionGranted: Boolean,
  onGrantPermissionClick: () -> Unit,
  onCameraPreviewTouch: () -> Unit,
  overlayContent: @Composable BoxScope.(ARMarkerRenderer) -> Unit = {},
) {
  AnimatedContent(targetState = cameraPermissionGranted) { granted ->
    if (granted) {
      CameraPreview(
        enabled = previewEnabled,
        blurred = previewBlurred,
        location = location,
        places = places,
        blurredRectFs = blurredRectFs,
        padding = padding,
        onCameraPreviewTouch = onCameraPreviewTouch,
        overlayContent = overlayContent,
      )
    } else {
      CameraPermissionDeniedContent(
        modifier = Modifier.fillMaxSize().padding(16.dp).padding(padding),
        onGrantPermissionClick = onGrantPermissionClick,
      )
    }
  }
}
