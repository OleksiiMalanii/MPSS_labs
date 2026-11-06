package com.smartparking.locator.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface SyncResult {
    /** [sent] — скільки звітів доставлено за цей виклик (разом із раніше відкладеними). */
    data class Ok(val key: String?, val code: Int, val sent: Int) : SyncResult
    data class Failed(val reason: String, val queued: Int) : SyncResult
    data class Pending(val queued: Int) : SyncResult
}

/**
 * Одне місце для даних: звіт спершу потрапляє в чергу, потім черга відправляється по порядку.
 * Без інтернету звіти залишаються в черзі (Pending) і йдуть після відновлення мережі.
 */
class SensorRepository(
    private val api: CloudApi,
    private val isOnline: () -> Boolean,
    private val log: (String) -> Unit = {},
) {
    private val lock = Mutex()
    private val queue = ArrayDeque<SensorPayload>()

    // Мережа — не в головному потоці.
    suspend fun save(reading: SensorReading): SyncResult = withContext(Dispatchers.IO) {
        lock.withLock { flush(reading.toPayload()) }
    }

    private suspend fun flush(payload: SensorPayload): SyncResult {
        queue.addLast(payload)
        while (queue.size > CloudConfig.MAX_PENDING) queue.removeFirst()
        if (!isOnline()) {
            log("OFFLINE, у черзі ${queue.size}")
            return SyncResult.Pending(queue.size)
        }
        var sent = 0
        var last: SyncResult.Ok? = null
        while (queue.isNotEmpty()) {
            val response = try {
                api.send(queue.first())
            } catch (e: IOException) {
                log("Помилка мережі: ${e.message}")
                return SyncResult.Failed("Мережа: ${e.message ?: e.javaClass.simpleName}", queue.size)
            }
            if (!response.isSuccessful) {
                log("HTTP ${response.code()}")
                return SyncResult.Failed("HTTP ${response.code()}", queue.size)
            }
            queue.removeFirst()
            sent++
            last = SyncResult.Ok(response.body()?.name, response.code(), sent)
            log("HTTP ${response.code()}, key=${response.body()?.name}")
        }
        return last!!
    }
}
