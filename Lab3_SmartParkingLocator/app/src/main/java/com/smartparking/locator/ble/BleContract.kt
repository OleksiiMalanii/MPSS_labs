package com.smartparking.locator.ble

import com.smartparking.locator.helpers.BleDevice
import com.smartparking.locator.helpers.ConnectionState
import com.smartparking.locator.helpers.HeadingMath
import com.smartparking.locator.helpers.Orientation
import java.util.Locale

// State — стан з'єднання, знайдені пристрої та журнал обміну.
data class BleState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val devices: List<BleDevice> = emptyList(),
    val log: List<String> = emptyList(),
)

// Action — кнопки екрана BLE.
sealed interface BleAction {
    data object Scan : BleAction
    data object Connect : BleAction
    data object Disconnect : BleAction
    data class Send(val packet: String) : BleAction
    data object Read : BleAction
}

// Пакет, який екран надсилає в характеристику COMMAND.
object StatusPacket {
    // "%.1f" інакше дає "-0.0" для малих від'ємних значень.
    private fun clean(value: Float) = if (kotlin.math.abs(value) < 0.05f) 0f else value

    /** Формат: HDG=<курс>;PITCH=<кут>;ROLL=<кут>;TILT=OK|WARN. Без курсу: HDG=NA;TILT=NA. */
    fun from(orientation: Orientation?): String = orientation?.let {
        String.format(
            Locale.US, "HDG=%.1f;PITCH=%.1f;ROLL=%.1f;TILT=%s", clean(it.azimuth), clean(it.pitch), clean(it.roll),
            if (HeadingMath.excessiveTilt(it.pitch, it.roll)) "WARN" else "OK",
        )
    } ?: "HDG=NA;TILT=NA"
}
