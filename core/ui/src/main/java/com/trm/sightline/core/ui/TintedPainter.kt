package com.trm.sightline.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

fun Painter.tinted(color: Color): Painter =
  object : Painter() {
    override val intrinsicSize
      get() = this@tinted.intrinsicSize

    override fun DrawScope.onDraw() {
      with(this@tinted) { draw(size, colorFilter = ColorFilter.tint(color)) }
    }
  }
