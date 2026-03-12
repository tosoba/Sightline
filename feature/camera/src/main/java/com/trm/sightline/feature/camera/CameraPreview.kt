package com.trm.sightline.feature.camera

import android.content.Context
import android.location.Location
import android.view.ViewStub
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.trm.sightline.core.ar.camera.OpenGLRenderer
import com.trm.sightline.core.ar.model.ARMarker
import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.ar.orientation.OrientationManager
import com.trm.sightline.core.ar.util.phoneRotation
import com.trm.sightline.core.ar.view.ARView
import com.trm.sightline.core.model.Marker
import timber.log.Timber

@Composable
fun CameraPreview(location: Location, markers: List<Marker>, enabled: Boolean) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val viewStub = remember { mutableStateOf<ViewStub?>(null) }
  val preview =
    produceState<Preview?>(null) {
      value = context.initCameraPreview(lifecycleOwner, context.phoneRotation)
    }

  AnimatedVisibility(visible = preview.value != null, enter = fadeIn(), exit = fadeOut()) {
    Box {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
          FrameLayout(context).also { container ->
            container.addView(
              ViewStub(context).apply {
                layoutParams =
                  FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                  )
              }
            )
          }
        },
        update = { container ->
          if (viewStub.value == null) {
            viewStub.value = container.getChildAt(0) as ViewStub
          }
        },
      )

      val arView = remember { ARView(context) }
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { arView },
        update = { view ->
          view.povLocation = location
          view.markers = markers.map(::ARMarker)
          view.markerRenderer.disabled = !enabled
        },
      )

      val orientationManager = remember {
        OrientationManager().apply {
          axisMode = OrientationManager.Mode.AR
          onOrientationChangedListener =
            object : OrientationManager.OnOrientationChangedListener {
              override fun onOrientationChanged(orientation: Orientation) {
                if (!orientation.pitchWithinLimit) return
                arView.orientation = orientation
                arView.phoneRotation = context.phoneRotation
              }
            }
        }
      }
      LifecycleStartEffect(Unit) {
        orientationManager.startSensor(context)
        onStopOrDispose { orientationManager.stopSensor() }
      }

      val openGLRenderer = rememberOpenGLRenderer(preview.value, viewStub.value)
      LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
          arView.markerRenderer.drawnMarkerRectFs.collect(openGLRenderer::setMarkerRectFs)
        }
      }

      val markersPagingState by
        arView.markerRenderer.markersPagingState.collectAsStateWithLifecycle()
      AnimatedVisibility(
        visible = markersPagingState.maxPage > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
      ) {
        CameraPreviewPagePicker(
          currentPage = markersPagingState.currentPage,
          totalPages = markersPagingState.maxPage + 1,
          onPageSelected = { arView.markerRenderer.currentPage = it },
        )
      }
    }
  }
}

@Composable
private fun rememberOpenGLRenderer(preview: Preview?, viewStub: ViewStub?): OpenGLRenderer {
  val openGLRenderer = remember(::OpenGLRenderer)
  if (viewStub != null && preview != null) {
    DisposableEffect(Unit) {
      try {
        openGLRenderer.attachInputPreview(preview, viewStub)
      } catch (ex: Exception) {
        Timber.e(ex)
      }
      onDispose(openGLRenderer::shutdown)
    }
  }
  return openGLRenderer
}

suspend fun Context.initCameraPreview(
  lifecycleOwner: LifecycleOwner,
  @ImageOutputConfig.RotationValue rotation: Int,
): Preview {
  val preview = Preview.Builder().setTargetRotation(rotation).build()
  ProcessCameraProvider.getInstance(this)
    .await()
    .bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
  return preview
}

private val Orientation.pitchWithinLimit: Boolean
  get() = pitch in -PITCH_LIMIT_RADIANS..PITCH_LIMIT_RADIANS

private const val PITCH_LIMIT_RADIANS = Math.PI / 3
