package com.trm.sightline.core.ar.util

import android.content.Context
import android.graphics.RectF
import android.os.Build
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import java.io.File
import kotlin.math.ceil

val Context.phoneRotation: Int
  get() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      display.rotation
    } else {
      @Suppress("DEPRECATION")
      (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay?.rotation
    } ?: Surface.ROTATION_0

val Context.statusBarHeightPx: Int
  get() {
    val heightDp = 24
    return ceil(heightDp * resources.displayMetrics.density).toInt()
  }

val Context.actionBarHeightPx: Float
  get() {
    val actionBarStyledAttributes =
      theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarHeight = actionBarStyledAttributes.getDimension(0, 0f)
    actionBarStyledAttributes.recycle()
    return actionBarHeight
  }

val Context.bottomNavigationViewHeightPx: Int
  get() {
    val heightDp = 56
    return ceil(heightDp * resources.displayMetrics.density).toInt()
  }

fun Context.dpToPx(value: Float): Float =
  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

fun Context.pxToDp(value: Float): Float = value / resources.displayMetrics.density

fun Context.spToPx(value: Float): Float =
  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

fun Context.getOrCreateCacheFile(name: String): File? {
  val cacheDir = externalCacheDir
  val tileCacheDir: File?
  if (cacheDir != null) {
    tileCacheDir = File(cacheDir, name)
    if (!tileCacheDir.exists()) tileCacheDir.mkdir()
  } else {
    tileCacheDir = null
  }
  return tileCacheDir
}

val Context.bottomNavigationViewRectF: RectF
  get() {
    val screenSize = getScreenSize(includeTopInset = true)
    val bottomNavigationViewHeight = dpToPx(56f)
    return RectF(
      0f,
      screenSize.height - bottomNavigationViewHeight,
      screenSize.width.toFloat(),
      screenSize.height.toFloat(),
    )
  }

fun Context.getScreenSize(
  includeLeftInset: Boolean = false,
  includeTopInset: Boolean = false,
  includeRightInset: Boolean = false,
  includeBottomInset: Boolean = false,
): Size =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val metrics = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics
    val insets =
      metrics.windowInsets.getInsetsIgnoringVisibility(
        WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
      )
    val bounds = metrics.bounds
    var width = bounds.width()
    if (!includeRightInset) width -= insets.right
    if (!includeLeftInset) width -= insets.left
    var height = bounds.height()
    if (!includeTopInset) height -= insets.top
    if (!includeBottomInset) height -= insets.bottom
    Size(width, height)
  } else {
    val displayMetrics = resources.displayMetrics
    Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
  }
