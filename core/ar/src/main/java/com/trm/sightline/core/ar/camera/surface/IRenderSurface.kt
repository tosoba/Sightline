package com.trm.sightline.core.ar.camera.surface

import android.view.View
import android.view.ViewStub
import com.google.common.util.concurrent.ListenableFuture
import com.trm.sightline.core.ar.camera.OpenGLRenderer

internal interface IRenderSurface {
  fun waitForNextFrame(): ListenableFuture<Unit>

  fun inflateWith(viewStub: ViewStub, renderer: OpenGLRenderer): View
}
