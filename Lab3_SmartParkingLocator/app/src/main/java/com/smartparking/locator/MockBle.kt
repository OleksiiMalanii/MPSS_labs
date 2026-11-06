package com.smartparking.locator

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Імітує сканування: через 2 секунди «знаходить» віртуальне реле. */
class MockBleScanner(private val log: (String) -> Unit) {
    suspend fun scan(): List<BleDevice> {
        log("SCAN start service=${BleProfile.short(BleProfile.SERVICE)}")
        delay(SCAN_MS)
        val device = MockBleConnector.DEVICE
        log("SCAN found ${device.name} ${device.address} rssi=${device.rssi}")
        return listOf(device)
    }

    companion object {
        const val SCAN_MS = 2000L
    }
}

/** Віртуальне BLE-реле. Станом Scanning керує ViewModel, тут його немає. */
class MockBleConnector(private val log: (String) -> Unit) : IBleConnector {
    private val _state = MutableStateFlow(BleState.Disconnected)
    override val state: StateFlow<BleState> = _state.asStateFlow()
    private var acknowledged = 0
    private var lastPacket = "NONE"

    override suspend fun connect() {
        check(_state.value == BleState.Disconnected) { "connect() у стані ${_state.value}" }
        _state.value = BleState.Connecting
        log("GATT connect ${DEVICE.address}")
        try {
            delay(CONNECT_MS)
        } catch (e: Throwable) {
            _state.value = BleState.Disconnected
            throw e
        }
        _state.value = BleState.Connected
        log("GATT connected, service ${BleProfile.short(BleProfile.SERVICE)} discovered")
    }

    override suspend fun disconnect() {
        check(_state.value == BleState.Connected) { "disconnect() у стані ${_state.value}" }
        _state.value = BleState.Disconnected
        log("GATT disconnected")
    }

    override suspend fun sendData(data: String) {
        val size = data.toByteArray().size
        require(data.isNotBlank() && size <= BleProfile.MAX_PACKET_BYTES) {
            "Пакет порожній або понад ${BleProfile.MAX_PACKET_BYTES} байт"
        }
        transfer {
            log("WRITE ${BleProfile.short(BleProfile.COMMAND)} \"$data\" ($size B)")
            delay(WRITE_MS)
            lastPacket = data
            acknowledged++
            log("NOTIFY ${BleProfile.short(BleProfile.STATUS)} \"ACK:$acknowledged\"")
        }
    }

    override suspend fun readStatus(): String = transfer {
        log("READ ${BleProfile.short(BleProfile.STATUS)}")
        delay(READ_MS)
        "READY;ACK=$acknowledged;LAST=$lastPacket".also { log("READ value \"$it\"") }
    }

    private suspend fun <T> transfer(block: suspend () -> T): T {
        check(_state.value == BleState.Connected) { "Обмін у стані ${_state.value}" }
        _state.value = BleState.DataTransfer
        try {
            return block()
        } finally {
            // Якщо з'єднання вже закрито, стан не змінюю.
            if (_state.value == BleState.DataTransfer) _state.value = BleState.Connected
        }
    }

    companion object {
        const val CONNECT_MS = 800L
        const val WRITE_MS = 400L
        const val READ_MS = 200L
        val DEVICE = BleDevice("ParkingHub-01", "AA:BB:CC:00:09:01", -58)
    }
}
