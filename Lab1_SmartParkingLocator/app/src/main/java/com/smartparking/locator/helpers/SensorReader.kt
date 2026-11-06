package com.smartparking.locator.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class SensorKind { Accelerometer, Magnetometer }

sealed interface SensorSignal {
    data class Registered(val accelerometer: Boolean, val magnetometer: Boolean) : SensorSignal
    data class Reading(
        val kind: SensorKind, val x: Float, val y: Float, val z: Float, val displayRotation: Int,
    ) : SensorSignal
    data class MagnetometerAccuracy(val unreliable: Boolean) : SensorSignal
}

class SensorReader(context: Context) {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val displays = context.getSystemService(DisplayManager::class.java)
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun signals(): Flow<SensorSignal> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val kind = when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> SensorKind.Accelerometer
                    Sensor.TYPE_MAGNETIC_FIELD -> SensorKind.Magnetometer
                    else -> return
                }
                if (event.values.size < 3) return
                // Android повторно використовує event.values, тому беремо значення одразу.
                trySend(SensorSignal.Reading(kind, event.values[0], event.values[1], event.values[2],
                    displays.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD)
                    trySend(SensorSignal.MagnetometerAccuracy(accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE))
            }
        }
        val accelerationReady = accelerometer?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        } ?: false
        val magneticReady = magnetometer?.let {
            manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        } ?: false
        Log.i(TAG, "start: accelerometer=$accelerationReady magnetometer=$magneticReady")
        trySend(SensorSignal.Registered(accelerationReady, magneticReady))
        // Викликається при скасуванні Flow, тобто в onPause.
        awaitClose {
            manager.unregisterListener(listener)
            Log.i(TAG, "stop: listeners unregistered")
        }
    }

    private companion object { const val TAG = "ParkingSensors" }
}
