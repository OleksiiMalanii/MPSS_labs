package com.smartparking.locator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CloudUi(
    val status: SyncStatus = SyncStatus.Idle,
    val sent: Int = 0,
    val pending: Int = 0,
    val detail: String = "Ще нічого не відправлено",
    val lastPayload: String = "—",
    val auto: Boolean = true,
)

class CloudViewModel(private val repository: CloudRepository) : ViewModel() {
    private val policy = SendPolicy()
    private var lastSettled = SyncStatus.Idle
    private val _ui = MutableStateFlow(CloudUi())
    val ui: StateFlow<CloudUi> = _ui.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Повідомлення для Toast: лише при переході в Pending чи Failed, а не на кожен звіт. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setAuto(on: Boolean) = _ui.update { it.copy(auto = on) }

    /** Викликається на кожен відлік акселерометра; політика вирішує, чи відправляти. */
    fun onSample(value: Vector3, now: Long = System.currentTimeMillis()) {
        if (_ui.value.auto && policy.shouldSend(now, value)) send(value)
    }

    fun sendNow(value: Vector3?) {
        if (value == null) _messages.tryEmit("Немає даних сенсора") else send(value)
    }

    private fun send(value: Vector3) {
        val payload = SensorPayload(sensorType = "accelerometer", x = value.x, y = value.y, z = value.z)
        val before = _ui.value.status.takeIf { it != SyncStatus.Sending } ?: lastSettled
        _ui.update { it.copy(status = SyncStatus.Sending, lastPayload = Gson().toJson(payload)) }
        viewModelScope.launch {
            val result = repository.send(payload)
            _ui.update {
                when (result) {
                    is SyncResult.Ok -> it.copy(status = SyncStatus.Ok, sent = it.sent + result.sent, pending = 0,
                        detail = "HTTP ${result.code}, ключ ${result.key}")
                    is SyncResult.Failed -> it.copy(status = SyncStatus.Failed, pending = result.queued, detail = result.reason)
                    is SyncResult.Pending -> it.copy(status = SyncStatus.Pending, pending = result.queued,
                        detail = "Немає інтернету, у черзі: ${result.queued}")
                }
            }
            val status = _ui.value.status
            lastSettled = status
            if (status != SyncStatus.Ok && !(before == status)) _messages.tryEmit(
                if (status == SyncStatus.Pending) "Немає інтернету: дані чекають у черзі (Pending)"
                else "Помилка синхронізації: ${_ui.value.detail}")
        }
    }

    companion object {
        const val TAG = "CloudSync"
        fun log(message: String) { Log.i(TAG, message) }
    }
}
