package com.smartparking.locator.helpers

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

enum class ConnectionState(val label: String) {
    Disconnected("Disconnected"),
    Scanning("Scanning"),
    Connecting("Connecting"),
    Connected("Connected"),
    DataTransfer("Data Transfer"),
}

data class BleDevice(val name: String, val address: String, val rssi: Int)
