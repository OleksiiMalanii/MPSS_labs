package com.smartparking.locator.repository

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object CloudConfig {
    /**
     * Firebase Realtime Database через REST API (тестовий режим правил, ключ не потрібен).
     * URL бази не є секретом, але в тестовому режимі база відкрита для запису — для навчальної роботи це припустимо.
     */
    const val BASE_URL = "https://smart-parking-locator-6fd62-default-rtdb.europe-west1.firebasedatabase.app/"
    const val PATH = "sensor_reports.json"
    const val DEVICE_ID = "parking-locator-09"
    const val MAX_PENDING = 50
}

/** Один звіт датчика у форматі JSON для хмари. */
data class SensorPayload(
    val deviceId: String = CloudConfig.DEVICE_ID,
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
)

fun SensorPayload.toJson(): String = Gson().toJson(this)

/** Відповідь Firebase на POST: name — згенерований ключ нового запису. */
data class CloudAck(val name: String? = null)

interface CloudApi {
    @POST(CloudConfig.PATH)
    suspend fun send(@Body payload: SensorPayload): Response<CloudAck>
}

object CloudClient {
    fun create(baseUrl: String = CloudConfig.BASE_URL, log: (String) -> Unit): CloudApi {
        val http = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor { log(it) }.apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudApi::class.java)
    }
}
