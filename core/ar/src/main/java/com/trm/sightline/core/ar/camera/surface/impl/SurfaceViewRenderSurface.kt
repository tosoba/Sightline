package com.trm.sightline.core.ar.camera.surface.impl

import android.annotation.SuppressLint
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewStub
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.trm.sightline.core.ar.R
import com.trm.sightline.core.ar.camera.OpenGLRenderer
import com.trm.sightline.core.ar.camera.surface.IRenderSurface
import com.trm.sightline.core.ar.camera.surface.Surfaces

internal class SurfaceViewRenderSurface : IRenderSurface {
  /**
   * Inflates a [SurfaceView] into the provided [ViewStub] and attaches it to the provided
   * [OpenGLRenderer].
   *
   * @param viewStub Stub which will be replaced by SurfaceView.
   * @param renderer Renderer which will be used to update the SurfaceView.
   * @return The inflated SurfaceView.
   */
  override fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): SurfaceView {
    viewStub.layoutResource = R.layout.surface_view_render_surface
    val surfaceView = viewStub.inflate() as SurfaceView
    surfaceView.holder.addCallback(
      object : SurfaceHolder.Callback2 {
        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
          val surfaceViewDisplay = surfaceView.display
          if (surfaceViewDisplay != null) {
            renderer.invalidateSurface(
              Surfaces.toSurfaceRotationDegrees(surfaceViewDisplay.rotation)
            )
          }
        }

        override fun surfaceCreated(holder: SurfaceHolder) = Unit

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
          renderer.attachOutputSurface(
            holder.surface,
            Size(width, height),
            Surfaces.toSurfaceRotationDegrees(surfaceView.display.rotation),
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
