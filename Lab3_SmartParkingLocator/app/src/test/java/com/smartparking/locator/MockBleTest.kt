package com.smartparking.locator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockBleTest {
    private val log = mutableListOf<String>()
    private val connector = MockBleConnector { log += it }

    @Test fun scannerFindsVirtualDeviceAfterTwoSeconds() = runTest {
        val result = async { MockBleScanner { log += it }.scan() }
        advanceTimeBy(1999); runCurrent()
        assertFalse(result.isCompleted)
        advanceTimeBy(2); runCurrent()
        assertEquals(listOf(MockBleConnector.DEVICE), result.await())
    }

    @Test fun connectPassesThroughConnecting() = runTest {
        val job = async { connector.connect() }
        runCurrent()
        assertEquals(BleState.Connecting, connector.state.value)
        advanceUntilIdle(); job.await()
        assertEquals(BleState.Connected, connector.state.value)
    }

    @Test fun cancelledConnectReturnsToDisconnected() = runTest {
        val job = async { connector.connect() }
        runCurrent()
        job.cancel(); runCurrent()
        assertEquals(BleState.Disconnected, connector.state.value)
    }

    @Test fun writeEntersDataTransferAndNotifiesAck() = runTest {
        launchConnected()
        val job = async { connector.sendData("HDG=90.0;TILT=OK") }
        runCurrent()
        assertEquals(BleState.DataTransfer, connector.state.value)
        advanceUntilIdle(); job.await()
        assertEquals(BleState.Connected, connector.state.value)
        assertTrue(log.any { it.startsWith("WRITE") && "HDG=90.0;TILT=OK" in it })
        assertTrue(log.any { it.startsWith("NOTIFY") && "ACK:1" in it })
    }

    @Test fun readReturnsLastPacketAndAckCount() = runTest {
        launchConnected()
        val send = async { connector.sendData("PING") }
        advanceUntilIdle(); send.await()
        val read = async { connector.readStatus() }
        advanceUntilIdle()
        assertEquals("READY;ACK=1;LAST=PING", read.await())
    }

    @Test fun sendWhileDisconnectedFails() = runTest {
        assertThrows(IllegalStateException::class.java) { runBlockingSend("PING") }
    }

    @Test fun invalidPacketIsRejectedAndStateKept() = runTest {
        launchConnected()
        assertThrows(IllegalArgumentException::class.java) { runBlockingSend(" ") }
        assertThrows(IllegalArgumentException::class.java) { runBlockingSend("x".repeat(BleProfile.MAX_PACKET_BYTES + 1)) }
        assertEquals(BleState.Connected, connector.state.value)
    }

    @Test fun disconnectReturnsToDisconnected() = runTest {
        launchConnected()
        connector.disconnect()
        assertEquals(BleState.Disconnected, connector.state.value)
    }

    @Test fun statusPacketFormat() {
        assertEquals("HDG=NA;TILT=NA", StatusPacket.from(null))
        assertEquals("HDG=90.0;PITCH=-30.0;ROLL=2.0;TILT=WARN", StatusPacket.from(Orientation(90f, -30f, 2f)))
        assertEquals("HDG=0.0;PITCH=15.0;ROLL=0.0;TILT=OK", StatusPacket.from(Orientation(0f, 15f, 0f)))
        assertEquals("HDG=0.0;PITCH=0.0;ROLL=0.0;TILT=OK", StatusPacket.from(Orientation(0f, -0.04f, -0f)))
    }

    private fun kotlinx.coroutines.test.TestScope.launchConnected() {
        val job = async { connector.connect() }
        advanceUntilIdle()
        assertTrue(job.isCompleted)
    }

    private fun runBlockingSend(data: String) = kotlinx.coroutines.runBlocking { connector.sendData(data) }
}
