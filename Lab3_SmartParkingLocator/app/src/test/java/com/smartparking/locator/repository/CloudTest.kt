package com.smartparking.locator.repository

import com.smartparking.locator.cloud.SendPolicy

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

private class FakeApi(var behavior: () -> Response<CloudAck> = { Response.success(200, CloudAck("-Key1")) }) : CloudApi {
    val received = mutableListOf<SensorPayload>()
    override suspend fun send(payload: SensorPayload): Response<CloudAck> {
        received += payload
        return behavior()
    }
}

class CloudTest {
    private fun reading(t: Long = 1L, x: Float = 0f, y: Float = 0f, z: Float = 9.81f) = SensorReading("accelerometer", x, y, z, t)

    @Test fun policyFirstSampleThenIntervalAndDelta() {
        val policy = SendPolicy()
        assertTrue(policy.shouldSend(reading(0)))
        assertFalse(policy.shouldSend(reading(2000, y = 0.5f)))     // мала зміна, ще рано
        assertFalse(policy.shouldSend(reading(500, y = 5f)))        // велика зміна, але не частіше 1 с
        assertTrue(policy.shouldSend(reading(2500, y = 5f)))        // зміна ≥ 1 м/с²
        assertFalse(policy.shouldSend(reading(4000, y = 5.2f)))     // відносно останнього відправленого
        assertTrue(policy.shouldSend(reading(7500, y = 5.2f)))      // минуло 5 с
    }

    @Test fun onlineSendDeliversAndEmptiesQueue() = runBlocking {
        val api = FakeApi()
        val result = SensorRepository(api, { true }).save(reading())
        assertEquals(SyncResult.Ok("-Key1", 200, 1), result)
        assertEquals(1, api.received.size)
    }

    @Test fun offlineKeepsPayloadPendingWithoutCallingApi() = runBlocking {
        val api = FakeApi()
        val result = SensorRepository(api, { false }).save(reading())
        assertEquals(SyncResult.Pending(1), result)
        assertTrue(api.received.isEmpty())
    }

    @Test fun pendingPayloadsAreFlushedInOrderWhenNetworkReturns() = runBlocking {
        var online = false
        val api = FakeApi()
        val repository = SensorRepository(api, { online })
        repository.save(reading(1)); repository.save(reading(2))
        online = true
        val result = repository.save(reading(3))
        assertEquals(SyncResult.Ok("-Key1", 200, 3), result)
        assertEquals(listOf(1L, 2L, 3L), api.received.map { it.timestamp })
        assertEquals(SyncResult.Ok("-Key1", 200, 1), repository.save(reading(4)))
    }

    @Test fun serverErrorIsFailedAndPayloadIsRetriedLater() = runBlocking {
        val api = FakeApi({ Response.error(500, "".toResponseBody("text/plain".toMediaType())) })
        val repository = SensorRepository(api, { true })
        assertEquals(SyncResult.Failed("HTTP 500", 1), repository.save(reading(1)))
        api.behavior = { Response.success(200, CloudAck("-Key2")) }
        assertEquals(SyncResult.Ok("-Key2", 200, 2), repository.save(reading(2)))
        assertEquals(listOf(1L, 1L, 2L), api.received.map { it.timestamp })
    }

    @Test fun ioErrorIsFailedNotCrash() = runBlocking {
        val api = FakeApi({ throw IOException("timeout") })
        assertEquals(SyncResult.Failed("Мережа: timeout", 1), SensorRepository(api, { true }).save(reading()))
    }

    @Test fun queueIsLimited() = runBlocking {
        val api = FakeApi()
        val repository = SensorRepository(api, { false })
        repeat(CloudConfig.MAX_PENDING + 5) { repository.save(reading(it.toLong())) }
        val result = repository.save(reading(999)) as SyncResult.Pending
        assertEquals(CloudConfig.MAX_PENDING, result.queued)
    }

    @Test fun payloadJsonFields() {
        val json = reading(5).toPayload().toJson()
        assertEquals("""{"deviceId":"parking-locator-09","sensorType":"accelerometer","x":0.0,"y":0.0,"z":9.81,"timestamp":5}""", json)
    }

    @Test fun retrofitPostsJsonToFirebasePath() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody("""{"name":"-P5abc"}"""))
            val api = CloudClient.create(server.url("/").toString()) {}
            val response = api.send(reading(5).toPayload())
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/sensor_reports.json", request.path)
            assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
            assertTrue(request.body.readUtf8().contains(""""sensorType":"accelerometer""""))
            assertEquals(200, response.code())
            assertEquals("-P5abc", response.body()?.name)
        }
    }
}
