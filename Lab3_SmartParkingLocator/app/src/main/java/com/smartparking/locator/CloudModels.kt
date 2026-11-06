package com.smartparking.locator

import kotlin.math.abs
import kotlin.math.max

object CloudConfig {
    /**
     * Firebase Realtime Database через REST API (тестовий режим правил, ключ не потрібен).
     * URL бази не є секретом, але в тестовому режимі база відкрита для запису — для навчальної роботи це припустимо.
     */
    const val BASE_URL = "https://smart-parking-locator-6fd62-default-rtdb.europe-west1.firebasedatabase.app/"
    const val PATH = "sensor_reports.json"
    const val DEVICE_ID = "parking-locator-09"
    const val INTERVAL_MS = 5000L
    const val MIN_GAP_MS = 1000L
    const val DELTA = 1.0f
    const val MAX_PENDING = 50
}

/** Один звіт датчика для хмари. */
data class SensorPayload(
    val deviceId: String = CloudConfig.DEVICE_ID,
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Відповідь Firebase на POST: name — згенерований ключ нового запису. */
data class CloudAck(val name: String? = null)

enum class SyncStatus(val label: String) {
    Idle("Очікування"), Sending("Відправлення"), Ok("OK"), Failed("Помилка"), Pending("Pending"),
}

/**
 * Рішення «відправляти зараз чи ні»: раз на INTERVAL_MS або коли будь-яка вісь змінилась на DELTA,
 * але не частіше ніж раз на MIN_GAP_MS.
 */
class SendPolicy(
    private val intervalMs: Long = CloudConfig.INTERVAL_MS,
    private val minGapMs: Long = CloudConfig.MIN_GAP_MS,
    private val delta: Float = CloudConfig.DELTA,
) {
    private var lastAt = 0L
    private var lastValue: Vector3? = null

    fun shouldSend(now: Long, value: Vector3): Boolean {
        val prev = lastValue
        val send = prev == null || now - lastAt >= intervalMs ||
            (now - lastAt >= minGapMs && maxChange(prev, value) >= delta)
        if (send) {
            lastAt = now
            lastValue = value
        }
        return send
    }

    private fun maxChange(a: Vector3, b: Vector3) = max(abs(a.x - b.x), max(abs(a.y - b.y), abs(a.z - b.z)))
}
