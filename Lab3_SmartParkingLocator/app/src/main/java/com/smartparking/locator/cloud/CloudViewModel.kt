package com.smartparking.locator.cloud

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.locator.SmartParkingApp
import com.smartparking.locator.helpers.SensorKind
import com.smartparking.locator.helpers.SensorReader
import com.smartparking.locator.helpers.SensorSignal
import com.smartparking.locator.repository.SensorReading
import com.smartparking.locator.repository.SyncResult
import com.smartparking.locator.repository.toJson
import com.smartparking.locator.repository.toPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

class CloudViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as SmartParkingApp).repository
    private val sensorReader = SensorReader(app)
    private val policy = SendPolicy()
    private val _state = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState> = _state.asStateFlow()

    // Event: Channel доставляє кожне повідомлення рівно один раз.
    private val _events = Channel<CloudEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var lastSample: SensorReading? = null
    private var lastSettled = SyncStatus.Idle

    fun onAction(action: CloudAction) {
        when (action) {
            CloudAction.Start -> start()
            CloudAction.Stop -> stop()
            is CloudAction.SetAuto -> _state.update { it.copy(auto = action.on) }
            CloudAction.SendNow -> sendNow()
        }
    }

    private fun start() {
        if (sensorJob != null) return
        lastSample = null
        sensorJob = viewModelScope.launch {
            sensorReader.signals().collect { signal ->
                if (signal is SensorSignal.Reading && signal.kind == SensorKind.Accelerometer)
                    onSample(SensorReading("accelerometer", signal.x, signal.y, signal.z, System.currentTimeMillis()))
            }
        }
    }

    private fun stop() {
        sensorJob?.cancel()   // скасування Flow → unregisterListener()
        sensorJob = null
    }

    /** На кожен відлік акселерометра політика вирішує, чи відправляти. */
    private fun onSample(reading: SensorReading) {
        lastSample = reading
        if (_state.value.auto && policy.shouldSend(reading)) upload(reading)
    }

    private fun sendNow() {
        val reading = lastSample
        if (reading == null) viewModelScope.launch { _events.send(CloudEvent.ShowMessage("Немає даних сенсора")) }
        else upload(reading.copy(timestamp = System.currentTimeMillis()))
    }

    private fun upload(reading: SensorReading) {
        val before = _state.value.status.takeIf { it != SyncStatus.Sending } ?: lastSettled
        _state.update { it.copy(status = SyncStatus.Sending, lastPayload = reading.toPayload().toJson()) }
        viewModelScope.launch {
            val result = repository.save(reading)
            _state.update {
                when (result) {
                    is SyncResult.Ok -> it.copy(status = SyncStatus.Ok, sent = it.sent + result.sent, pending = 0,
                        detail = "HTTP ${result.code}, ключ ${result.key}")
                    is SyncResult.Failed -> it.copy(status = SyncStatus.Failed, pending = result.queued, detail = result.reason)
                    is SyncResult.Pending -> it.copy(status = SyncStatus.Pending, pending = result.queued,
                        detail = "Немає інтернету, у черзі: ${result.queued}")
                }
            }
            val status = _state.value.status
            lastSettled = status
            // Toast лише при переході в Pending чи Failed, а не на кожен звіт.
            if (status != SyncStatus.Ok && status != before) _events.send(CloudEvent.ShowMessage(
                if (status == SyncStatus.Pending) "Немає інтернету: дані чекають у черзі (Pending)"
                else "Помилка синхронізації: ${_state.value.detail}"))
        }
    }
}

/**
 * Рішення «відправляти зараз чи ні»: раз на INTERVAL_MS або коли будь-яка вісь змінилась на DELTA,
 * але не частіше ніж раз на MIN_GAP_MS.
 */
class SendPolicy(
    private val intervalMs: Long = INTERVAL_MS,
    private val minGapMs: Long = MIN_GAP_MS,
    private val delta: Float = DELTA,
) {
    private var last: SensorReading? = null

    fun shouldSend(reading: SensorReading): Boolean {
        val prev = last
        val gap = reading.timestamp - (prev?.timestamp ?: 0L)
        val send = prev == null || gap >= intervalMs || (gap >= minGapMs && maxChange(prev, reading) >= delta)
        if (send) last = reading
        return send
    }

    private fun maxChange(a: SensorReading, b: SensorReading) = max(abs(a.x - b.x), max(abs(a.y - b.y), abs(a.z - b.z)))

    companion object {
        const val INTERVAL_MS = 5000L
        const val MIN_GAP_MS = 1000L
        const val DELTA = 1.0f
    }
}
