package com.smartparking.locator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class BleUi(
    val state: BleState = BleState.Disconnected,
    val devices: List<BleDevice> = emptyList(),
    val log: List<String> = emptyList(),
)

class BleViewModel : ViewModel() {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val lines = MutableStateFlow<List<String>>(emptyList())
    private val log: (String) -> Unit = { message ->
        Log.i(TAG, message)
        lines.update { (it + "${LocalTime.now().format(timeFormat)} $message").takeLast(MAX_LINES) }
    }
    private val scanner = MockBleScanner(log)
    private val connector: IBleConnector = MockBleConnector(log)
    private val scanning = MutableStateFlow(false)
    private val devices = MutableStateFlow<List<BleDevice>>(emptyList())

    val ui: StateFlow<BleUi> = combine(scanning, devices, connector.state, lines) { scan, found, state, text ->
        BleUi(if (scan) BleState.Scanning else state, found, text)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BleUi())

    // viewModelScope працює на Main.immediate: стан змінюється ще до повернення з методу, тому подвійний дотик безпечний.
    fun scan() {
        if (scanning.value || connector.state.value != BleState.Disconnected) return
        scanning.value = true
        viewModelScope.launch {
            try {
                devices.value = scanner.scan()
            } finally {
                scanning.value = false
            }
        }
    }

    fun connect() {
        if (scanning.value || connector.state.value != BleState.Disconnected) return
        viewModelScope.launch { connector.connect() }
    }

    fun disconnect() {
        if (connector.state.value != BleState.Connected) return
        viewModelScope.launch { connector.disconnect() }
    }

    fun send(packet: String) {
        if (connector.state.value != BleState.Connected) return
        viewModelScope.launch { connector.sendData(packet) }
    }

    fun read() {
        if (connector.state.value != BleState.Connected) return
        viewModelScope.launch { connector.readStatus() }
    }

    private companion object {
        const val TAG = "BleMock"
        const val MAX_LINES = 60
    }
}
