package com.smartparking.locator

import com.google.gson.Gson
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
    private fun payload(t: Long = 1L, z: Float = 9.81f) = SensorPayload(sensorType = "accelerometer", x = 0f, y = 0f, z = z, timestamp = t)

    @Test fun policyFirstSampleThenIntervalAndDelta() {
        val policy = SendPolicy()
        val flat = Vector3(0f, 0f, 9.81f)
        assertTrue(policy.shouldSend(0, flat))
        assertFalse(policy.shouldSend(2000, Vector3(0f, 0.5f, 9.81f)))      // мала зміна, ще рано
        assertFalse(policy.shouldSend(500, Vector3(0f, 5f, 9.81f)))         // велика зміна, але не частіше 1 с
        assertTrue(policy.shouldSend(2500, Vector3(0f, 5f, 9.81f)))         // зміна ≥ 1 м/с²
        assertFalse(policy.shouldSend(4000, Vector3(0f, 5.2f, 9.81f)))      // відносно останнього відправленого
        assertTrue(policy.shouldSend(7500, Vector3(0f, 5.2f, 9.81f)))       // минуло 5 с
    }

    @Test fun onlineSendDeliversAndEmptiesQueue() = runBlocking {
        val api = FakeApi()
        val result = CloudRepository(api, { true }).send(payload())
        assertEquals(SyncResult.Ok("-Key1", 200, 1), result)
        assertEquals(1, api.received.size)
    }

    @Test fun offlineKeepsPayloadPendingWithoutCallingApi() = runBlocking {
        val api = FakeApi()
        val result = CloudRepository(api, { false }).send(payload())
        assertEquals(SyncResult.Pending(1), result)
        assertTrue(api.received.isEmpty())
    }

    @Test fun pendingPayloadsAreFlushedInOrderWhenNetworkReturns() = runBlocking {
        var online = false
        val api = FakeApi()
        val repository = CloudRepository(api, { online })
        repository.send(payload(1)); repository.send(payload(2))
        online = true
        val result = repository.send(payload(3))
        assertEquals(SyncResult.Ok("-Key1", 200, 3), result)
        assertEquals(listOf(1L, 2L, 3L), api.received.map { it.timestamp })
        assertEquals(SyncResult.Ok("-Key1", 200, 1), repository.send(payload(4)))
    }

    @Test fun serverErrorIsFailedAndPayloadIsRetriedLater() = runBlocking {
        val api = FakeApi({ Response.error(500, "".toResponseBody("text/plain".toMediaType())) })
        val repository = CloudRepository(api, { true })
        assertEquals(SyncResult.Failed("HTTP 500", 1), repository.send(payload(1)))
        api.behavior = { Response.success(200, CloudAck("-Key2")) }
        assertEquals(SyncResult.Ok("-Key2", 200, 2), repository.send(payload(2)))
        assertEquals(listOf(1L, 1L, 2L), api.received.map { it.timestamp })
    }

    @Test fun ioErrorIsFailedNotCrash() = runBlocking {
        val api = FakeApi({ throw IOException("timeout") })
        assertEquals(SyncResult.Failed("Мережа: timeout", 1), CloudRepository(api, { true }).send(payload()))
    }

    @Test fun queueIsLimited() = runBlocking {
        val api = FakeApi()
        val repository = CloudRepository(api, { false })
        repeat(CloudConfig.MAX_PENDING + 5) { repository.send(payload(it.toLong())) }
        val result = repository.send(payload(999)) as SyncResult.Pending
        assertEquals(CloudConfig.MAX_PENDING, result.queued)
    }

    @Test fun payloadJsonFields() {
        val json = Gson().toJson(payload(5))
        assertEquals("""{"deviceId":"parking-locator-09","sensorType":"accelerometer","x":0.0,"y":0.0,"z":9.81,"timestamp":5}""", json)
    }

    @Test fun retrofitPostsJsonToFirebasePath() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody("""{"name":"-P5abc"}"""))
            val api = CloudClient.create(server.url("/").toString()) {}
            val response = api.send(payload(5))
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
