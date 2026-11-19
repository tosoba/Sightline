package com.trm.sightline.core.ar.camera.surface

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewStub
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.trm.sightline.core.ar.R
import com.trm.sightline.core.ar.camera.OpenGLRenderer
import java.util.concurrent.atomic.AtomicReference

internal class TextureViewRenderSurface : RenderSurface {
  private val nextFrameCompleter = AtomicReference<CallbackToFutureAdapter.Completer<Unit>?>()

  override fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): TextureView {
    viewStub.layoutResource = R.layout.texture_view_render_surface
    val textureView = viewStub.inflate() as TextureView
    textureView.surfaceTextureListener =
      object : TextureView.SurfaceTextureListener {
        private lateinit var surface: Surface

        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
          surface = Surface(st)
          renderer.attachOutputSurface(
            surface = surface,
            surfaceSize = Size(width, height),
            surfaceRotationDegrees = SurfaceRotations.toDegrees(textureView.display.rotation),
          )
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
          renderer.attachOutputSurface(
            surface = surface,
            surfaceSize = Size(width, height),
            surfaceRotationDegrees = SurfaceRotations.toDegrees(textureView.display.rotation),
          )
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
          renderer
            .detachOutputSurface()
            .addListener(
              {
                surface.release()
                st.release()
              },
              ContextCompat.getMainExecutor(textureView.context),
            )
          return false
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
          val completer = nextFrameCompleter.getAndSet(null)
          completer?.set(null)
        }
      }
    return textureView
  }

  override fun waitForNextFrame(): ListenableFuture<Unit> =
    CallbackToFutureAdapter.getFuture { completer ->
      nextFrameCompleter.set(completer)
      "textureViewImpl_waitForNextFrame"
    }
}
