package com.trm.sightline.core.ar.util

import android.app.Activity
import android.content.Context
import android.graphics.RectF
import android.os.Build
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat

val Context.phoneRotation: Int
  get() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      display.rotation
    } else {
      @Suppress("DEPRECATION") getSystemService(WindowManager::class.java).defaultDisplay?.rotation
    } ?: Surface.ROTATION_0

const val sideSheetWidthDp = 400
const val collapsedBottomSheetContentHeightDp = 186
const val collapsedBottomSheetDragHandleHeightDp = 48

internal val Context.cameraPreviewVerticalPaddingPx: Float
  get() = dpToPx(16f)

internal val Context.bottomSheetHeightPx: Float
  get() {
    val heightDp = collapsedBottomSheetContentHeightDp + collapsedBottomSheetDragHandleHeightDp
    return dpToPx(heightDp.toFloat()) + navigationBarsBottomInsetPx
  }

internal val Context.navigationBarsBottomInsetPx: Float
  get() = rootWindowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom.toFloat()

internal val Context.statusBarTopInsetPx: Float
  get() = rootWindowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top.toFloat()

internal val Context.rootWindowInsets: WindowInsetsCompat
  get() =
    WindowInsetsCompat.toWindowInsetsCompat((this as Activity).window.decorView.rootWindowInsets)

internal val Context.isCompactHeight: Boolean
  get() = resources.displayMetrics.run { heightPixels / density < 480f }

fun Context.dpToPx(value: Float): Float =
  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

fun Context.spToPx(value: Float): Float =
  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

val Context.bottomSheetRectF: RectF
  get() {
    val screenSize = getScreenSize()
    return RectF(
      0f,
      screenSize.height - bottomSheetHeightPx,
      screenSize.width.toFloat(),
      screenSize.height.toFloat(),
    )
  }

val Context.sideSheetRectF: RectF
  get() {
    val screenSize = getScreenSize()
    return RectF(
      screenSize.width - dpToPx(sideSheetWidthDp.toFloat()),
      0f,
      screenSize.width.toFloat(),
      screenSize.height.toFloat(),
    )
  }

private fun Context.getScreenSize(
  includeLeftInset: Boolean = true,
  includeTopInset: Boolean = true,
  includeRightInset: Boolean = true,
  includeBottomInset: Boolean = true,
): Size =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
    val insets =
      metrics.windowInsets.getInsetsIgnoringVisibility(
        WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars()
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
