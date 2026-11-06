package com.smartparking.locator.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.locator.helpers.HeadingMath
import com.smartparking.locator.helpers.OrientationEngine
import com.smartparking.locator.helpers.SensorKind
import com.smartparking.locator.helpers.SensorReader
import com.smartparking.locator.helpers.SensorSignal
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonitorViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorReader = SensorReader(app)
    private val _state = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = _state.asStateFlow()
    private var sensorJob: Job? = null

    fun onAction(action: MonitorAction) {
        when (action) {
            MonitorAction.Start -> start()
            MonitorAction.Stop -> stop()
        }
    }

    private fun start() {
        if (sensorJob != null) return
        // Нові відліки після повернення.
        _state.value = MonitorState()
        sensorJob = viewModelScope.launch { sensorReader.signals().collect(::onSignal) }
    }

    private fun stop() {
        sensorJob?.cancel()
        sensorJob = null
        _state.update { it.copy(listening = false, orientation = null, tiltWarning = false) }
    }

    private fun onSignal(signal: SensorSignal) = _state.update { state ->
        when (signal) {
            is SensorSignal.Registered -> state.copy(
                listening = signal.accelerometer || signal.magnetometer,
                error = when {
                    !signal.accelerometer -> "Акселерометр недоступний"
                    !signal.magnetometer -> "Магнітометр недоступний"
                    else -> null
                })
            is SensorSignal.MagnetometerAccuracy -> state.copy(magnetometerUnreliable = signal.unreliable)
            is SensorSignal.Reading -> if (!state.listening) state else {
                val value = Vector3(signal.x, signal.y, signal.z)
                val next = when (signal.kind) {
                    SensorKind.Accelerometer -> state.copy(acceleration = value)
                    SensorKind.Magnetometer -> state.copy(magnetic = value)
                }
                val orientation = if (next.acceleration != null && next.magnetic != null)
                    OrientationEngine.calculate(next.acceleration.array(), next.magnetic.array(), signal.displayRotation)
                else null
                next.copy(orientation = orientation,
                    tiltWarning = orientation?.let { HeadingMath.excessiveTilt(it.pitch, it.roll) } ?: false)
            }
        }
    }
}
