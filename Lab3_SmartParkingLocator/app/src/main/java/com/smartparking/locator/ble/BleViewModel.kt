package com.smartparking.locator.ble

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartparking.locator.helpers.BleDevice
import com.smartparking.locator.helpers.ConnectionState
import com.smartparking.locator.helpers.IBleConnector
import com.smartparking.locator.helpers.MockBleConnector
import com.smartparking.locator.helpers.MockBleScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    // State збирається з чотирьох потоків; назовні лише StateFlow для читання.
    val state: StateFlow<BleState> = combine(scanning, devices, connector.state, lines) { scan, found, connection, text ->
        BleState(if (scan) ConnectionState.Scanning else connection, found, text)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BleState())

    fun onAction(action: BleAction) {
        when (action) {
            BleAction.Scan -> scan()
            BleAction.Connect -> connect()
            BleAction.Disconnect -> disconnect()
            is BleAction.Send -> send(action.packet)
            BleAction.Read -> read()
        }
    }

    // viewModelScope працює на Main.immediate: стан змінюється ще до повернення з методу, тому подвійний дотик безпечний.
    private fun scan() {
        if (scanning.value || connector.state.value != ConnectionState.Disconnected) return
        scanning.value = true
        viewModelScope.launch {
            try {
                devices.value = scanner.scan()
            } finally {
                scanning.value = false
            }
        }
    }

    private fun connect() {
        if (scanning.value || connector.state.value != ConnectionState.Disconnected) return
        viewModelScope.launch { connector.connect() }
    }

    private fun disconnect() {
        if (connector.state.value != ConnectionState.Connected) return
        viewModelScope.launch { connector.disconnect() }
    }

    private fun send(packet: String) {
        if (connector.state.value != ConnectionState.Connected) return
        viewModelScope.launch { connector.sendData(packet) }
    }

    private fun read() {
        if (connector.state.value != ConnectionState.Connected) return
        viewModelScope.launch { connector.readStatus() }
    }

    private companion object {
        const val TAG = "BleMock"
        const val MAX_LINES = 60
    }
}
