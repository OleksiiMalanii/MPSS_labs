package com.smartparking.locator.helpers

import kotlinx.coroutines.flow.StateFlow

interface IBleConnector {
    val state: StateFlow<ConnectionState>
    suspend fun connect()
    suspend fun disconnect()

    /** Write у характеристику COMMAND. */
    suspend fun sendData(data: String)

    /** Read характеристики STATUS. */
    suspend fun readStatus(): String
}
