package com.trm.sightline.core.ar.camera.surface

import android.annotation.SuppressLint
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewStub
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.trm.sightline.core.ar.R
import com.trm.sightline.core.ar.camera.OpenGLRenderer

internal class SurfaceViewRenderSurface : RenderSurface {
  override fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): SurfaceView {
    viewStub.layoutResource = R.layout.surface_view_render_surface
    val surfaceView = viewStub.inflate() as SurfaceView
    surfaceView.holder.addCallback(
      object : SurfaceHolder.Callback2 {
        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
          val surfaceViewDisplay = surfaceView.display
          if (surfaceViewDisplay != null) {
            renderer.invalidateSurface(SurfaceRotations.toDegrees(surfaceViewDisplay.rotation))
          }
        }

        override fun surfaceCreated(holder: SurfaceHolder) = Unit

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
          renderer.attachOutputSurface(
            surface = holder.surface,
            surfaceSize = Size(width, height),
            surfaceRotationDegrees = SurfaceRotations.toDegrees(surfaceView.display.rotation),
          )
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
          renderer.detachOutputSurface()
        }
      }
    )
    return surfaceView
  }

  @SuppressLint("RestrictedApi")
  override fun waitForNextFrame(): ListenableFuture<Unit> = Futures.immediateFuture(Unit)
}
