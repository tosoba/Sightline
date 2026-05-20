package com.trm.sightline.core.ar.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.MainThread
import com.trm.sightline.core.ar.math.Math3D
import com.trm.sightline.core.ar.math.Trig1
import com.trm.sightline.core.ar.math.Trig3
import com.trm.sightline.core.ar.math.Vector1
import com.trm.sightline.core.ar.math.Vector2
import com.trm.sightline.core.ar.math.Vector3
import com.trm.sightline.core.ar.model.ARMarker
import com.trm.sightline.core.ar.orientation.Orientation
import com.trm.sightline.core.ar.orientation.pitchWithinLimit
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ARView : View {
  var povLocation: Location? = null
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

  var phoneRotation: Int = 0
    @MainThread set

  var onMarkerPressed: ((ARMarker) -> Unit)? = null
    @MainThread set

  var onTouch: (() -> Unit)? = null
    @MainThread set

  private val camTrig = Trig3()
  private val camPos = Vector3()
  private val screenRatio = Vector3()
  private val screenSize = Vector2()
  private val screenRot = Vector1()
  private val screenRotTrig = Trig1()

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
  ) : super(context, attrs, defStyle)

  init {
    screenRatio.z = SCREEN_DEPTH.toDouble()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val povLocation = this.povLocation ?: return
    preDraw(povLocation)
    markers.forEach(::calculateMarkerScreenPosition)
    markerRenderer.draw(markers.filter { it.distance < maxRange }, canvas)
  }

  private fun preDraw(location: Location) {
    // For the moment we set a square as ratio. Size is arithmetic mean of width and height
    screenRatio.y = ((width + height).toFloat() / 2).toDouble()
    screenRatio.x = ((width + height).toFloat() / 2).toDouble()
    // Get the current size of the window
    screenSize.y = height.toDouble()
    screenSize.x = width.toDouble()
    // Obtain the current camera rotation and related calculations based on phone orientation
    // and rotation
    val camRot = Vector3()
    Math3D.getCamRotation(orientation, phoneRotation, camRot, camTrig, screenRot, screenRotTrig)
    // Transform current camera location into a position object;
    Math3D.convertLocationToPosition(location, camPos)
  }

  private fun calculateMarkerScreenPosition(marker: ARMarker) {
    val markerPos = Vector3()
    // Transform marker Location into a Position object
    Math3D.convertLocationToPosition(marker.location, markerPos)
    // Calculate relative position to the camera. Transforms angles of latitude and longitude
    // into meters of distance.
    val relativePos = Vector3()
    Math3D.getRelativeTranslationInMeters(markerPos, camPos, relativePos)
    // Rotates the marker around the camera in order to set the camera rotation to <0,0,0>
    val relativeRotPos = Vector3()
    Math3D.getRelativeRotation(relativePos, camTrig, relativeRotPos)
    // Converts a 3d position into a 2d position on screen
    val screenPos = Vector2()
    val drawn =
      Math3D.convert3dTo2d(relativeRotPos, screenSize, screenRatio, screenRotTrig, screenPos)
    // If drawn is false, the marker is behind us, so no need to paint
    if (relativeRotPos.z > 0) {
      marker.x = screenPos.x.toFloat()
      marker.y = screenPos.y.toFloat()
    }
    marker.isDrawn = drawn
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

  private fun calculateDistancesBetween(location: Location, markers: List<ARMarker>) {
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
    private const val SCREEN_DEPTH = 1
    private const val DEFAULT_MAX_RANGE_METERS = 1_000.0
    private const val RANGE_MARGIN_MULTIPLIER = 1.1
  }
}
