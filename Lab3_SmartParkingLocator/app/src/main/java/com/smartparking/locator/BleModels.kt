package com.smartparking.locator

import java.util.Locale
import java.util.UUID

/** Профіль віртуального реле паркінгу: один сервіс і дві характеристики. */
object BleProfile {
    val SERVICE: UUID = UUID.fromString("7a1c0001-5b6e-4c2a-9d3f-0a11b0c4e901")

    /** Read + Notify: статус пристрою та підтвердження (ACK). */
    val STATUS: UUID = UUID.fromString("7a1c0002-5b6e-4c2a-9d3f-0a11b0c4e901")

    /** Write: команди й дані від застосунку. */
    val COMMAND: UUID = UUID.fromString("7a1c0003-5b6e-4c2a-9d3f-0a11b0c4e901")

    /** Короткий запис UUID для терміналу: перші 8 символів. */
    fun short(uuid: UUID) = uuid.toString().take(8)

    /** Максимальна довжина пакета в байтах (UTF-8) після узгодження MTU. */
    const val MAX_PACKET_BYTES = 100
}

enum class BleState(val label: String) {
    Disconnected("Disconnected"),
    Scanning("Scanning"),
    Connecting("Connecting"),
    Connected("Connected"),
    DataTransfer("Data Transfer"),
}

data class BleDevice(val name: String, val address: String, val rssi: Int)

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
