package com.trm.sightline.core.ar.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.MainThread

class OrientationManager : SensorEventListener {

  // --- Rotation vector path (primary) ---
  private val rotationVectorValues = FloatArray(5)

  // --- Accelerometer + magnetometer path (fallback) ---
  private val gravs = FloatArray(3)
  private val geoMags = FloatArray(3)

  // --- Shared ---
  private val rotationM = FloatArray(9)
  private val remappedRotationM = FloatArray(9)
  private val orientationArray = FloatArray(3)

  private var sensorManager: SensorManager? = null
  private var orientation = Orientation()
  private var sensorRunning = false

  private val pitchFilter = AngleLowPassFilter()
  private val rollFilter = AngleLowPassFilter()
  private val azimuthFilter = AngleLowPassFilter()

  var onOrientationChangedListener: OnOrientationChangedListener? = null

  var axisMode: Mode = Mode.COMPASS
    set(value) {
      field = value
      if (value == Mode.COMPASS) {
        firstAxis = SensorManager.AXIS_Y
        secondAxis = SensorManager.AXIS_MINUS_X
      } else {
        firstAxis = SensorManager.AXIS_X
        secondAxis = SensorManager.AXIS_Z
      }
    }

  private var firstAxis: Int = SensorManager.AXIS_Y
  private var secondAxis: Int = SensorManager.AXIS_MINUS_X

  // Tracks whether both fallback sensors have received at least one reading,
  // so we don't compute orientation from a partially-initialized state.
  private var hasGravity = false
  private var hasGeoMag = false

  @MainThread
  fun startSensor(context: Context): Boolean {
    if (sensorRunning) return true

    val manager = context.getSystemService(SensorManager::class.java)
    val started = tryStartRotationVector(manager) || tryStartFallback(manager)

    if (started) {
      sensorManager = manager
      sensorRunning = true
    }
    return started
  }

  private fun tryStartRotationVector(manager: SensorManager): Boolean {
    val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return false
    return manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
  }

  private fun tryStartFallback(manager: SensorManager): Boolean {
    val accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
    val mag = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return false

    val accelOk = manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    val magOk = manager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)

    if (!accelOk || !magOk) {
      manager.unregisterListener(this)
      return false
    }

    return true
  }

  @MainThread
  fun stopSensor() {
    if (sensorRunning) {
      sensorManager?.unregisterListener(this)
      sensorManager = null
      sensorRunning = false
      hasGravity = false
      hasGeoMag = false
      pitchFilter.reset()
      rollFilter.reset()
      azimuthFilter.reset()
    }
  }

  override fun onSensorChanged(event: SensorEvent) {
    when (event.sensor.type) {
      Sensor.TYPE_ROTATION_VECTOR -> {
        handleRotationVector(event)
      }
      Sensor.TYPE_ACCELEROMETER -> {
        System.arraycopy(event.values, 0, gravs, 0, 3)
        hasGravity = true
        if (hasGeoMag) handleFallback()
      }
      Sensor.TYPE_MAGNETIC_FIELD -> {
        System.arraycopy(event.values, 0, geoMags, 0, 3)
        hasGeoMag = true
        if (hasGravity) handleFallback()
      }
    }
  }

  private fun handleRotationVector(event: SensorEvent) {
    // event.values can be length 4 or 5 depending on the device; copy defensively
    System.arraycopy(event.values, 0, rotationVectorValues, 0, event.values.size)
    SensorManager.getRotationMatrixFromVector(rotationM, rotationVectorValues)
    computeOrientation()
  }

  private fun handleFallback() {
    if (!SensorManager.getRotationMatrix(rotationM, null, gravs, geoMags)) return
    computeOrientation()
  }

  private fun computeOrientation() {
    SensorManager.remapCoordinateSystem(rotationM, firstAxis, secondAxis, remappedRotationM)
    SensorManager.getOrientation(remappedRotationM, orientationArray)

    val pitch = orientationArray[1]
    val azimuth = orientationArray[0]
    val roll = orientationArray[2]

    orientation.pitch = pitchFilter.filter(pitch)
    orientation.azimuth = azimuthFilter.filter(azimuth)
    orientation.roll = rollFilter.filter(roll)

    onOrientationChangedListener?.onOrientationChanged(orientation)
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

  enum class Mode {
    COMPASS,
    AR,
  }

  interface OnOrientationChangedListener {
    fun onOrientationChanged(orientation: Orientation)
  }
}
