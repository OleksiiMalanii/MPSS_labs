package com.smartparking.locator

import android.app.Application
import android.util.Log
import com.smartparking.locator.helpers.NetworkMonitor
import com.smartparking.locator.repository.CloudClient
import com.smartparking.locator.repository.SensorRepository

class SmartParkingApp : Application() {
    // Один репозиторій на весь застосунок, щоб черга Pending не роз'їхалась між екранами.
    val repository: SensorRepository by lazy {
        val log: (String) -> Unit = { Log.i("CloudSync", it) }
        SensorRepository(CloudClient.create(log = log), NetworkMonitor(this)::isOnline, log)
    }
}
