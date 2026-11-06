package com.smartparking.locator

import kotlinx.coroutines.flow.StateFlow

interface IBleConnector {
    val state: StateFlow<BleState>
    suspend fun connect()
    suspend fun disconnect()

    /** Write у характеристику COMMAND. */
    suspend fun sendData(data: String)

    /** Read характеристики STATUS. */
    suspend fun readStatus(): String
}
