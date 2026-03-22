package com.trm.sightline.core.ar.util

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.core.graphics.withTranslation
import androidx.core.util.lruCache

@SuppressLint("WrongConstant")
@MainThread
internal fun Canvas.drawMultilineText(
  text: CharSequence,
  textPaint: TextPaint,
  width: Int,
  x: Float,
  y: Float,
  start: Int = 0,
  end: Int = text.length,
  alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
  textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
  spacingMult: Float = 1f,
  spacingAdd: Float = 0f,
  includePad: Boolean = true,
  ellipsizedWidth: Int = width,
  ellipsize: TextUtils.TruncateAt? = null,
  maxLines: Int = Int.MAX_VALUE,
  breakStrategy: Int = Layout.BREAK_STRATEGY_SIMPLE,
  hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE,
) {
  val cacheKey =
    "$text-$start-$end-$textPaint-$width-$alignment-$textDir-" +
      "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize-" +
      "$maxLines-$breakStrategy-$hyphenationFrequency"

  val staticLayout =
    StaticLayoutCache[cacheKey]
      ?: StaticLayout.Builder.obtain(text, start, end, textPaint, width)
        .setAlignment(alignment)
        .setTextDirection(textDir)
        .setLineSpacing(spacingAdd, spacingMult)
        .setIncludePad(includePad)
        .setEllipsizedWidth(ellipsizedWidth)
        .setEllipsize(ellipsize)
        .setMaxLines(maxLines)
        .setBreakStrategy(breakStrategy)
        .setHyphenationFrequency(hyphenationFrequency)
        .build()
        .apply { StaticLayoutCache[cacheKey] = this }

  staticLayout.draw(this, x, y)
}

private fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
  canvas.withTranslation(x, y) { draw(this) }
}

private object StaticLayoutCache {
  private const val MAX_SIZE = 50 // Arbitrary max number of cached items
  private val cache = lruCache<String, StaticLayout>(MAX_SIZE)

  operator fun set(key: String, staticLayout: StaticLayout) {
    cache.put(key, staticLayout)
  }

  operator fun get(key: String): StaticLayout? = cache[key]
}
