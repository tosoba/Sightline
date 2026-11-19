package com.trm.sightline.core.ar.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.MainThread
import com.trm.sightline.core.ar.math.MarkerProjection
import com.trm.sightline.core.ar.math.ScreenRotation
import com.trm.sightline.core.ar.model.ARMarker
import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.ar.orientation.pitchWithinLimit
import com.trm.sightline.core.model.GeoLocation
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ARView : View {
  var povLocation: GeoLocation? = null
    @MainThread
    set(value) {
      field = value
      value?.let { calculateDistancesBetween(it, markers) }
      maxRange = calculateMaxRageFor(markers)
      markerRenderer.povLocation = value
    }

  private var maxRange: Double = DEFAULT_MAX_RANGE_METERS * RANGE_MARGIN_MULTIPLIER
    @MainThread
    set(value) {
      field = value
      invalidate()
    }

  var markers: List<ARMarker> = emptyList()
    @MainThread
    set(value) {
      field = value
      povLocation?.let { calculateDistancesBetween(it, value) }
      maxRange = calculateMaxRageFor(value)
      markerRenderer.setMarkers(value)
    }

  private fun calculateMaxRageFor(value: List<ARMarker>): Double =
    (value.maxByOrNull(ARMarker::distance)?.distance?.toDouble() ?: DEFAULT_MAX_RANGE_METERS) *
      RANGE_MARGIN_MULTIPLIER

  val markerRenderer: ARMarkerRenderer = ARMarkerRenderer(context)

  var orientation: Orientation = Orientation()
    @MainThread
    set(value) {
      field = value
      invalidate()
    }

  var screenRotation: ScreenRotation = ScreenRotation.ROTATION_0
    @MainThread set

  var onMarkerPressed: ((ARMarker) -> Unit)? = null
    @MainThread set

  var onTouch: (() -> Unit)? = null
    @MainThread set

  private val markerProjection = MarkerProjection()

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
  ) : super(context, attrs, defStyle)

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val povLocation = this.povLocation ?: return
    markerProjection.updateViewport(width, height)
    markerProjection.updateCamera(povLocation, orientation, screenRotation)
    markers.forEach(::calculateMarkerScreenPosition)
    markerRenderer.draw(markers.filter { it.distance < maxRange }, canvas)
  }

  private fun calculateMarkerScreenPosition(marker: ARMarker) {
    val projected = markerProjection.project(marker.location)
    if (projected.isInFront) {
      marker.x = projected.screenX
      marker.y = projected.screenY
    }
    marker.isDrawn = projected.isDrawn
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!orientation.pitchWithinLimit) return super.onTouchEvent(event)

    val markerPressed =
      onMarkerPressed?.let { listener ->
        if (event.action != MotionEvent.ACTION_DOWN) return@let false
        val markerWidth = markerRenderer.markerWidthPx
        val markerHeight = markerRenderer.markerHeightPx
        val pressedMarker =
          findNearestMarker(event.x, event.y)?.takeIf { marker ->
            abs(marker.x - event.x) < markerWidth / 2 && abs(marker.y - event.y) < markerHeight / 2
          }
        if (pressedMarker != null) {
          listener(pressedMarker)
          true
        } else {
          false
        }
      } ?: false
    if (!markerPressed && event.action == MotionEvent.ACTION_DOWN) onTouch?.invoke()
    return super.onTouchEvent(event)
  }

  private fun findNearestMarker(x: Float, y: Float): ARMarker? =
    markers
      .filter { marker -> marker.isDrawn && markerRenderer.isOnCurrentPage(marker) }
      .minByOrNull { marker ->
        sqrt((marker.x - x).toDouble().pow(2.0) + (marker.y - y).toDouble().pow(2.0))
      }

  private fun calculateDistancesBetween(location: GeoLocation, markers: List<ARMarker>) {
    markers.forEach { marker -> marker.distance = marker.location.distanceTo(location) }
  }

  override fun onSaveInstanceState(): Parcelable =
    SavedState(super.onSaveInstanceState(), markerRenderer.onSaveInstanceState())

  override fun onRestoreInstanceState(state: Parcelable?) {
    val savedState = state as? SavedState
    super.onRestoreInstanceState(savedState?.superSavedState ?: state)
    markerRenderer.onRestoreInstanceState(savedState?.rendererBundle)
  }

  @Parcelize
  internal class SavedState(val superSavedState: Parcelable?, val rendererBundle: Bundle?) :
    BaseSavedState(superSavedState), Parcelable

  companion object {
    private const val DEFAULT_MAX_RANGE_METERS = 1_000.0
    private const val RANGE_MARGIN_MULTIPLIER = 1.1
  }
}
