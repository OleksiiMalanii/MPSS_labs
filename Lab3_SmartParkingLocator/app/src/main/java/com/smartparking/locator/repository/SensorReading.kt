package com.smartparking.locator.repository

/** Відлік датчика, з яким працює ViewModel. */
data class SensorReading(
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
)

fun SensorReading.toPayload() = SensorPayload(sensorType = sensorType, x = x, y = y, z = z, timestamp = timestamp)
