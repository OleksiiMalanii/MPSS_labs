package com.smartparking.locator

import android.hardware.SensorManager
import android.view.Surface

data class Orientation(val azimuth: Float, val pitch: Float, val roll: Float)

object OrientationEngine {
    fun calculate(gravity: FloatArray, magnetic: FloatArray, displayRotation: Int): Orientation? {
        if (gravity.size != 3 || magnetic.size != 3 ||
            gravity.any { !it.isFinite() } || magnetic.any { !it.isFinite() }) return null
        val rotation = FloatArray(9)
        // Акселерометр компенсує нахил; магнітометр задає магнітну північ.
        if (!SensorManager.getRotationMatrix(rotation, null, gravity, magnetic)) return null
        val axes = when (displayRotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> return null
        }
        val screen = FloatArray(9)
        if (!SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, screen)) return null
        // Майже вертикальна грань не має стійкого курсу.
        if (kotlin.math.hypot(screen[1], screen[4]) < 0.1f) return null
        val angles = SensorManager.getOrientation(screen, FloatArray(3))
        if (angles.any { !it.isFinite() }) return null
        fun degrees(radians: Float) = Math.toDegrees(radians.toDouble()).toFloat()
        return Orientation(HeadingMath.normalizeDegrees(degrees(angles[0])),
            degrees(angles[1]), degrees(angles[2]))
    }
}
